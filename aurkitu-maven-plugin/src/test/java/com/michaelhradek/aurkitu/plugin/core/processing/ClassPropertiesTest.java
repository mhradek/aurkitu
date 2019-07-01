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
    public void testProcessClassOld() throws NoSuchFieldException, IllegalAccessException {
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
    }
}