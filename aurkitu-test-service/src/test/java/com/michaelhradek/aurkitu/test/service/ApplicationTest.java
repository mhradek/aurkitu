package com.michaelhradek.aurkitu.test.service;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class ApplicationTest {

    // File locations
    private static final String OUTPUT_DIRECTORY = "target/aurkitu/schemas";
    private static final String FILENAME = "test-service-consolidated.fbs";

    // Test strings
    private static final String SCHEMA_NAMESPACE = "namespace com.michaelhradek.aurkitu.test.flatbuffers;";
    private static final String SCHEMA_ENUM = "enum UserState : byte { GUEST, ACTIVE, DISABLED, INACTIVE }";
    private static final String SCHEMA_TABLE_RESPONSE = "table Response {";
    private static final String SCHEMA_TABLE_REQUEST = "table Request {";
    private static final String SCHEMA_TABLE_WALLET = "table Wallet {";

    @Test
    public void testConsolidatedSchema() throws IOException {

        // File should exist at this point as the plugin runs during the Maven process-classes stage
        BufferedReader br = new BufferedReader(new FileReader(OUTPUT_DIRECTORY + File.separator + FILENAME));
        StringBuilder builder = new StringBuilder();
        String lineContents;
        while ((lineContents = br.readLine()) != null) {
            // The new lines are stripped via FileReader
            builder.append(lineContents);
            builder.append(System.lineSeparator());
        }

        // These definitions exist in the test schema file. Ordering is random hence the contains way of testing
        final String schemaFileContents = builder.toString();
        Assert.assertThat(schemaFileContents, CoreMatchers.containsString(SCHEMA_NAMESPACE));
        Assert.assertThat(schemaFileContents, CoreMatchers.containsString(SCHEMA_ENUM));
        Assert.assertThat(schemaFileContents, CoreMatchers.containsString(SCHEMA_TABLE_REQUEST));
        Assert.assertThat(schemaFileContents, CoreMatchers.containsString(SCHEMA_TABLE_RESPONSE));
        Assert.assertThat(schemaFileContents, CoreMatchers.containsString(SCHEMA_TABLE_WALLET));
    }
}