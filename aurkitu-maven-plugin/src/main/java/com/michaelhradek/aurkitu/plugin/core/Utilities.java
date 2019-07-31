package com.michaelhradek.aurkitu.plugin.core;

import com.michaelhradek.aurkitu.plugin.Config;
import com.michaelhradek.aurkitu.plugin.core.output.Schema;
import com.michaelhradek.aurkitu.plugin.core.parsing.ArtifactReference;
import com.michaelhradek.aurkitu.plugin.core.parsing.ClasspathReference;
import com.michaelhradek.aurkitu.plugin.core.parsing.ClasspathSearchType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.reflections.Reflections;
import org.reflections.adapters.JavassistAdapter;
import org.reflections.scanners.FieldAnnotationsScanner;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author m.hradek
 */
@Slf4j
public class Utilities {

    private static Set<Artifact> dependencyArtifactsCache;
    private static List<String> classpathElementsCache;
    private static String workingProject;

    // Never need to instantiate this
    private Utilities() {
        // Empty
    }

    /**
     * @param type The class which needs to be tested if it is a primitive. Also, double and Double are both
     *             considered primitive within this context.
     * @return boolean if the type is a primitive or corresponding wrapper
     */
    public static boolean isPrimitiveOrWrapperType(Class<?> type) {
        return (type.isPrimitive() && type != void.class) || type == Double.class || type == Float.class
                || type == Long.class || type == Integer.class || type == Short.class
                || type == Character.class || type == Byte.class || type == Boolean.class
                || type == String.class;
    }

    /**
     * @param type The class which needs to be tested if it is a primitive. Also, double and Double are both
     *             considered primitive within this context.
     * @return the lower case primitive version of a wrapper type
     */
    public static String getPrimitiveNameForWrapperType(Class<?> type) {
        if (!isPrimitiveOrWrapperType(type)) {
            throw new IllegalArgumentException("Expecting a primitive or wrapper type");
        }

        if (type == Character.class || type == char.class) {
            return "string";
        }

        if (type == Boolean.class || type == boolean.class) {
            return "bool";
        }

        if (type == Integer.class) {
            return "int";
        }

        // We may want to use the above if statement format and avoid chance
        return type.getSimpleName().toLowerCase();
    }

    /**
     * @param artifactReference      Our helper which contains all the goodies needed from the MavenProject and the other
     *                               artifact handling stuff
     * @param classpathReferenceList List of classpath references to consider when building the class list
     * @return initialized Reflections object
     * @throws DependencyResolutionRequiredException if unable to MavenProject#getCompileClasspathElements()
     * @throws MojoExecutionException                if getting NULL from MavenProject#getCompileClasspathElements()
     */
    public static synchronized Reflections buildReflections(ArtifactReference artifactReference, List<ClasspathReference> classpathReferenceList)
            throws DependencyResolutionRequiredException, MojoExecutionException {

        List<String> classpathElements;

        // Load build class path
        classpathElements = artifactReference.getMavenProject().getCompileClasspathElements();
        if (classpathElements == null) {
            throw new MojoExecutionException("No valid compile classpath elements exist; is there source code for " +
                    "this project?");
        }

        // Load all paths into custom classloader
        ClassLoader urlClassLoader = URLClassLoader.newInstance(arrayForClasspathReferenceList(classpathReferenceList),
                Thread.currentThread().getContextClassLoader());

        // Retain annotations
        JavassistAdapter javassistAdapter = new JavassistAdapter();
        JavassistAdapter.includeInvisibleTag = false;

        FilterBuilder filterBuilder = null;
        if (artifactReference.getSpecifiedDependencies() != null && artifactReference.getSpecifiedDependencies().size() > 0) {
            log.debug("Adding specified dependencies to filter for `org.reflections` package scanning...");
            filterBuilder = new FilterBuilder();
            for (String dependency : artifactReference.getSpecifiedDependencies()) {
                filterBuilder.include(FilterBuilder.prefix(extractDependencyDetails(dependency).specifiedGroupId));
            }
        }

        return new Reflections(
                new ConfigurationBuilder()
                        .filterInputsBy(filterBuilder)
                        .setUrls(ClasspathHelper.forClassLoader(urlClassLoader))
                        .addClassLoader(urlClassLoader)
                        .setScanners(
                                new SubTypesScanner(false),
                                new TypeAnnotationsScanner(),
                                new FieldAnnotationsScanner(),
                                new MethodAnnotationsScanner()
                        )
                        .setMetadataAdapter(javassistAdapter)
                        .useParallelExecutor()
        );
    }

