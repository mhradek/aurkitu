package io.github.mhradek.aurkitu.reflections;

import io.github.mhradek.aurkitu.reflections.scanners.*;
import io.github.mhradek.aurkitu.reflections.util.ClasspathHelper;
import io.github.mhradek.aurkitu.reflections.util.ConfigurationBuilder;
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
