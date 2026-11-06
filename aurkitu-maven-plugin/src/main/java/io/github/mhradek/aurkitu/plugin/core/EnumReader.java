package io.github.mhradek.aurkitu.plugin.core;

import io.github.classgraph.AnnotationInfo;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.FieldInfo;
import io.github.mhradek.aurkitu.annotations.FlatBufferComment;
import io.github.mhradek.aurkitu.annotations.FlatBufferEnum;
import io.github.mhradek.aurkitu.annotations.FlatBufferEnumTypeField;
import io.github.mhradek.aurkitu.plugin.core.model.Declaration;
import io.github.mhradek.aurkitu.plugin.core.model.EnumDeclaration;
import io.github.mhradek.aurkitu.plugin.core.model.EnumDeclaration.Value;
import io.github.mhradek.aurkitu.plugin.core.model.ScalarKind;
import io.github.mhradek.aurkitu.plugin.core.model.TypeName;
import io.github.mhradek.aurkitu.plugin.core.model.UnionDeclaration;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Reads {@code @FlatBufferEnum} classes into enum and union declarations.
 *
 * <p>Enum values are implicit (declaration order) unless the Java enum has a field
 * annotated with {@link FlatBufferEnumTypeField}, in which case that field's value
 * for each constant is used, in ascending order.</p>
 */
@Slf4j
final class EnumReader {

    private final TypeRegistry registry;

    EnumReader(TypeRegistry registry) {
        this.registry = registry;
    }

    Declaration read(ClassInfo classInfo) {
        AnnotationInfo annotation = classInfo.getAnnotationInfo(FlatBufferEnum.class.getName());
        TypeName name = registry.nameOf(classInfo.getName());
        String comment = SchemaReader.comment(classInfo.getAnnotationInfo(FlatBufferComment.class.getName()));
        List<String> constants = constantNames(classInfo);

        if ("UNION".equals(Naming.enumConstantName(Naming.param(annotation, "value")))) {
            return new UnionDeclaration(name, classInfo.getName(), comment, constants);
        }

        ScalarKind underlying = underlying(annotation);
        List<Value> implicit = constants.stream().map(c -> new Value(c, null)).toList();

        List<String> valueFields = classInfo.getDeclaredFieldInfo().stream()
                .filter(f -> f.hasAnnotation(FlatBufferEnumTypeField.class.getName()))
                .map(FieldInfo::getName).toList();
        if (valueFields.isEmpty()) {
            return new EnumDeclaration(name, classInfo.getName(), comment, underlying, implicit, null);
        }
        if (valueFields.size() > 1) {
            return new EnumDeclaration(name, classInfo.getName(), comment, underlying, implicit,
                    "has " + valueFields.size() + " @FlatBufferEnumTypeField fields (" + String.join(", ", valueFields)
                            + "); only one is allowed");
        }

        List<Value> explicit = readValues(classInfo, valueFields.get(0), underlying);
        if (explicit == null) {
            return new EnumDeclaration(name, classInfo.getName(), comment, underlying, implicit,
                    "could not read @FlatBufferEnumTypeField " + valueFields.get(0));
        }
        Comparator<Long> order = underlying == ScalarKind.ULONG ? Long::compareUnsigned : Long::compare;
        explicit.sort(Comparator.comparing(Value::value, order));
        return new EnumDeclaration(name, classInfo.getName(), comment, underlying, explicit, null);
    }

    /** {@code enumType} (every EnumType is an integral ScalarKind), defaulting to byte when absent. */
    private static ScalarKind underlying(AnnotationInfo annotation) {
        String constant = Naming.enumConstantName(Naming.param(annotation, "enumType"));
        return constant.isEmpty() ? ScalarKind.BYTE : ScalarKind.valueOf(constant);
    }

    /** Constant names in declaration order. */
    private static List<String> constantNames(ClassInfo classInfo) {
        List<String> names = new ArrayList<>();
        for (FieldInfo field : classInfo.getDeclaredFieldInfo()) {
            if (field.isEnum()) {
                names.add(field.getName());
            }
        }
        return names;
    }

    /** Reads the value field of every constant; null if the class cannot be loaded or the field is not integral. */
    @SuppressWarnings("PMD.AvoidAccessibilityAlteration")
    private static List<Value> readValues(ClassInfo classInfo, String fieldName, ScalarKind underlying) {
        Class<?> enumClass = classInfo.loadClass(true);
        if (enumClass == null) {
            return null;
        }
        try {
            Field field = enumClass.getDeclaredField(fieldName);
            field.setAccessible(true);
            List<Value> values = new ArrayList<>();
            for (Object constant : enumClass.getEnumConstants()) {
                Object raw = field.get(constant);
                if (!(raw instanceof Byte || raw instanceof Short || raw instanceof Integer || raw instanceof Long)) {
                    return null;
                }
                values.add(new Value(((Enum<?>) constant).name(), unsignedView((Number) raw, underlying)));
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
    private static long unsignedView(Number raw, ScalarKind underlying) {
        long value = raw.longValue();
        if (!underlying.isUnsigned() || value >= 0) {
            return value;
        }
        if (raw instanceof Byte) return value & 0xFFL;
        if (raw instanceof Short) return value & 0xFFFFL;
        if (raw instanceof Integer) return value & 0xFFFF_FFFFL;
        return value; // long: kept as-is and written unsigned for ulong
    }
}
