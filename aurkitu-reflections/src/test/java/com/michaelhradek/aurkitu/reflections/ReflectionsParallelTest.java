package com.michaelhradek.aurkitu.reflections;

import com.michaelhradek.aurkitu.reflections.scanners.*;
import com.michaelhradek.aurkitu.reflections.util.ClasspathHelper;
import com.michaelhradek.aurkitu.reflections.util.ConfigurationBuilder;
import org.junit.BeforeClass;

import static java.util.Arrays.asList;

/** */
public class ReflectionsParallelTest extends ReflectionsTest {

    @BeforeClass
    public static void init() {
        reflections = new Reflections(new ConfigurationBuilder()
                .setUrls(asList(ClasspathHelper.forClass(TestModel.class)))
                .filterInputsBy(TestModelFilter)
                .setScanners(
                        new SubTypesScanner(false),
                        new TypeAnnotationsScanner(),
                        new FieldAnnotationsScanner(),
                        new MethodAnnotationsScanner(),
                        new MethodParameterScanner(),
                        new MethodParameterNamesScanner(),
                        new MemberUsageScanner())
                .useParallelExecutor());
    }
}
