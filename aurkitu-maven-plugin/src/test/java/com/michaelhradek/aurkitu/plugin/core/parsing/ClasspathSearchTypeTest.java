package com.michaelhradek.aurkitu.plugin.core.parsing;

import org.junit.Assert;
import org.junit.Test;

public class ClasspathSearchTypeTest {

    @Test
    public void testClasspathSearchType() {

        for (ClasspathSearchType type : ClasspathSearchType.values()) {
            switch (type) {
                case BOTH:
                case DEPENDENCIES:
                case PROJECT:
                    break;
                default:
                    Assert.fail("Unexpected ClasspathSearchType");
                    break;
            }
        }
    }
}
