package com.michaelhradek.aurkitu.reflections;

import com.michaelhradek.aurkitu.reflections.util.FilterBuilder;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test filtering
 */
public class FilterBuilderTest {

    @Test
    public void test_include() {
        FilterBuilder filter = new FilterBuilder().include("com\\.michaelhradek\\.aurkitu\\.reflections.*");
        assertTrue(filter.apply("com.michaelhradek.aurkitu.reflections.Reflections"));
        assertTrue(filter.apply("com.michaelhradek.aurkitu.reflections.foo.Reflections"));
        assertFalse(filter.apply("com.michaelhradek.aurkitu.foobar.Reflections"));
    }

    @Test
    public void test_includePackage() {
        FilterBuilder filter = new FilterBuilder().includePackage("com.michaelhradek.aurkitu.reflections");
        assertTrue(filter.apply("com.michaelhradek.aurkitu.reflections.Reflections"));
        assertTrue(filter.apply("com.michaelhradek.aurkitu.reflections.foo.Reflections"));
        assertFalse(filter.apply("com.michaelhradek.aurkitu.foobar.Reflections"));
    }

    @Test
    public void test_includePackageMultiple() {
        FilterBuilder filter = new FilterBuilder().includePackage("com.michaelhradek.aurkitu.reflections", "org.foo");
        assertTrue(filter.apply("com.michaelhradek.aurkitu.reflections.Reflections"));
        assertTrue(filter.apply("com.michaelhradek.aurkitu.reflections.foo.Reflections"));
        assertTrue(filter.apply("org.foo.Reflections"));
        assertTrue(filter.apply("org.foo.bar.Reflections"));
        assertFalse(filter.apply("org.bar.Reflections"));
    }

