package com.michaelhradek.aurkitu.core;

import com.michaelhradek.aurkitu.Application;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.reflections.Reflections;
import org.reflections.adapters.JavassistAdapter;
import org.reflections.scanners.FieldAnnotationsScanner;
import org.reflections.scanners.MemberUsageScanner;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.MethodParameterNamesScanner;
import org.reflections.scanners.MethodParameterScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

/**
 * @author m.hradek
 *
 */
class Utilities {

    private static Set<Artifact> dependencyArtifactsCache;
    private static List<String> classpathElementsCache;

    /**
     * @param type The class which needs to be tested if it is a primative. Also, double and Double are both considered primative within this context.
     * @return boolean
     */
    public static boolean isLowerCaseType(Class<?> type) {
        return (type.isPrimitive() && type != void.class) || type == Double.class || type == Float.class
                || type == Long.class || type == Integer.class || type == Short.class
                || type == Character.class || type == Byte.class || type == Boolean.class
                || type == String.class;
    }

    /**
     *
     * @param artifactReference Our helper which contains all the goodies needed from the MavenProject and the other artifact handling stuff
     * @return initialized Reflections object
     * @throws DependencyResolutionRequiredException if unable to MavenProject#getCompileClasspathElements()
     * @throws ArtifactResolutionException if unable to Utilities#buildProjectClasspathList#resolveArtifact via Repo System
     * @throws MalformedURLException if unable to convert paths for classes to URL format
     * @throws MojoExecutionException if getting NULL from MavenProject#getCompileClasspathElements()
     */
    public static Reflections buildReflections(ArtifactReference artifactReference) throws DependencyResolutionRequiredException,
            ArtifactResolutionException, MalformedURLException, MojoExecutionException {

        List<String> classpathElements;

            // Load build class path
            classpathElements = artifactReference.getMavenProject().getCompileClasspathElements();
            if(classpathElements == null) {
                throw new MojoExecutionException("No valid compile classpath elements exist; is there source code for this project?");
            }

            List<URL> projectClasspathList = new ArrayList<URL>();
            for (String element : classpathElements) {
                Application.getLogger().debug("Considering compile classpath element (via MavenProject): " + element);
                projectClasspathList.add(new File(element).toURI().toURL());
            }

            // Load all paths into custom classloader
            ClassLoader urlClassLoader = URLClassLoader.newInstance(buildProjectClasspathList(artifactReference),
                    Thread.currentThread().getContextClassLoader());

            // Retain annotations
            JavassistAdapter javassistAdapter = new JavassistAdapter();
            javassistAdapter.includeInvisibleTag = false;

            return new Reflections(
                    new ConfigurationBuilder().setUrls(
                            ClasspathHelper.forClassLoader(urlClassLoader)
                    ).addClassLoader(urlClassLoader).setScanners(
                            new SubTypesScanner(false),
                            new TypeAnnotationsScanner(),
                            new FieldAnnotationsScanner(),
                            new MethodAnnotationsScanner(),
                            new MethodParameterScanner(),
                            new MethodParameterNamesScanner(),
                            new MemberUsageScanner()
                    ).setMetadataAdapter(javassistAdapter)
            );
    }

    /**
     *
     * @param artifactReference Our helper which contains all the goodies needed from the MavenProject and the other artifact handling stuff
     * @return an array of URLs which will be used to attempt to initialize our classes
     * @throws ArtifactResolutionException if unable to Utilities#buildProjectClasspathList#resolveArtifact via Repo System
     * @throws MalformedURLException if unable to convert paths for classes to URL format
     * @throws DependencyResolutionRequiredException if unable to MavenProject#getCompileClasspathElements()
     */
    public static URL[] buildProjectClasspathList(ArtifactReference artifactReference)  throws ArtifactResolutionException, MalformedURLException, DependencyResolutionRequiredException {

        List<URL> projectClasspathList = new ArrayList<URL>();

        // Load build class path
        if(classpathElementsCache == null) {
            Application.getLogger().debug("Compile Classpath Elements Cache was null; fetching update");
            classpathElementsCache = artifactReference.getMavenProject().getCompileClasspathElements();
        }

        for (String element : classpathElementsCache) {
            Application.getLogger().debug("Looking at compile classpath element (via MavenProject): " + element);
            projectClasspathList.add(new File(element).toURI().toURL());
        }

        // Load artifact(s) jars using resolver
        if(dependencyArtifactsCache == null) {
            Application.getLogger().debug("Dependency Artifacts Cache was null; fetching update");
            dependencyArtifactsCache =  artifactReference.getMavenProject().getDependencyArtifacts();
        }

        Application.getLogger().debug("Number of artifacts to resolve: "
                + dependencyArtifactsCache.size());

        for (Artifact unresolvedArtifact : dependencyArtifactsCache) {
            String artifactId = unresolvedArtifact.getArtifactId();
            org.eclipse.aether.artifact.Artifact aetherArtifact = new DefaultArtifact(
                    unresolvedArtifact.getGroupId(),
                    unresolvedArtifact.getArtifactId(),
                    unresolvedArtifact.getClassifier(),
                    unresolvedArtifact.getType(),
                    unresolvedArtifact.getVersion());

            ArtifactRequest artifactRequest = new ArtifactRequest()
                    .setRepositories(artifactReference.getRepositories())
                    .setArtifact(aetherArtifact);
            ArtifactResult resolutionResult = artifactReference.getRepoSystem()
                    .resolveArtifact(artifactReference.getRepoSession(), artifactRequest);

            // The file should exist, but we never know.
            File file = resolutionResult.getArtifact().getFile();
            if (file == null || !file.exists()) {
                Application.getLogger().warn("Artifact " + artifactId +
                        " has no attached file. Its content will not be copied in the target model directory.");
                continue;
            }

            String jarPath = "jar:file:" + file.getAbsolutePath() + "!/";
            Application.getLogger().debug("Adding resolved artifact: " + file.getAbsolutePath());
            projectClasspathList.add(new URL(jarPath));
        }

        return projectClasspathList.toArray(new URL[]{});
    }

    /**
     * @param classLoaderToSwitchTo which will be used temporarily during the operation
     * @param actionToPerformOnProvidedClassLoader a ExecutableAction
     * @param <T> resulting type
     * @return the result
     */
    public static synchronized <T> T executeActionOnSpecifiedClassLoader(final ClassLoader classLoaderToSwitchTo,
        final ExecutableAction<T> actionToPerformOnProvidedClassLoader) {

        final ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();

        try {
            Thread.currentThread().setContextClassLoader(classLoaderToSwitchTo);
            for (URL url : ((URLClassLoader) (Thread.currentThread().getContextClassLoader())).getURLs()) {
                Application.getLogger().debug("Classloader loaded with: " + url.toString());
            }

            return actionToPerformOnProvidedClassLoader.run();
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    /**
     * Encapsulates action to be executed.
     */
    public interface ExecutableAction<T> {

        /**
         * Execute the operation.
         *
         * @return Optional value returned by this operation; implementations should document what, if anything, is
         * returned by implementations of this method.
         */
        T run();
    }
}
