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
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.lang.reflect.Field;
import java.util.ArrayList;

@RunWith(PowerMockRunner.class)
public class ListPropertiesTest {

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

        TypeDeclaration.Property property = new ListProperties(processor).process(new TypeDeclaration.Property(), arrayField, false);

        ArtifactReference mockReference = Mockito.mock(ArtifactReference.class);
        Mockito.when(mockReference.getMavenProject()).thenThrow(Exception.class);
        processor.withArtifactReference(mockReference);
        property = new ListProperties(processor).process(new TypeDeclaration.Property(), arrayField, false);
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
        TypeDeclaration.Property property = new ListProperties(spyProcessor).process(new TypeDeclaration.Property(), arrayField, false);

        Assert.assertEquals(namespace.toString() + ".String", property.options.get(TypeDeclaration.Property.PropertyOptionKey.ARRAY));
    }
}