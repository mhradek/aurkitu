package com.michaelhradek.aurkitu.core;

import org.junit.Assert;
import org.junit.Test;

public class UtilitiesTest {

    @Test
    public void testIsLowerCaseType() {

        Double primDouble = 123D;
        Assert.assertTrue(Utilities.isLowerCaseType(primDouble.getClass()));

        Float primFloat = 123F;
        Assert.assertTrue(Utilities.isLowerCaseType(primFloat.getClass()));

        Long primLong = 123L;
        Assert.assertTrue(Utilities.isLowerCaseType(primLong.getClass()));

        Integer primInteger = 123;
        Assert.assertTrue(Utilities.isLowerCaseType(primInteger.getClass()));

        Short primShort = 12;
        Assert.assertTrue(Utilities.isLowerCaseType(primShort.getClass()));

        Character primCharacter = 'd';
        Assert.assertTrue(Utilities.isLowerCaseType(primCharacter.getClass()));

        Byte primByte = 8;
        Assert.assertTrue(Utilities.isLowerCaseType(primByte.getClass()));

        Boolean primBoolean = true;
        Assert.assertTrue(Utilities.isLowerCaseType(primBoolean.getClass()));

        String primString = "Test string";
        Assert.assertTrue(Utilities.isLowerCaseType(primString.getClass()));
    }
}