    @Test
    public void test_includePackagebyClass() {
        FilterBuilder filter = new FilterBuilder().includePackage(Reflections.class);
        assertTrue(filter.apply("com.michaelhradek.aurkitu.reflections.Reflections"));
        assertTrue(filter.apply("com.michaelhradek.aurkitu.reflections.foo.Reflections"));
        assertFalse(filter.apply("com.michaelhradek.aurkitu.foobar.Reflections"));
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_exclude() {
        FilterBuilder filter = new FilterBuilder().exclude("com\\.michaelhradek\\.aurkitu\\.reflections.*");
        assertFalse(filter.apply("com.michaelhradek.aurkitu.reflections.Reflections"));
        assertFalse(filter.apply("com.michaelhradek.aurkitu.reflections.foo.Reflections"));
        assertTrue(filter.apply("com.michaelhradek.aurkitu.foobar.Reflections"));
    }

    @Test
    public void test_excludePackage() {
        FilterBuilder filter = new FilterBuilder().excludePackage("com.michaelhradek.aurkitu.reflections");
        assertFalse(filter.apply("com.michaelhradek.aurkitu.reflections.Reflections"));
        assertFalse(filter.apply("com.michaelhradek.aurkitu.reflections.foo.Reflections"));
        assertTrue(filter.apply("com.michaelhradek.aurkitu.foobar.Reflections"));
    }

    @Test
    public void test_excludePackageByClass() {
        FilterBuilder filter = new FilterBuilder().excludePackage(Reflections.class);
        assertFalse(filter.apply("com.michaelhradek.aurkitu.reflections.Reflections"));
        assertFalse(filter.apply("com.michaelhradek.aurkitu.reflections.foo.Reflections"));
        assertTrue(filter.apply("com.michaelhradek.aurkitu.foobar.Reflections"));
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_parse_include() {
        FilterBuilder filter = FilterBuilder.parse("+com.michaelhradek.aurkitu.reflections.*");
        assertTrue(filter.apply("com.michaelhradek.aurkitu.reflections.Reflections"));
        assertTrue(filter.apply("com.michaelhradek.aurkitu.reflections.foo.Reflections"));
        assertFalse(filter.apply("com.michaelhradek.aurkitu.foobar.Reflections"));
        assertTrue(filter.apply("com.michaelhradek.aurkitu.reflectionsplus.Reflections"));
    }

    @Test
    public void test_parse_include_notRegex() {
        FilterBuilder filter = FilterBuilder.parse("+com.michaelhradek.aurkitu.reflections");
        assertFalse(filter.apply("com.michaelhradek.aurkitu.reflections.Reflections"));
        assertFalse(filter.apply("com.michaelhradek.aurkitu.reflections.foo.Reflections"));
        assertFalse(filter.apply("com.michaelhradek.aurkitu.foobar.Reflections"));
        assertFalse(filter.apply("com.michaelhradek.aurkitu.reflectionsplus.Reflections"));
    }

    @Test
    public void test_parse_exclude() {
        FilterBuilder filter = FilterBuilder.parse("-com.michaelhradek.aurkitu.reflections.*");
        assertFalse(filter.apply("com.michaelhradek.aurkitu.reflections.Reflections"));
        assertFalse(filter.apply("com.michaelhradek.aurkitu.reflections.foo.Reflections"));
        assertTrue(filter.apply("com.michaelhradek.aurkitu.foobar.Reflections"));
        assertFalse(filter.apply("com.michaelhradek.aurkitu.reflectionsplus.Reflections"));
    }

    @Test
    public void test_parse_exclude_notRegex() {
        FilterBuilder filter = FilterBuilder.parse("-com.michaelhradek.aurkitu.reflections");
        assertTrue(filter.apply("com.michaelhradek.aurkitu.reflections.Reflections"));
        assertTrue(filter.apply("com.michaelhradek.aurkitu.reflections.foo.Reflections"));
        assertTrue(filter.apply("com.michaelhradek.aurkitu.foobar.Reflections"));
        assertTrue(filter.apply("com.michaelhradek.aurkitu.reflectionsplus.Reflections"));
    }

    @Test
    public void test_parse_include_exclude() {
        FilterBuilder filter = FilterBuilder.parse("+com.michaelhradek.aurkitu.reflections.*, -com.michaelhradek.aurkitu.reflections.foo.*");
        assertTrue(filter.apply("com.michaelhradek.aurkitu.reflections.Reflections"));
        assertFalse(filter.apply("com.michaelhradek.aurkitu.reflections.foo.Reflections"));
        assertFalse(filter.apply("com.michaelhradek.aurkitu.foobar.Reflections"));
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_parsePackages_include() {
        FilterBuilder filter = FilterBuilder.parsePackages("+com.michaelhradek.aurkitu.reflections");
        assertTrue(filter.apply("com.michaelhradek.aurkitu.reflections.Reflections"));
        assertTrue(filter.apply("com.michaelhradek.aurkitu.reflections.foo.Reflections"));
        assertFalse(filter.apply("com.michaelhradek.aurkitu.foobar.Reflections"));
        assertFalse(filter.apply("com.michaelhradek.aurkitu.reflectionsplus.Reflections"));
    }

    @Test
    public void test_parsePackages_include_trailingDot() {
        FilterBuilder filter = FilterBuilder.parsePackages("+com.michaelhradek.aurkitu.reflections.");
        assertTrue(filter.apply("com.michaelhradek.aurkitu.reflections.Reflections"));
        assertTrue(filter.apply("com.michaelhradek.aurkitu.reflections.foo.Reflections"));
        assertFalse(filter.apply("com.michaelhradek.aurkitu.foobar.Reflections"));
        assertFalse(filter.apply("com.michaelhradek.aurkitu.reflectionsplus.Reflections"));
    }

    @Test
    public void test_parsePackages_exclude() {
        FilterBuilder filter = FilterBuilder.parsePackages("-com.michaelhradek.aurkitu.reflections");
        assertFalse(filter.apply("com.michaelhradek.aurkitu.reflections.Reflections"));
        assertFalse(filter.apply("com.michaelhradek.aurkitu.reflections.foo.Reflections"));
        assertTrue(filter.apply("com.michaelhradek.aurkitu.foobar.Reflections"));
        assertTrue(filter.apply("com.michaelhradek.aurkitu.reflectionsplus.Reflections"));
    }

    @Test
    public void test_parsePackages_exclude_trailingDot() {
        FilterBuilder filter = FilterBuilder.parsePackages("-com.michaelhradek.aurkitu.reflections.");
        assertFalse(filter.apply("com.michaelhradek.aurkitu.reflections.Reflections"));
        assertFalse(filter.apply("com.michaelhradek.aurkitu.reflections.foo.Reflections"));
        assertTrue(filter.apply("com.michaelhradek.aurkitu.foobar.Reflections"));
        assertTrue(filter.apply("com.michaelhradek.aurkitu.reflectionsplus.Reflections"));
    }

    @Test
    public void test_parsePackages_include_exclude() {
        FilterBuilder filter = FilterBuilder.parsePackages("+com.michaelhradek.aurkitu.reflections, -com.michaelhradek.aurkitu.reflections.foo");
        assertTrue(filter.apply("com.michaelhradek.aurkitu.reflections.Reflections"));
        assertFalse(filter.apply("com.michaelhradek.aurkitu.reflections.foo.Reflections"));
        assertFalse(filter.apply("com.michaelhradek.aurkitu.foobar.Reflections"));
    }

}
