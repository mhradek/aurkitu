package com.michaelhradek.aurkitu.plugin.core.processing;

import com.michaelhradek.aurkitu.plugin.core.Processor;
import com.michaelhradek.aurkitu.plugin.core.output.Schema;
import com.michaelhradek.aurkitu.plugin.core.output.TypeDeclaration;
import com.michaelhradek.aurkitu.plugin.core.output.components.Namespace;
import com.michaelhradek.aurkitu.plugin.core.parsing.ArtifactReference;
import com.michaelhradek.aurkitu.plugin.test.SampleClassTable;
import org.apache.maven.project.MavenProject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@RunWith(PowerMockRunner.class)
public class ClassPropertiesTest {

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

        TypeDeclaration.Property property = new ClassProperties(processor).process(new TypeDeclaration.Property(), tableField, false);

        // TODO Assert...

        Map<String, String> namespaceOverrideMap = new HashMap<>();
        processor.withNamespaceOverrideMap(namespaceOverrideMap);
        property = new ClassProperties(processor).process(new TypeDeclaration.Property(), tableField, false);

        // TODO Assert...

        namespaceOverrideMap.put(SampleClassTable.class.getPackage().getName(), "other.test.package");
        processor.withNamespaceOverrideMap(namespaceOverrideMap);
        property = new ClassProperties(processor).process(new TypeDeclaration.Property(), tableField, true);

        // TODO Assert...

        ArtifactReference mockReference = Mockito.mock(ArtifactReference.class);
        Mockito.when(mockReference.getMavenProject()).thenThrow(Exception.class);

        processor.withArtifactReference(mockReference);
        property = new ClassProperties(processor).process(new TypeDeclaration.Property(), tableField, false);

        // TODO Assert...

        property = new ClassProperties(processor).process(new TypeDeclaration.Property(), tableField, true);

        // TODO Assert...
    }

    @Test
    public void testProcess() throws Exception {
        MavenProject mockMavenProject = Mockito.mock(MavenProject.class);
        ArtifactReference reference = new ArtifactReference(mockMavenProject, null, null, null, null);

        Field tableField = SampleClassTable.class.getDeclaredField("fullnameClass");

        Schema schema = new Schema();
        schema.setClasspathReferenceList(new ArrayList<>());

        Processor processor = new Processor().withArtifactReference(reference).withConsolidatedSchemas(false).withSchema(schema);

        Field currentSchemaField = processor.getClass().getDeclaredField("currentSchema");
        currentSchemaField.setAccessible(true);
        currentSchemaField.set(processor, schema);

        TypeDeclaration.Property property = new ClassProperties(processor).process(new TypeDeclaration.Property(), tableField, false);

        Processor spyProcessor = PowerMockito.spy(processor);

        final Namespace namespace = new Namespace("sexy.new.namespace", null, "sexy-artifact");
        Processor.ExternalClassDefinition mockExternalClassDefinition = PowerMockito.mock(Processor.ExternalClassDefinition.class);
        mockExternalClassDefinition.locatedOutside = true;
        mockExternalClassDefinition.targetNamespace = namespace;

        Assert.assertEquals("SampleClassReferenced", property.options.get(TypeDeclaration.Property.PropertyOptionKey.IDENT));

        PowerMockito.doReturn(mockExternalClassDefinition).when(spyProcessor, PowerMockito.method(Processor.class, "getExternalClassDefinitionDetails", Class.class)).withArguments(Mockito.any());
        property = new ClassProperties(spyProcessor).process(new TypeDeclaration.Property(), tableField, false);

        Assert.assertEquals(namespace.toString() + ".SampleClassReferenced", property.options.get(TypeDeclaration.Property.PropertyOptionKey.IDENT));

        Processor mockProcessor = PowerMockito.mock(Processor.class);

        // Test exception block
        PowerMockito.when(mockProcessor, PowerMockito.method(Processor.class, "getExternalClassDefinitionDetails", Class.class)).withArguments(Mockito.any()).thenAnswer(invocation -> {
            throw new ClassNotFoundException();
        });
        property = new ClassProperties(mockProcessor).process(new TypeDeclaration.Property(), tableField, false);

        // Test inner class handling within exception block
        tableField = SampleClassTable.class.getDeclaredField("innerClassField");
        property = new ClassProperties(mockProcessor).process(new TypeDeclaration.Property(), tableField, false);
    }
}