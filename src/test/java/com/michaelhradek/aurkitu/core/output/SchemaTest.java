package com.michaelhradek.aurkitu.core.output;

import org.junit.Assert;
import org.junit.Test;

public class SchemaTest {

    @Test
    public void testSetFileIdentifier() {
        Schema schema = new Schema();
        Assert.assertEquals(null, schema.getFileIdentifier());

        schema.setFileIdentifier("INVALID_LENGTH");
        Assert.assertEquals(null, schema.getFileIdentifier());

        final String validFileIdentifier = "VLID";
        schema.setFileIdentifier(validFileIdentifier);
        Assert.assertEquals(validFileIdentifier, schema.getFileIdentifier());
    }
}