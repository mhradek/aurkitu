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

@RunWith(PowerMockRunner.class)
public class MapPropertiesTest {

    @Test
    public void testProcessMap() throws Exception {
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

        TypeDeclaration.Property property = new MapProperties(processor).process(new TypeDeclaration.Property(), mapField, false);

        // TODO Assert...

        ArtifactReference mockReference = Mockito.mock(ArtifactReference.class);
        Mockito.when(mockReference.getMavenProject()).thenThrow(Exception.class);
        processor.withArtifactReference(mockReference);
        property = new MapProperties(processor).process(new TypeDeclaration.Property(), mapField, false);

        // TODO Assert...

        Processor spyProcessor = PowerMockito.spy(processor);

        final Namespace namespace = new Namespace("sexy.new.namespace", null, "sexy-artifact");
        Processor.ExternalClassDefinition mockExternalClassDefinition = PowerMockito.mock(Processor.ExternalClassDefinition.class);
        mockExternalClassDefinition.locatedOutside = true;
        mockExternalClassDefinition.targetNamespace = namespace;

        Assert.assertEquals("MapValueSet_SampleClassTable_dataMap", property.options.get(TypeDeclaration.Property.PropertyOptionKey.MAP));
        PowerMockito.doReturn(mockExternalClassDefinition).when(spyProcessor, PowerMockito.method(Processor.class, "getExternalClassDefinitionDetails", Class.class)).withArguments(Mockito.any());

        property = new MapProperties(spyProcessor).process(new TypeDeclaration.Property(), mapField, false);
        Assert.assertEquals("MapValueSet_SampleClassTable_dataMap", property.options.get(TypeDeclaration.Property.PropertyOptionKey.MAP));

        //Processor mockProcessor = PowerMockito.mock(Processor.class);

        //PowerMockito.when(mockProcessor, PowerMockito.method(Processor.class, "getExternalClassDefinitionDetails", Class.class)).withArguments(Mockito.any()).thenAnswer(invocation -> { throw new ClassNotFoundException(); });
        //property = new MapProperties(mockProcessor).process(new TypeDeclaration.Property(), mapField, false);

    }
}