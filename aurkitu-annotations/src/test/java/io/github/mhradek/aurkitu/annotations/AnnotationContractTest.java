package io.github.mhradek.aurkitu.annotations;

import io.github.mhradek.aurkitu.annotations.types.EnumType;
import io.github.mhradek.aurkitu.annotations.types.FieldType;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.LOCAL_VARIABLE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The public contract of every annotation: runtime retention, where it may be used, and every element
 * with its exact default. The plugin reads class files, where omitted elements are absent, so it assumes
 * these defaults itself; a changed default must fail here and be changed in the plugin too.
 */
class AnnotationContractTest {

    /** Marks an element that has no default and so must be given. */
    private static final Object REQUIRED = new Object() {
        @Override
        public String toString() {
            return "<required>";
        }
    };

    @Test
    void flatBufferTable() {
        assertContract(FlatBufferTable.class, Set.of(TYPE), Map.of(
                "rootType", false,
                "value", FlatBufferTable.TableStructureType.TABLE,
                "namespace", "",
                "originalOrder", false,
                "forceAlign", 0,
                "attributes", List.of()));
        assertEquals(List.of("TABLE", "STRUCT"), names(FlatBufferTable.TableStructureType.values()));
    }

    @Test
    void flatBufferEnum() {
        assertContract(FlatBufferEnum.class, null, Map.of(
                "value", FlatBufferEnum.EnumStructureType.ENUM,
                "enumType", EnumType.BYTE,
                "namespace", "",
                "bitFlags", false,
                "attributes", List.of()));
        assertEquals(List.of("ENUM", "UNION"), names(FlatBufferEnum.EnumStructureType.values()));
        assertEquals("enum", FlatBufferEnum.EnumStructureType.ENUM.toString());
        assertEquals("union", FlatBufferEnum.EnumStructureType.UNION.toString());
    }

    @Test
    void flatBufferFieldOptions() {
        Map<String, Object> defaults = new LinkedHashMap<>();
        defaults.put("fieldType", REQUIRED);
        defaults.put("useFullName", false);
        defaults.put("defaultValue", "");
        defaults.put("required", false);
        defaults.put("deprecated", false);
        defaults.put("fieldId", -1);
        defaults.put("key", false);
        defaults.put("hash", "");
        defaults.put("nestedFlatbuffer", "");
        defaults.put("flexbuffer", false);
        defaults.put("forceAlign", 0);
        defaults.put("fixedLength", 0);
        defaults.put("attributes", List.of());
        assertContract(FlatBufferFieldOptions.class, Set.of(FIELD, PARAMETER, LOCAL_VARIABLE), defaults);
    }

    @Test
    void flatBufferComment() {
        assertContract(FlatBufferComment.class, Set.of(FIELD, PARAMETER, LOCAL_VARIABLE, TYPE, METHOD),
                Map.of("comment", REQUIRED));
    }

    @Test
    void markerAnnotations() {
        assertContract(FlatBufferIgnore.class, Set.of(FIELD, TYPE, METHOD), Map.of());
        assertContract(FlatBufferEnumTypeField.class, null, Map.of());
    }

    @Test
    void flatBufferRpcServiceAndUnionMember() {
        assertContract(FlatBufferRpcService.class, Set.of(TYPE), Map.of("namespace", ""));
        assertContract(FlatBufferUnionMember.class, Set.of(FIELD), Map.of("value", REQUIRED));
        assertEquals(Class.class, method(FlatBufferUnionMember.class, "value").getReturnType());
    }

    @Test
    @SuppressWarnings("removal") // covers a deprecated annotation
    void flatBufferOptionsIsDeprecatedForRemoval() {
        assertContract(FlatBufferOptions.class, null, Map.of());
        Deprecated deprecated = FlatBufferOptions.class.getAnnotation(Deprecated.class);
        assertNotNull(deprecated);
        assertTrue(deprecated.forRemoval());
        assertEquals("1.0.1", deprecated.since());
    }

    @Test
    void valuesAreReadableAtRuntime() throws Exception {
        @FlatBufferTable(rootType = true, value = FlatBufferTable.TableStructureType.STRUCT, attributes = "a: 1")
        class Annotated {
            @FlatBufferFieldOptions(fieldType = FieldType.UINT, fieldId = 3, attributes = {"x", "y: 2"})
            int field;
        }
        FlatBufferTable table = Annotated.class.getAnnotation(FlatBufferTable.class);
        assertTrue(table.rootType());
        assertEquals(FlatBufferTable.TableStructureType.STRUCT, table.value());
        assertArrayEquals(new String[]{"a: 1"}, table.attributes());
        FlatBufferFieldOptions options = Annotated.class.getDeclaredField("field").getAnnotation(FlatBufferFieldOptions.class);
        assertEquals(FieldType.UINT, options.fieldType());
        assertEquals(3, options.fieldId());
        assertArrayEquals(new String[]{"x", "y: 2"}, options.attributes());
    }

    private static void assertContract(Class<? extends Annotation> annotation, Set<ElementType> targets,
                                       Map<String, Object> defaults) {
        assertTrue(annotation.isAnnotation());
        Retention retention = annotation.getAnnotation(Retention.class);
        assertNotNull(retention, annotation + " needs @Retention(RUNTIME)");
        assertEquals(RetentionPolicy.RUNTIME, retention.value(), annotation.getName());

        Target target = annotation.getAnnotation(Target.class);
        if (targets == null) {
            assertNull(target, annotation + ": no @Target, so usable on any declaration");
        } else {
            assertNotNull(target, annotation + " needs @Target");
            assertEquals(targets, Set.of(target.value()), annotation.getName());
            assertEquals(target.value().length, targets.size(), annotation + ": duplicate targets");
        }

        Map<String, Object> actual = new TreeMap<>();
        for (Method element : annotation.getDeclaredMethods()) {
            Object value = element.getDefaultValue();
            actual.put(element.getName(), value == null ? REQUIRED : value.getClass().isArray() ? asList(value) : value);
        }
        assertEquals(new TreeMap<>(defaults), actual, annotation.getName() + " elements and defaults");
    }

    private static List<Object> asList(Object array) {
        return java.util.stream.IntStream.range(0, Array.getLength(array)).mapToObj(i -> Array.get(array, i)).toList();
    }

    private static Method method(Class<?> type, String name) {
        return Arrays.stream(type.getDeclaredMethods()).filter(m -> m.getName().equals(name)).findFirst().orElseThrow();
    }

    private static List<String> names(Enum<?>[] values) {
        return Arrays.stream(values).map(Enum::name).collect(Collectors.toList());
    }
}