    /**
     * @param artifactReference   Our helper which contains all the goodies needed from the MavenProject and the other
     *                            artifact handling stuff
     * @param classpathSearchType Which type of classpath search should we use. BOTH, PROJECT, DEPENDENCIES.
     * @return an array of URLs which will be used to attempt to initialize our classes
     * @throws ArtifactResolutionException           if unable to Utilities#buildProjectClasspathList#resolveArtifact
     *                                               via Repo System
     * @throws MalformedURLException                 if unable to convert paths for classes to URL format
     * @throws DependencyResolutionRequiredException if unable to MavenProject#getCompileClasspathElements()
     */
    public static synchronized List<ClasspathReference> buildProjectClasspathList(ArtifactReference artifactReference,
                                                                                  ClasspathSearchType classpathSearchType) throws ArtifactResolutionException, MalformedURLException, DependencyResolutionRequiredException {

        List<ClasspathReference> classpathReferenceList = new ArrayList<>();
        final MavenProject mavenProject = artifactReference.getMavenProject();

        if (classpathSearchType == ClasspathSearchType.BOTH || classpathSearchType == ClasspathSearchType.PROJECT) {
            // Load build class path
            if (classpathElementsCache == null || workingProject == null || !workingProject.equalsIgnoreCase(getCurrentProject(artifactReference))) {
                log.debug("Compile Classpath Elements Cache was null; fetching update");
                classpathElementsCache = mavenProject.getCompileClasspathElements();
            }

            for (String element : classpathElementsCache) {
                log.debug("Looking at compile classpath element (via MavenProject): " + element);
                log.debug("  Adding: " + element);
                final ClasspathReference classpathReference = new ClasspathReference(
                        new File(element).toURI().toURL(),
                        mavenProject.getGroupId(),
                        mavenProject.getArtifactId()
                );
                classpathReferenceList.add(classpathReference);
            }
        }

        if (classpathSearchType == ClasspathSearchType.BOTH || classpathSearchType == ClasspathSearchType.DEPENDENCIES) {
            // Load artifact(s) jars using resolver
            if (dependencyArtifactsCache == null || workingProject == null || !workingProject.equalsIgnoreCase(getCurrentProject(artifactReference))) {
                log.debug("Dependency Artifacts Cache was null; fetching update");
                dependencyArtifactsCache = artifactReference.getMavenProject().getDependencyArtifacts();
            }

            log.debug("Number of artifacts to resolve: "
                    + dependencyArtifactsCache.size());

            for (Artifact unresolvedArtifact : dependencyArtifactsCache) {
                String artifactId = unresolvedArtifact.getArtifactId();

                if (!isArtifactResolutionRequired(unresolvedArtifact, artifactReference)) {
                    log.debug("  Skipping: " + unresolvedArtifact.toString());
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

                // This takes time; minimizing what needs to be resolved is the goal of the specified base code
                // block
                ArtifactResult resolutionResult = artifactReference.getRepoSystem()
                        .resolveArtifact(artifactReference.getRepoSession(), artifactRequest);

                // The file should exist, but we never know.
                File file = resolutionResult.getArtifact().getFile();
                if (file == null || !file.exists()) {
                    log.warn("Artifact " + artifactId +
                            " has no attached file. Its content will not be copied in the target model directory.");
                    continue;
                }

                String jarPath = "jar:file:" + file.getAbsolutePath() + "!/";
                log.debug("Adding resolved artifact: " + file.getAbsolutePath());
                final ClasspathReference classpathReference = new ClasspathReference(
                        new URL(jarPath),
                        resolutionResult.getArtifact().getGroupId(),
                        resolutionResult.getArtifact().getArtifactId()
                );
                classpathReferenceList.add(classpathReference);
            }
        }

        workingProject = getCurrentProject(artifactReference);
        return classpathReferenceList;
    }

    /**
     * @param classLoaderToSwitchTo                which will be used temporarily during the operation
     * @param actionToPerformOnProvidedClassLoader a ExecutableAction
     * @param <T>                                  resulting type
     * @return the result
     */
    public static synchronized <T> T executeActionOnSpecifiedClassLoader(final ClassLoader classLoaderToSwitchTo,
                                                                         final ExecutableAction<T> actionToPerformOnProvidedClassLoader) {

        final ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();

        try {
            Thread.currentThread().setContextClassLoader(classLoaderToSwitchTo);
            for (URL url : ((URLClassLoader) (Thread.currentThread().getContextClassLoader())).getURLs()) {
                log.debug("Classloader loaded with: " + url.toString());
            }

            return actionToPerformOnProvidedClassLoader.run();
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    /**
     * @param schema          The target schema (used to determine filename)
     * @param outputDirectory Where we have configured the schema to be written to
     * @return boolean whether or not the schema file exists in the location specified
     */
    public static synchronized boolean isSchemaPresent(final Schema schema, final File outputDirectory) {
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
     * Currently this is odd because some schemas end up being invalid so likely always false
     *
     * @param schemas         The target schema (used to determine filename)
     * @param outputDirectory Where we have configured the schema to be written to
     * @return boolean whether or not the schema file exists in the location specified
     */
    public static boolean areSchemasPresent(final List<Schema> schemas, final File outputDirectory) {
        for (Schema schema : schemas) {
            if (!isSchemaPresent(schema, outputDirectory)) {
                return false;
            }
        }

        return true;
    }

    /**
     * @param unresolvedArtifact an artifact
     * @param artifactReference  the Maven repo bundle
     * @return whether or not the artifact should be resolved
     */
    public static boolean isArtifactResolutionRequired(final Artifact unresolvedArtifact,
                                                       final ArtifactReference artifactReference) {

        // if a base search is specified, use it
        if (artifactReference.getSpecifiedDependencies() != null && !artifactReference.getSpecifiedDependencies().isEmpty()) {
            log.debug("Targeted base search requested. Will skip artifacts not specified in" +
                    " configuration.");

            int matchesFound = 0;

            // Loop through all the specified dependencies
            for (String dependency : artifactReference.getSpecifiedDependencies()) {

                // Using groupId or is artifactId included? Using the Maven notation
                log.debug("  Testing against: " + dependency);
                DependencyDetails dependencyDetails = extractDependencyDetails(dependency);

                log.debug("  Unresolved groupId: {}, artifactId: {}",
                        unresolvedArtifact.getGroupId(), unresolvedArtifact.getArtifactId());

                // If only a groupId is specified...
                if (dependencyDetails.specifiedArtifactId == null && dependencyDetails.specifiedGroupId.equalsIgnoreCase(unresolvedArtifact.getGroupId())) {
                    matchesFound++;
                }

                // If both a group and artifactId are specified...
                if (dependencyDetails.specifiedArtifactId != null &&
                        dependencyDetails.specifiedArtifactId.equalsIgnoreCase(unresolvedArtifact.getArtifactId()) &&
                        dependencyDetails.specifiedGroupId.equalsIgnoreCase(unresolvedArtifact.getGroupId())) {
                    matchesFound++;
                }
            }

            // If the unresolvedArtifact doesn't match any of the specified dependencies then we will skip it from
            // resolution
            return matchesFound >= 1;
        }

        // If we don't specify dependencies OR if it matched one of the specified group or group:artifact combos
        // resolve it
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
        log.debug("  Current project name: {}", projectName);
        return projectName;
    }

    /**
     * @param classpathReferenceList a List&lt;ClasspathReference&gt;
     * @return an array of URLs
     */
    public static URL[] arrayForClasspathReferenceList(List<ClasspathReference> classpathReferenceList) {
        if (classpathReferenceList == null) {
            return null;
        }

        URL[] urls = new URL[classpathReferenceList.size()];
        for (int i = 0; i < classpathReferenceList.size(); i++) {
            urls[i] = classpathReferenceList.get(i).getUrl();
        }

        return urls;
    }

    /**
     * @param dependencyStringFromPom The groupId:artifactId dependency string from the configuration.
     * @return A helper object with the required details
     */
    private static DependencyDetails extractDependencyDetails(String dependencyStringFromPom) {
        String specifiedGroupId;
        String specifiedArtifactId;
        if (dependencyStringFromPom.contains(":")) {
            String[] temp = dependencyStringFromPom.split(":");
            specifiedGroupId = temp[0];
            specifiedArtifactId = temp[1];
        } else {
            specifiedGroupId = dependencyStringFromPom;
            specifiedArtifactId = null;
        }

        log.debug("Dependency specified groupId: {}, artifactId: {}", specifiedGroupId, specifiedArtifactId);

        return new DependencyDetails(specifiedGroupId, specifiedArtifactId);
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

    @Getter
    @AllArgsConstructor
    private static class DependencyDetails {
        private String specifiedGroupId;
        private String specifiedArtifactId;
    }
}
