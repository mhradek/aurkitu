package io.github.mhradek.aurkitu.plugin.core;

import io.github.classgraph.ClassInfo;
import io.github.classgraph.FieldInfo;
import io.github.mhradek.aurkitu.annotations.FlatBufferComment;
import io.github.mhradek.aurkitu.annotations.FlatBufferEnum;
import io.github.mhradek.aurkitu.annotations.FlatBufferEnumTypeField;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Emits enums and unions.
 *
 * <p>Enum values are implicit (declaration order) unless the Java enum has a field
 * annotated with {@link FlatBufferEnumTypeField}, in which case that field's value
 * for each constant is emitted explicitly, in ascending order.</p>
 */
@Slf4j
final class EnumEmitter {

    /** Inclusive value ranges of the FlatBuffers enum underlying types. */
    private static final Map<String, BigInteger[]> RANGES = Map.of(
            "byte", range(Byte.MIN_VALUE, Byte.MAX_VALUE),
            "ubyte", range(0, 0xFF),
            "short", range(Short.MIN_VALUE, Short.MAX_VALUE),
            "ushort", range(0, 0xFFFF),
            "int", range(Integer.MIN_VALUE, Integer.MAX_VALUE),
            "uint", range(0, 0xFFFF_FFFFL),
            "long", range(Long.MIN_VALUE, Long.MAX_VALUE),
            "ulong", new BigInteger[]{BigInteger.ZERO, BigInteger.TWO.pow(64).subtract(BigInteger.ONE)});

    private final boolean validate;
    private final List<String> validationIssues;

    EnumEmitter(boolean validate, List<String> validationIssues) {
        this.validate = validate;
        this.validationIssues = validationIssues;
    }

    private record Constant(String name, Long value) {}

    String buildEnum(ClassInfo classInfo) {
        var enumAnno = classInfo.getAnnotationInfo(FlatBufferEnum.class.getName());
        boolean isUnion = "UNION".equals(Naming.enumConstantName(Naming.param(enumAnno, "value")));
        String enumType = Naming.enumConstantName(Naming.param(enumAnno, "enumType")).toLowerCase();
        String underlying = RANGES.containsKey(enumType) ? enumType : "byte";
        String name = TypeRegistry.typeName(classInfo.getName());

        StringBuilder sb = new StringBuilder();
        String comment = Naming.stringParam(classInfo.getAnnotationInfo(FlatBufferComment.class.getName()), "comment");
        if (!comment.isBlank()) {
            sb.append("// ").append(comment).append("\n");
        }

        List<Constant> constants = isUnion ? names(classInfo) : constants(classInfo, name, underlying);

        if (isUnion) {
            sb.append("union ").append(name).append(" {\n");
        } else {
            sb.append("enum ").append(name).append(" : ").append(underlying).append(" {\n");
        }
        boolean ulong = "ulong".equals(underlying);
        for (Constant constant : constants) {
            sb.append("  ").append(constant.name());
            if (constant.value() != null) {
                sb.append(" = ").append(ulong ? Long.toUnsignedString(constant.value()) : Long.toString(constant.value()));
            }
            sb.append(",\n");
        }
        sb.append("}\n\n");
        return sb.toString();
    }

    /** Constant names in declaration order. */
    private static List<Constant> names(ClassInfo classInfo) {
        List<Constant> names = new ArrayList<>();
        for (FieldInfo field : classInfo.getDeclaredFieldInfo()) {
            if (field.isEnum()) {
                names.add(new Constant(field.getName(), null));
            }
        }
        return names;
    }

    private List<Constant> constants(ClassInfo classInfo, String name, String underlying) {
        List<Constant> names = names(classInfo);
        FieldInfo valueField = null;
        for (FieldInfo field : classInfo.getDeclaredFieldInfo()) {
            if (field.hasAnnotation(FlatBufferEnumTypeField.class.getName())) {
                valueField = field;
                break;
            }
        }
        if (valueField == null) {
            return names;
        }

        List<Constant> valued = readValues(classInfo, valueField.getName(), underlying);
        if (valued == null) {
            issue("Enum " + name + ": could not read @FlatBufferEnumTypeField " + valueField.getName() + "; values are implicit");
            return names;
        }

        Comparator<Long> order = "ulong".equals(underlying) ? Long::compareUnsigned : Long::compare;
        valued.sort(Comparator.comparing(Constant::value, order));
        Set<Long> seen = new HashSet<>();
        BigInteger[] range = RANGES.get(underlying);
        for (Constant constant : valued) {
            if (!seen.add(constant.value())) {
                issue("Enum " + name + " has duplicate value " + constant.value() + " (" + constant.name() + ")");
            }
            BigInteger v = "ulong".equals(underlying)
                    ? new BigInteger(Long.toUnsignedString(constant.value()))
                    : BigInteger.valueOf(constant.value());
            if (v.compareTo(range[0]) < 0 || v.compareTo(range[1]) > 0) {
                issue("Enum " + name + " value " + v + " (" + constant.name() + ") is out of range for " + underlying);
            }
        }
        return valued;
    }

    /** Reads the value field of every constant; null if the class cannot be loaded or the field is not integral. */
    @SuppressWarnings("PMD.AvoidAccessibilityAlteration")
    private static List<Constant> readValues(ClassInfo classInfo, String fieldName, String underlying) {
        Class<?> enumClass = classInfo.loadClass(true);
        if (enumClass == null || !enumClass.isEnum()) {
            return null;
        }
        try {
            Field field = enumClass.getDeclaredField(fieldName);
            field.setAccessible(true);
            List<Constant> values = new ArrayList<>();
            for (Object constant : enumClass.getEnumConstants()) {
                Object raw = field.get(constant);
                if (!(raw instanceof Byte || raw instanceof Short || raw instanceof Integer || raw instanceof Long)) {
                    return null;
                }
                values.add(new Constant(((Enum<?>) constant).name(), unsignedView((Number) raw, underlying)));
            }
            return values;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            log.debug("Could not read enum values of {}", classInfo.getName(), e);
            return null;
        }
    }

    /**
     * Java has no unsigned types, so e.g. {@code (byte) 0xFF} is -1. For unsigned
     * underlying types, reinterpret the bits at the Java field's own width.
     */
    private static long unsignedView(Number raw, String underlying) {
        long value = raw.longValue();
        if (!underlying.startsWith("u") || value >= 0) {
            return value;
        }
        if (raw instanceof Byte) return value & 0xFFL;
        if (raw instanceof Short) return value & 0xFFFFL;
        if (raw instanceof Integer) return value & 0xFFFF_FFFFL;
        return value; // long: kept as-is and printed unsigned for ulong
    }

    private void issue(String message) {
        log.warn(message);
        if (validate) {
            validationIssues.add(message);
        }
    }

    private static BigInteger[] range(long min, long max) {
        return new BigInteger[]{BigInteger.valueOf(min), BigInteger.valueOf(max)};
    }
}
