package com.michaelhradek.aurkitu.reflections;

import com.google.common.base.Predicate;
import com.michaelhradek.aurkitu.reflections.scanners.TypeElementsScanner;
import com.michaelhradek.aurkitu.reflections.serializers.JavaCodeSerializer;
import com.michaelhradek.aurkitu.reflections.util.ClasspathHelper;
import com.michaelhradek.aurkitu.reflections.util.ConfigurationBuilder;
import com.michaelhradek.aurkitu.reflections.util.FilterBuilder;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

/**
 *
 */
public class JavaCodeSerializerTest {

    @BeforeClass
    public static void generateAndSave() {
        Predicate<String> filter = new FilterBuilder().include("com.michaelhradek.aurkitu.reflections.TestModel\\$.*");

        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .filterInputsBy(filter)
                .setScanners(new TypeElementsScanner().includeFields().publicOnly(false))
                .setUrls(asList(ClasspathHelper.forClass(TestModel.class))));

        //save
        String filename = ReflectionsTest.getUserDir() + "/src/test/java/com.michaelhradek.aurkitu.reflections.MyTestModelStore";
        reflections.save(filename, new JavaCodeSerializer());
    }

    @Test
    public void resolve() throws NoSuchMethodException, NoSuchFieldException {
        //class
        assertEquals(TestModel.C1.class,
                JavaCodeSerializer.resolveClass(MyTestModelStore.com.michaelhradek.aurkitu.reflections.TestModel$C1.class));

        //method
        assertEquals(TestModel.C4.class.getDeclaredMethod("m1"),
                JavaCodeSerializer.resolveMethod(MyTestModelStore.com.michaelhradek.aurkitu.reflections.TestModel$C4.methods.m1.class));

        //overloaded method with parameters
        assertEquals(TestModel.C4.class.getDeclaredMethod("m1", int.class, String[].class),
                JavaCodeSerializer.resolveMethod(MyTestModelStore.com.michaelhradek.aurkitu.reflections.TestModel$C4.methods.m1_int__java_lang_String$$.class));

        //overloaded method with parameters and multi dimensional array
        assertEquals(TestModel.C4.class.getDeclaredMethod("m1", int[][].class, String[][].class),
                JavaCodeSerializer.resolveMethod(MyTestModelStore.com.michaelhradek.aurkitu.reflections.TestModel$C4.methods.m1_int$$$$__java_lang_String$$$$.class));

        //field
        assertEquals(TestModel.C4.class.getDeclaredField("f1"),
                JavaCodeSerializer.resolveField(MyTestModelStore.com.michaelhradek.aurkitu.reflections.TestModel$C4.fields.f1.class));

        //annotation
        Assert.assertEquals(TestModel.C2.class.getAnnotation(TestModel.AC2.class),
                JavaCodeSerializer.resolveAnnotation(MyTestModelStore.com.michaelhradek.aurkitu.reflections.TestModel$C2.annotations.com_michaelhradek_aurkitu_reflections_TestModel$AC2.class));
    }
}
