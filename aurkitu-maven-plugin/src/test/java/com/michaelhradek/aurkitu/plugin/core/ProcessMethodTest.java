package com.michaelhradek.aurkitu.plugin.core;

import com.michaelhradek.aurkitu.plugin.core.output.Schema;
import com.michaelhradek.aurkitu.plugin.core.output.TypeDeclaration.Property;
import com.michaelhradek.aurkitu.plugin.core.parsing.ArtifactReference;
import com.michaelhradek.aurkitu.plugin.test.SampleClassTable;
import org.apache.maven.project.MavenProject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @author m.hradek
 */
@RunWith(MockitoJUnitRunner.class)
public class ProcessMethodTest {

    @Test
    public void testProcessClass() throws NoSuchFieldException, IllegalAccessException {
        MavenProject mockMavenProject = Mockito.mock(MavenProject.class);
        ArtifactReference reference = new ArtifactReference(mockMavenProject, null, null, null, null);

        Field tableField = SampleClassTable.class.getDeclaredField("fullnameClass");

        Schema schema = new Schema();
        schema.setClasspathReferenceList(new ArrayList<>());

        Processor processor = new Processor()
                .withArtifactReference(reference)
                .withConsolidatedSchemas(false)
                .withSchema(schema);

        Field currentSchemaField = processor.getClass().getDeclaredField("currentSchema");
        currentSchemaField.setAccessible(true);
        currentSchemaField.set(processor, schema);

        Property property = processor.processClass(new Property(), tableField, false);

        // TODO Assert...

        Map<String, String> namespaceOverrideMap = new HashMap<>();
        processor.withNamespaceOverrideMap(namespaceOverrideMap);
        property = processor.processClass(new Property(), tableField, false);

        // TODO Assert...

        namespaceOverrideMap.put(SampleClassTable.class.getPackage().getName(), "other.test.package");
        processor.withNamespaceOverrideMap(namespaceOverrideMap);
        property = processor.processClass(new Property(), tableField, true);

        // TODO Assert...

        ArtifactReference mockReference = Mockito.mock(ArtifactReference.class);
        Mockito.when(mockReference.getMavenProject()).thenThrow(Exception.class);

        processor.withArtifactReference(mockReference);
        property = processor.processClass(new Property(), tableField, false);

        // TODO Assert...

        property = processor.processClass(new Property(), tableField, true);

        // TODO Assert...
    }

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

        Property property = processor.processArray(new Property(), arrayField, false);

        // TODO Assert...

        Map<String, String> namespaceOverrideMap = new HashMap<>();
        processor.withNamespaceOverrideMap(namespaceOverrideMap);
        property = processor.processArray(new Property(), arrayField, false);

        // TODO ASsert...

        namespaceOverrideMap.put(SampleClassTable.class.getPackage().getName(), "other.test.package");
        processor.withNamespaceOverrideMap(namespaceOverrideMap);
        property = processor.processArray(new Property(), arrayField, true);
    }

    @Test
    public void testProcessMap() throws NoSuchFieldException, IllegalAccessException {
        MavenProject mockMavenProject = Mockito.mock(MavenProject.class);
        ArtifactReference reference = new ArtifactReference(mockMavenProject, null, null, null, null);

        Field mapField = SampleClassTable.class.getDeclaredField("dataMap");

        Schema schema = new Schema();
        schema.setClasspathReferenceList(new ArrayList<>());

        Processor processor = new Processor()
                .withArtifactReference(reference)
                .withConsolidatedSchemas(false)
                .withSchema(schema);

        Field currentSchemaField = processor.getClass().getDeclaredField("currentSchema");
        currentSchemaField.setAccessible(true);
        currentSchemaField.set(processor, schema);
        Property property = processor.processMap(new Property(), schema, mapField, false);

        // TODO ASsert...

        ArtifactReference mockReference = Mockito.mock(ArtifactReference.class);
        Mockito.when(mockReference.getMavenProject()).thenThrow(Exception.class);
        processor.withArtifactReference(mockReference);
        property = processor.processMap(new Property(), schema, mapField, false);

        // TODO ASsert...
    }

    @Test
    public void testProcessList() throws NoSuchFieldException, IllegalAccessException {
        MavenProject mockMavenProject = Mockito.mock(MavenProject.class);
        ArtifactReference reference = new ArtifactReference(mockMavenProject, null, null, null, null);

        Field arrayField = SampleClassTable.class.getDeclaredField("tokens");

        Schema schema = new Schema();
        schema.setClasspathReferenceList(new ArrayList<>());

        Processor processor = new Processor()
                .withArtifactReference(reference)
                .withConsolidatedSchemas(false)
                .withSchema(schema);

        Field currentSchemaField = processor.getClass().getDeclaredField("currentSchema");
        currentSchemaField.setAccessible(true);
        currentSchemaField.set(processor, schema);

        Property property = processor.processList(new Property(), arrayField, false);

        ArtifactReference mockReference = Mockito.mock(ArtifactReference.class);
        Mockito.when(mockReference.getMavenProject()).thenThrow(Exception.class);
        processor.withArtifactReference(mockReference);
        property = processor.processList(new Property(), arrayField, false);
    }
}
