package io.github.mhradek.aurkitu.annotations;

import io.github.mhradek.aurkitu.annotations.types.FieldType;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/** Annotations and options added in 1.1.0: retention, targets and defaults. */
class NewAnnotationsTest {

    @FlatBufferTable
    static class Plain {
        @FlatBufferFieldOptions(fieldType = FieldType.INT)
        int value;
    }

    @FlatBufferEnum
    enum PlainEnum { A }

    @FlatBufferRpcService
    interface Service {
        Plain call(Plain request);
    }

    enum Union {
        @FlatBufferUnionMember(Plain.class)
        Alias
    }

    @Test
    void newOptionsDefaultToOff() throws Exception {
        FlatBufferTable table = Plain.class.getAnnotation(FlatBufferTable.class);
        assertFalse(table.originalOrder());
        assertEquals(0, table.forceAlign());
        assertEquals(0, table.attributes().length);

        FlatBufferEnum enumeration = PlainEnum.class.getAnnotation(FlatBufferEnum.class);
        assertFalse(enumeration.bitFlags());
        assertEquals(0, enumeration.attributes().length);

        FlatBufferFieldOptions options = Plain.class.getDeclaredField("value").getAnnotation(FlatBufferFieldOptions.class);
        assertEquals(0, options.attributes().length);

        assertEquals("", Service.class.getAnnotation(FlatBufferRpcService.class).namespace());
    }

    @Test
    void unionMemberNamesItsTable() throws Exception {
        FlatBufferUnionMember member = Union.class.getField("Alias").getAnnotation(FlatBufferUnionMember.class);
        assertEquals(Plain.class, member.value());
    }

    @Test
    void annotationsAreRuntimeVisibleWithTheDocumentedTargets() {
        for (Class<?> annotation : new Class<?>[]{FlatBufferUnionMember.class, FlatBufferRpcService.class}) {
            assertEquals(RetentionPolicy.RUNTIME, annotation.getAnnotation(Retention.class).value());
        }
        assertArrayEquals(new ElementType[]{ElementType.FIELD}, FlatBufferUnionMember.class.getAnnotation(Target.class).value());
        assertArrayEquals(new ElementType[]{ElementType.TYPE}, FlatBufferRpcService.class.getAnnotation(Target.class).value());
        assertTrue(Arrays.asList(FlatBufferComment.class.getAnnotation(Target.class).value()).contains(ElementType.METHOD));
        assertTrue(Arrays.asList(FlatBufferIgnore.class.getAnnotation(Target.class).value()).contains(ElementType.METHOD));
    }
}
