package com.michaelhradek.aurkitu.reflections;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.michaelhradek.aurkitu.reflections.scanners.*;
import com.michaelhradek.aurkitu.reflections.util.ClasspathHelper;
import com.michaelhradek.aurkitu.reflections.util.ConfigurationBuilder;
import com.michaelhradek.aurkitu.reflections.util.FilterBuilder;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.annotation.Nullable;
import java.io.File;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static com.michaelhradek.aurkitu.reflections.util.Utils.index;
import static java.util.Arrays.asList;
import static org.junit.Assert.*;

/**
 *
 */
@SuppressWarnings("unchecked")
public class ReflectionsTest {
    public static final FilterBuilder TestModelFilter = new FilterBuilder().include("com.michaelhradek.aurkitu.reflections.TestModel\\$.*");
    static Reflections reflections;
    private final BaseMatcher<Set<Class<?>>> isEmpty = new BaseMatcher<Set<Class<?>>>() {
        public boolean matches(Object o) {
            return ((Collection<?>) o).isEmpty();
        }

        public void describeTo(Description description) {
            description.appendText("empty collection");
        }
    };

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
                        new MemberUsageScanner()));
    }

    //
    public static String getUserDir() {
        File file = new File(System.getProperty("user.dir"));
        //a hack to fix user.dir issue(?) in surefire
        if (Lists.newArrayList(file.list()).contains("reflections")) {
            file = new File(file, "reflections");
        }
        return file.getAbsolutePath();
    }

    public static <T> Matcher<Set<? super T>> are(final T... ts) {
        final Collection<?> c1 = Arrays.asList(ts);
        return new Match<Set<? super T>>() {
            public boolean matches(Object o) {
                Collection<?> c2 = (Collection<?>) o;
                return c1.containsAll(c2) && c2.containsAll(c1);
            }
        };
    }

    @Test
    public void testSubTypesOf() {
        assertThat(reflections.getSubTypesOf(TestModel.I1.class), are(TestModel.I2.class, TestModel.C1.class, TestModel.C2.class, TestModel.C3.class, TestModel.C5.class));
        assertThat(reflections.getSubTypesOf(TestModel.C1.class), are(TestModel.C2.class, TestModel.C3.class, TestModel.C5.class));

        assertFalse("getAllTypes should not be empty when Reflections is configured with SubTypesScanner(false)",
                reflections.getAllTypes().isEmpty());
    }

    @Test
    public void testTypesAnnotatedWith() {
        assertThat(reflections.getTypesAnnotatedWith(TestModel.MAI1.class, true), are(TestModel.AI1.class));
        assertThat(reflections.getTypesAnnotatedWith(TestModel.MAI1.class, true), annotatedWith(TestModel.MAI1.class));

        assertThat(reflections.getTypesAnnotatedWith(TestModel.AI2.class, true), are(TestModel.I2.class));
        assertThat(reflections.getTypesAnnotatedWith(TestModel.AI2.class, true), annotatedWith(TestModel.AI2.class));

        assertThat(reflections.getTypesAnnotatedWith(TestModel.AC1.class, true), are(TestModel.C1.class, TestModel.C2.class, TestModel.C3.class, TestModel.C5.class));
        assertThat(reflections.getTypesAnnotatedWith(TestModel.AC1.class, true), annotatedWith(TestModel.AC1.class));

        assertThat(reflections.getTypesAnnotatedWith(TestModel.AC1n.class, true), are(TestModel.C1.class));
        assertThat(reflections.getTypesAnnotatedWith(TestModel.AC1n.class, true), annotatedWith(TestModel.AC1n.class));

        assertThat(reflections.getTypesAnnotatedWith(TestModel.MAI1.class), are(TestModel.AI1.class, TestModel.I1.class, TestModel.I2.class, TestModel.C1.class, TestModel.C2.class, TestModel.C3.class, TestModel.C5.class));
        assertThat(reflections.getTypesAnnotatedWith(TestModel.MAI1.class), metaAnnotatedWith(TestModel.MAI1.class));

        assertThat(reflections.getTypesAnnotatedWith(TestModel.AI1.class), are(TestModel.I1.class, TestModel.I2.class, TestModel.C1.class, TestModel.C2.class, TestModel.C3.class, TestModel.C5.class));
        assertThat(reflections.getTypesAnnotatedWith(TestModel.AI1.class), metaAnnotatedWith(TestModel.AI1.class));

        assertThat(reflections.getTypesAnnotatedWith(TestModel.AI2.class), are(TestModel.I2.class, TestModel.C1.class, TestModel.C2.class, TestModel.C3.class, TestModel.C5.class));
        assertThat(reflections.getTypesAnnotatedWith(TestModel.AI2.class), metaAnnotatedWith(TestModel.AI2.class));

        assertThat(reflections.getTypesAnnotatedWith(TestModel.AM1.class), isEmpty);

        //annotation member value matching
        TestModel.AC2 ac2 = new TestModel.AC2() {
            public String value() {return "ugh?!";}

            public Class<? extends Annotation> annotationType() {
                return TestModel.AC2.class;
            }
        };

        assertThat(reflections.getTypesAnnotatedWith(ac2), are(TestModel.C3.class, TestModel.C5.class, TestModel.I3.class, TestModel.C6.class, TestModel.AC3.class, TestModel.C7.class));

        assertThat(reflections.getTypesAnnotatedWith(ac2, true), are(TestModel.C3.class, TestModel.I3.class, TestModel.AC3.class));
    }

    @Test
    public void testMethodsAnnotatedWith() {
        try {
            assertThat(reflections.getMethodsAnnotatedWith(TestModel.AM1.class),
                    are(TestModel.C4.class.getDeclaredMethod("m1"),
                            TestModel.C4.class.getDeclaredMethod("m1", int.class, String[].class),
                            TestModel.C4.class.getDeclaredMethod("m1", int[][].class, String[][].class),
                            TestModel.C4.class.getDeclaredMethod("m3")));

            TestModel.AM1 am1 = new TestModel.AM1() {
                public String value() {return "1";}

                public Class<? extends Annotation> annotationType() {
                    return TestModel.AM1.class;
                }
            };
            assertThat(reflections.getMethodsAnnotatedWith(am1),
                    are(TestModel.C4.class.getDeclaredMethod("m1"),
                            TestModel.C4.class.getDeclaredMethod("m1", int.class, String[].class),
                            TestModel.C4.class.getDeclaredMethod("m1", int[][].class, String[][].class)));
        } catch (NoSuchMethodException e) {
            fail();
        }
    }

    @Test
    public void testConstructorsAnnotatedWith() {
        try {
            assertThat(reflections.getConstructorsAnnotatedWith(TestModel.AM1.class),
                    are(TestModel.C4.class.getDeclaredConstructor(String.class)));

            TestModel.AM1 am1 = new TestModel.AM1() {
                public String value() {return "1";}

                public Class<? extends Annotation> annotationType() {
                    return TestModel.AM1.class;
                }
            };
            assertThat(reflections.getConstructorsAnnotatedWith(am1),
                    are(TestModel.C4.class.getDeclaredConstructor(String.class)));
        } catch (NoSuchMethodException e) {
            fail();
        }
    }

    @Test
    public void testFieldsAnnotatedWith() {
        try {
            assertThat(reflections.getFieldsAnnotatedWith(TestModel.AF1.class),
                    are(TestModel.C4.class.getDeclaredField("f1"),
                            TestModel.C4.class.getDeclaredField("f2")
                        ));

            assertThat(reflections.getFieldsAnnotatedWith(new TestModel.AF1() {
                            public String value() {return "2";}

                        public Class<? extends Annotation> annotationType() {
                            return TestModel.AF1.class;
                        }
                    }),
                    are(TestModel.C4.class.getDeclaredField("f2")));
        } catch (NoSuchFieldException e) {
            fail();
        }
    }

    @Test
    public void testMethodParameter() {
        try {
            assertThat(reflections.getMethodsMatchParams(String.class),
                    are(TestModel.C4.class.getDeclaredMethod("m4", String.class), TestModel.Usage.C1.class.getDeclaredMethod("method", String.class)));

            assertThat(reflections.getMethodsMatchParams(),
                    are(TestModel.C4.class.getDeclaredMethod("m1"), TestModel.C4.class.getDeclaredMethod("m3"),
                            TestModel.AC2.class.getMethod("value"), TestModel.AF1.class.getMethod("value"), TestModel.AM1.class.getMethod("value"),
                            TestModel.Usage.C1.class.getDeclaredMethod("method"), TestModel.Usage.C2.class.getDeclaredMethod("method")));

            assertThat(reflections.getMethodsMatchParams(int[][].class, String[][].class),
                    are(TestModel.C4.class.getDeclaredMethod("m1", int[][].class, String[][].class)));

            assertThat(reflections.getMethodsReturn(int.class),
                    are(TestModel.C4.class.getDeclaredMethod("add", int.class, int.class)));

            assertThat(reflections.getMethodsReturn(String.class),
                    are(TestModel.C4.class.getDeclaredMethod("m3"), TestModel.C4.class.getDeclaredMethod("m4", String.class),
                            TestModel.AC2.class.getMethod("value"), TestModel.AF1.class.getMethod("value"), TestModel.AM1.class.getMethod("value")));

            assertThat(reflections.getMethodsReturn(void.class),
                    are(TestModel.C4.class.getDeclaredMethod("m1"), TestModel.C4.class.getDeclaredMethod("m1", int.class, String[].class),
                            TestModel.C4.class.getDeclaredMethod("m1", int[][].class, String[][].class), TestModel.Usage.C1.class.getDeclaredMethod("method"),
                            TestModel.Usage.C1.class.getDeclaredMethod("method", String.class), TestModel.Usage.C2.class.getDeclaredMethod("method")));

            assertThat(reflections.getMethodsWithAnyParamAnnotated(TestModel.AM1.class),
                    are(TestModel.C4.class.getDeclaredMethod("m4", String.class)));

            assertThat(reflections.getMethodsWithAnyParamAnnotated(
                    new TestModel.AM1() {
                        public String value() { return "2"; }

                        public Class<? extends Annotation> annotationType() {
                            return TestModel.AM1.class;
                        }
                    }),
                    are(TestModel.C4.class.getDeclaredMethod("m4", String.class)));
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testConstructorParameter() throws NoSuchMethodException {
        assertThat(reflections.getConstructorsMatchParams(String.class),
                are(TestModel.C4.class.getDeclaredConstructor(String.class)));

        assertThat(reflections.getConstructorsMatchParams(),
                are(TestModel.C1.class.getDeclaredConstructor(), TestModel.C2.class.getDeclaredConstructor(), TestModel.C3.class.getDeclaredConstructor(),
                        TestModel.C4.class.getDeclaredConstructor(), TestModel.C5.class.getDeclaredConstructor(), TestModel.C6.class.getDeclaredConstructor(),
                        TestModel.C7.class.getDeclaredConstructor(), TestModel.Usage.C1.class.getDeclaredConstructor(), TestModel.Usage.C2.class.getDeclaredConstructor()));

        assertThat(reflections.getConstructorsWithAnyParamAnnotated(TestModel.AM1.class),
                are(TestModel.C4.class.getDeclaredConstructor(String.class)));

        assertThat(reflections.getConstructorsWithAnyParamAnnotated(
                new TestModel.AM1() {
                    public String value() { return "1"; }

                    public Class<? extends Annotation> annotationType() {
                        return TestModel.AM1.class;
                    }
                }),
                are(TestModel.C4.class.getDeclaredConstructor(String.class)));
    }

    @Test
    public void testResourcesScanner() {
        Predicate<String> filter = new FilterBuilder().include(".*\\.xml").exclude(".*testModel-reflections\\.xml");
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .filterInputsBy(filter)
                .setScanners(new ResourcesScanner())
                .setUrls(asList(ClasspathHelper.forClass(TestModel.class))));

        Set<String> resolved = reflections.getResources(Pattern.compile(".*resource1-reflections\\.xml"));
        assertThat(resolved, are("META-INF/reflections/resource1-reflections.xml"));

        Set<String> resources = reflections.getStore().get(index(ResourcesScanner.class)).keySet();
        assertThat(resources, are("resource1-reflections.xml", "resource2-reflections.xml"));
    }

    @Test
    public void testMethodParameterNames() throws NoSuchMethodException {
        assertEquals(reflections.getMethodParamNames(TestModel.C4.class.getDeclaredMethod("m3")),
                Lists.newArrayList());

        assertEquals(reflections.getMethodParamNames(TestModel.C4.class.getDeclaredMethod("m4", String.class)),
                Lists.newArrayList("string"));

        assertEquals(reflections.getMethodParamNames(TestModel.C4.class.getDeclaredMethod("add", int.class, int.class)),
                Lists.newArrayList("i1", "i2"));

        assertEquals(reflections.getConstructorParamNames(TestModel.C4.class.getDeclaredConstructor(String.class)),
                Lists.newArrayList("f1"));
    }

    @Test
    public void testMemberUsageScanner() throws NoSuchFieldException, NoSuchMethodException {
        //field usage
        assertThat(reflections.getFieldUsage(TestModel.Usage.C1.class.getDeclaredField("c2")),
                are(TestModel.Usage.C1.class.getDeclaredConstructor(),
                        TestModel.Usage.C1.class.getDeclaredConstructor(TestModel.Usage.C2.class),
                        TestModel.Usage.C1.class.getDeclaredMethod("method"),
                        TestModel.Usage.C1.class.getDeclaredMethod("method", String.class)));

        //method usage
        assertThat(reflections.getMethodUsage(TestModel.Usage.C1.class.getDeclaredMethod("method")),
                are(TestModel.Usage.C2.class.getDeclaredMethod("method")));

        assertThat(reflections.getMethodUsage(TestModel.Usage.C1.class.getDeclaredMethod("method", String.class)),
                are(TestModel.Usage.C2.class.getDeclaredMethod("method")));

        //constructor usage
        assertThat(reflections.getConstructorUsage(TestModel.Usage.C1.class.getDeclaredConstructor()),
                are(TestModel.Usage.C2.class.getDeclaredConstructor(),
                        TestModel.Usage.C2.class.getDeclaredMethod("method")));

        assertThat(reflections.getConstructorUsage(TestModel.Usage.C1.class.getDeclaredConstructor(TestModel.Usage.C2.class)),
                are(TestModel.Usage.C2.class.getDeclaredMethod("method")));
    }

    @Test
    public void testScannerNotConfigured() {
        try {
            new Reflections(TestModel.class, TestModelFilter).getMethodsAnnotatedWith(TestModel.AC1.class);
            fail();
        } catch (ReflectionsException e) {
            assertEquals(e.getMessage(), "Scanner " + MethodAnnotationsScanner.class.getSimpleName() + " was not configured");
        }
    }

    private Matcher<Set<Class<?>>> annotatedWith(final Class<? extends Annotation> annotation) {
        return new Match<Set<Class<?>>>() {
            public boolean matches(Object o) {
                for (Class<?> c : (Iterable<Class<?>>) o) {
                    if (!Iterables.contains(annotationTypes(Arrays.asList(c.getAnnotations())), annotation)) return false;
                }
                return true;
            }
        };
    }

    private Matcher<Set<Class<?>>> metaAnnotatedWith(final Class<? extends Annotation> annotation) {
        return new Match<Set<Class<?>>>() {
            public boolean matches(Object o) {
                for (Class<?> c : (Iterable<Class<?>>) o) {
                    Set<Class> result = Sets.newHashSet();
                    List<Class> stack = Lists.newArrayList(ReflectionUtils.getAllSuperTypes(c));
                    while (!stack.isEmpty()) {
                        Class next = stack.remove(0);
                        if (result.add(next)) {
                            for (Class<? extends Annotation> ac : annotationTypes(Arrays.asList(next.getDeclaredAnnotations()))) {
                                if (!result.contains(ac) && !stack.contains(ac)) stack.add(ac);
                            }
                        }
                    }
                    if (!result.contains(annotation)) return false;
                }
                return true;
            }
        };
    }

    private Iterable<Class<? extends Annotation>> annotationTypes(Iterable<Annotation> annotations) {
        return Iterables.transform(annotations, new Function<Annotation, Class<? extends Annotation>>() {
            @Nullable
            public Class<? extends Annotation> apply(@Nullable Annotation input) {
                return input != null ? input.annotationType() : null;
            }
        });
    }

    private abstract static class Match<T> extends BaseMatcher<T> {
        public void describeTo(Description description) { }
    }
}
