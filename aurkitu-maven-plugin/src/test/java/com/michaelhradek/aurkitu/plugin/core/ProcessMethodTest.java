package com.michaelhradek.aurkitu.plugin.core;

import com.michaelhradek.aurkitu.plugin.core.output.Schema;
import com.michaelhradek.aurkitu.plugin.core.output.TypeDeclaration.Property;
import com.michaelhradek.aurkitu.plugin.core.output.components.Namespace;
import com.michaelhradek.aurkitu.plugin.core.parsing.ArtifactReference;
import com.michaelhradek.aurkitu.plugin.test.SampleClassTable;
import org.apache.maven.project.MavenProject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @author m.hradek
 */
@RunWith(PowerMockRunner.class)
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

    @Test
    @PrepareForTest({Processor.class, Processor.ExternalClassDefinition.class, MavenProject.class})
    public void testProcessListWithExternalClassDefinition() throws Exception {
        MavenProject mockMavenProject = PowerMockito.mock(MavenProject.class);
        ArtifactReference reference = new ArtifactReference(mockMavenProject, null, null, null, null);

        Field arrayField = SampleClassTable.class.getDeclaredField("tokens");

        Schema schema = new Schema();
        schema.setClasspathReferenceList(new ArrayList<>());

        Field currentSchemaField = Processor.class.getDeclaredField("currentSchema");
        currentSchemaField.setAccessible(true);

        Processor processor = new Processor()
                .withArtifactReference(reference)
                .withConsolidatedSchemas(false)
                .withSchema(schema);
        currentSchemaField.set(processor, schema);

        Processor spyProcessor = PowerMockito.spy(processor);

        final Namespace namespace = new Namespace("sexy.new.namespace", null, "sexy-artifact");
        Processor.ExternalClassDefinition mockExternalClassDefinition = PowerMockito.mock(Processor.ExternalClassDefinition.class);
        mockExternalClassDefinition.locatedOutside = true;
        mockExternalClassDefinition.targetNamespace = namespace;

        PowerMockito.doReturn(mockExternalClassDefinition).when(spyProcessor, PowerMockito.method(Processor.class, "getExternalClassDefinitionDetails", Class.class)).withArguments(Mockito.any());
        Property property = spyProcessor.processList(new Property(), arrayField, false);

        Assert.assertEquals(namespace.toString() + ".String", property.options.get(Property.PropertyOptionKey.ARRAY));
    }
}
