package com.michaelhradek.aurkitu.plugin.core.parser;

import com.michaelhradek.aurkitu.annotations.FlatBufferTable;
import com.michaelhradek.aurkitu.plugin.core.parsing.AnnotationParser;
import com.michaelhradek.aurkitu.plugin.core.parsing.ArtifactReference;
import com.michaelhradek.aurkitu.plugin.test.*;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@RunWith(MockitoJUnitRunner.class)
public class AnnotationParserTest {

    @Mock
    private MavenProject mockProject;

    @Mock
    private RepositorySystem mockRepositorySystem;

    @Mock
    private RepositorySystemSession mockRepositorySystemSession;

    @Mock
    private List<RemoteRepository> mockRemoteRepositories;

    @Mock
    private List<String> mockSpecifiedDependencies;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
    }

    /**
     * Test method for
     * {@link AnnotationParser#findAnnotatedClasses(java.lang.Class)}.
     */
    @Test
    public void testFindAnnotatedClasses() {
        Set<Class<?>> annotated = AnnotationParser.findAnnotatedClasses(FlatBufferTable.class);
        Assert.assertEquals(false, annotated.isEmpty());
        Assert.assertEquals(6, annotated.size());

        Assert.assertEquals(true, annotated.contains(SampleClassReferenced.class));
        Assert.assertEquals(true, annotated.contains(SampleClassTable.class));
        Assert.assertEquals(false, annotated.contains(SampleEnumByte.class));
        Assert.assertEquals(false, annotated.contains(SampleEnumNull.class));
    }

    @Test
    public void testFindAnnotatedClassesWithPath() {
        Set<Class<?>> annotated = AnnotationParser.findAnnotatedClasses(".*", BogusAnnotation.class);
        Assert.assertEquals(true, annotated.isEmpty());
    }

    @Test
    public void testFindAnnotatedClassesExtended() throws Exception {
        Mockito.when(mockProject.getCompileClasspathElements()).thenReturn(new ArrayList<String>());

        ArtifactReference artifactReference = new ArtifactReference(mockProject, mockRepositorySystem, mockRepositorySystemSession, mockRemoteRepositories, mockSpecifiedDependencies);
        Set<Class<?>> annotated = AnnotationParser.findAnnotatedClasses(artifactReference, new ArrayList<>(),
                FlatBufferTable.class);
        Assert.assertEquals(true, annotated.isEmpty());

        Mockito.when(mockProject.getCompileClasspathElements()).thenReturn(null);
        try {
            annotated = AnnotationParser.findAnnotatedClasses(artifactReference, new ArrayList<>(), FlatBufferTable.class);
            Assert.fail("Expected MojoExecutionException; Compile Classpath Elements returned null");
        } catch (MojoExecutionException e) {
            Assert.assertEquals("No valid compile classpath elements exist; is there source code for this project?", e.getMessage());
        }
    }
}
