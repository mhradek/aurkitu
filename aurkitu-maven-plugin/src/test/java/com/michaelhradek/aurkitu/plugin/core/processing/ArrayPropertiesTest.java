package com.michaelhradek.aurkitu.plugin.core.processing;

import com.michaelhradek.aurkitu.plugin.core.Processor;
import com.michaelhradek.aurkitu.plugin.core.output.Schema;
import com.michaelhradek.aurkitu.plugin.core.output.TypeDeclaration;
import com.michaelhradek.aurkitu.plugin.core.parsing.ArtifactReference;
import com.michaelhradek.aurkitu.plugin.test.SampleClassTable;
import org.apache.maven.project.MavenProject;
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ArrayPropertiesTest {

    @Test
    public void testProcessArray() throws NoSuchFieldException, IllegalAccessException {
        MavenProject mockMavenProject = Mockito.mock(MavenProject.class);
        ArtifactReference reference = new ArtifactReference(mockMavenProject, null, null, null, null);

        Field arrayField = SampleClassTable.class.getDeclaredField("anomalousSamples");

        Schema schema = new Schema();
        schema.setClasspathReferenceList(new ArrayList<>());

        Processor processor = new Processor()
                .withArtifactReference(reference)
                .withConsolidatedSchemas(false)
                .withSchema(schema);

        Field currentSchemaField = processor.getClass().getDeclaredField("currentSchema");
        currentSchemaField.setAccessible(true);
        currentSchemaField.set(processor, schema);

        TypeDeclaration.Property property = new ArrayProperties(processor).process(new TypeDeclaration.Property(), arrayField, false);

        // TODO Assert...

        Map<String, String> namespaceOverrideMap = new HashMap<>();
        processor.withNamespaceOverrideMap(namespaceOverrideMap);
        property = new ArrayProperties(processor).process(new TypeDeclaration.Property(), arrayField, false);

        // TODO ASsert...

        namespaceOverrideMap.put(SampleClassTable.class.getPackage().getName(), "other.test.package");
        processor.withNamespaceOverrideMap(namespaceOverrideMap);
        property = new ArrayProperties(processor).process(new TypeDeclaration.Property(), arrayField, true);
    }
}