package com.michaelhradek.aurkitu.reflections;

import com.michaelhradek.aurkitu.reflections.util.ClasspathHelper;
import com.michaelhradek.aurkitu.reflections.util.ConfigurationBuilder;
import com.michaelhradek.aurkitu.reflections.util.FilterBuilder;
import junit.framework.Assert;
import org.junit.Test;

import java.util.Set;

public class ReflectionsExpandSupertypesTest {

    private final static String packagePrefix =
            "com.michaelhradek.aurkitu.reflections.ReflectionsExpandSupertypesTest\\$TestModel\\$ScannedScope\\$.*";
    private FilterBuilder inputsFilter = new FilterBuilder().include(packagePrefix);

    @Test
    public void testExpandSupertypes() throws Exception {
        Reflections refExpand = new Reflections(new ConfigurationBuilder().
                setUrls(ClasspathHelper.forClass(TestModel.ScannedScope.C.class)).
                filterInputsBy(inputsFilter));
        Assert.assertTrue(refExpand.getConfiguration().shouldExpandSuperTypes());
        Set<Class<? extends TestModel.A>> subTypesOf = refExpand.getSubTypesOf(TestModel.A.class);
        Assert.assertTrue("expanded", subTypesOf.contains(TestModel.B.class));
        Assert.assertTrue("transitivity", subTypesOf.containsAll(refExpand.getSubTypesOf(TestModel.B.class)));
    }

    @Test
    public void testNotExpandSupertypes() throws Exception {
        Reflections refDontExpand = new Reflections(new ConfigurationBuilder().
                setUrls(ClasspathHelper.forClass(TestModel.ScannedScope.C.class)).
                filterInputsBy(inputsFilter).
                setExpandSuperTypes(false));
        Assert.assertFalse(refDontExpand.getConfiguration().shouldExpandSuperTypes());
        Set<Class<? extends TestModel.A>> subTypesOf1 = refDontExpand.getSubTypesOf(TestModel.A.class);
        Assert.assertFalse(subTypesOf1.contains(TestModel.B.class));
    }

    public interface TestModel {
        interface A {
        } // outside of scanned scope

        interface B extends A {
        } // outside of scanned scope, but immediate supertype

        interface ScannedScope {
            interface C extends B {
            }

            interface D extends B {
            }
        }
    }
}
