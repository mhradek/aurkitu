package com.michaelhradek.aurkitu.core;

import com.michaelhradek.aurkitu.Application;
import com.michaelhradek.aurkitu.Config;
import com.michaelhradek.aurkitu.core.output.Schema;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.reflections.Reflections;
import org.reflections.adapters.JavassistAdapter;
import org.reflections.scanners.*;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author m.hradek
 *
 */
public class Utilities {

    private static Set<Artifact> dependencyArtifactsCache;
    private static List<String> classpathElementsCache;
    private static String workingProject;

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
        if (classpathElementsCache == null || workingProject == null || !workingProject.equalsIgnoreCase(getCurrentProject(artifactReference))) {
            Application.getLogger().debug("Compile Classpath Elements Cache was null; fetching update");
            classpathElementsCache = artifactReference.getMavenProject().getCompileClasspathElements();
        }

        for (String element : classpathElementsCache) {
            Application.getLogger().debug("Looking at compile classpath element (via MavenProject): " + element);
            Application.getLogger().debug("  Adding: " + element);
            projectClasspathList.add(new File(element).toURI().toURL());
        }

        // Load artifact(s) jars using resolver
        if (dependencyArtifactsCache == null || workingProject == null || !workingProject.equalsIgnoreCase(getCurrentProject(artifactReference))) {
            Application.getLogger().debug("Dependency Artifacts Cache was null; fetching update");
            dependencyArtifactsCache =  artifactReference.getMavenProject().getDependencyArtifacts();
        }

        Application.getLogger().debug("Number of artifacts to resolve: "
                + dependencyArtifactsCache.size());

        for (Artifact unresolvedArtifact : dependencyArtifactsCache) {
            String artifactId = unresolvedArtifact.getArtifactId();

            if (!isArtifactResolutionRequired(unresolvedArtifact, artifactReference)) {
                Application.getLogger().debug("  Skipping: " + unresolvedArtifact.toString());
                continue;
            }

            org.eclipse.aether.artifact.Artifact aetherArtifact = new DefaultArtifact(
                    unresolvedArtifact.getGroupId(),
                    unresolvedArtifact.getArtifactId(),
                    unresolvedArtifact.getClassifier(),
                    unresolvedArtifact.getType(),
                    unresolvedArtifact.getVersion());

            ArtifactRequest artifactRequest = new ArtifactRequest()
                    .setRepositories(artifactReference.getRepositories())
                    .setArtifact(aetherArtifact);

            // This takes time; minimizing what needs to be resolved is the goal of the specified dependency code block
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

        workingProject = getCurrentProject(artifactReference);
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

    /**
     * @param schema          The target schema (used to determine filename)
     * @param outputDirectory Where we have configured the schema to be written to
     * @return boolean whether or not the schema file exists in the location specified
     */
    public static boolean isSchemaPresent(final Schema schema, final File outputDirectory) {
        if (!outputDirectory.exists()) {
            return false;
        }

        String fileName = "." + Config.FILE_EXTENSION;
        if (schema.getName() == null || schema.getName().length() < 1) {
            // The file generation stuff will create a time based file
            return false;
        } else {
            fileName = schema.getName() + fileName;
        }

        File targetFile = new File(outputDirectory, fileName);

        return targetFile.exists();
    }

    /**
     * @param unresolvedArtifact an artifact
     * @param artifactReference  the Maven repo bundle
     * @return whether or not the artifact should be resolved
     */
    public static boolean isArtifactResolutionRequired(final Artifact unresolvedArtifact, final ArtifactReference artifactReference) {

        // if a dependency search is specified, use it
        if (artifactReference.getSpecifiedDependencies() != null && !artifactReference.getSpecifiedDependencies().isEmpty()) {
            Application.getLogger().debug("Targeted dependency search requested. Will skip artifacts not specified in configuration.");

            int matchesFound = 0;

            // Loop through all the specified dependencies
            for (String dependency : artifactReference.getSpecifiedDependencies()) {

                // Using groupId or is artifactId included? Using the Maven notation
                Application.getLogger().debug("  Testing against: " + dependency);
                String specifiedGroupId;
                String specifiedArtifactId;
                if (dependency.contains(":")) {
                    String[] temp = dependency.split(":");
                    specifiedGroupId = temp[0];
                    specifiedArtifactId = temp[1];
                } else {
                    specifiedGroupId = dependency;
                    specifiedArtifactId = null;
                }

                Application.getLogger().debug(String.format("  Specified groupId: %s, artifactId: %s", specifiedGroupId, specifiedArtifactId));
                Application.getLogger().debug(String.format("  Unresolved groupId: %s, artifactId: %s", unresolvedArtifact.getGroupId(), unresolvedArtifact.getArtifactId()));

                // If only a groupId is specified...
                if (specifiedArtifactId == null && specifiedGroupId.equalsIgnoreCase(unresolvedArtifact.getGroupId())) {
                    matchesFound++;
                }

                // If both a group and artifactId are specified...
                if (specifiedArtifactId != null &&
                        specifiedArtifactId.equalsIgnoreCase(unresolvedArtifact.getArtifactId()) &&
                        specifiedGroupId.equalsIgnoreCase(unresolvedArtifact.getGroupId())) {
                    matchesFound++;
                }
            }

            // If the unresolvedArtifact doesn't match any of the specified dependencies then we will skip it from resolution
            return matchesFound >= 1;
        }

        // If we don't specify dependencies OR if it matched one of the specified group or group:artifact combos resolve it
        return true;
    }

    /**
     * The current project name in artifactId:groupId format
     *
     * @param artifactReference the Maven repo bundle
     * @return the project name
     */
    public static String getCurrentProject(final ArtifactReference artifactReference) {
        final String projectName = String.join(":",
                artifactReference.getMavenProject().getGroupId(), artifactReference.getMavenProject().getArtifactId());
        Application.getLogger().debug("  Current project name: " + projectName);
        return projectName;
    }
}
