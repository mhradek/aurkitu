/**
 *
 */
package com.michaelhradek.aurkitu.core;

import java.io.File;
import java.lang.annotation.Annotation;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.reflections.Reflections;
import org.reflections.adapters.JavassistAdapter;
import org.reflections.scanners.FieldAnnotationsScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.scanners.TypeElementsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import com.michaelhradek.aurkitu.Application;

/**
 * @author m.hradek
 */
public class AnnotationParser {

    /**
     *
     * @param mavenProject The MavenProject
     * @param input A list of Aurkitu annotations.
     * @return A list of classes which are annotated with the above annotations.
     * @throws MojoExecutionException when there is a MalformedURLException in the classpathElements
     */
    public static Set<Class<?>> findAnnotatedClasses(MavenProject mavenProject, Class<? extends Annotation> input) throws MojoExecutionException {
        List<String> classpathElements = null;

        try {
            classpathElements = mavenProject.getCompileClasspathElements();
            if(classpathElements == null) {
                throw new MojoExecutionException("No valid compile classpath elements exist; is there source code for this project?");
            }

            List<URL> projectClasspathList = new ArrayList<URL>();
            for (String element : classpathElements) {
                Application.getLogger().debug("Considering compile classpath element (via MavenProject): " + element);
                try {
                    projectClasspathList.add(new File(element).toURI().toURL());
                } catch (MalformedURLException e) {
                    throw new MojoExecutionException(element + " is an invalid classpath element", e);
                }
            }

            // Retain annotations
            JavassistAdapter javassistAdapter = new JavassistAdapter();
            javassistAdapter.includeInvisibleTag = false;

            URLClassLoader urlClassLoader = new URLClassLoader(projectClasspathList.toArray(new URL[]{}),
                    Thread.currentThread().getContextClassLoader());

            Reflections reflections = new Reflections(
                    new ConfigurationBuilder().setUrls(
                            ClasspathHelper.forClassLoader(urlClassLoader)
                    ).addClassLoader(urlClassLoader).setScanners(new TypeAnnotationsScanner(), new TypeElementsScanner(),
                            new FieldAnnotationsScanner(), new TypeAnnotationsScanner(), new SubTypesScanner(false)
                    ).setMetadataAdapter(javassistAdapter)
            );

            return findAnnotatedClasses(reflections, input);

        } catch (DependencyResolutionRequiredException e) {
            throw new MojoExecutionException("Dependency resolution failed", e);
        }
    }

    /**
     * @param input A list of Aurkitu annotations.
     * @return A list of classes which are annotated with the above annotations.
     */
    public static Set<Class<?>> findAnnotatedClasses(Class<? extends Annotation> input) {
        Reflections reflections =
                new Reflections(new ConfigurationBuilder().setUrls(ClasspathHelper.forJavaClassPath()));

        return findAnnotatedClasses(reflections, input);
    }

    /**
     * @param path The path to traverse.
     * @param input A list of Aurkitu annotations.
     * @return A list of classes which are annotated with the above annotations.
     */
    public static Set<Class<?>> findAnnotatedClasses(String path, Class<? extends Annotation> input) {
        return findAnnotatedClasses(new Reflections(path), input);
    }

    /**
     * @param reflections Reflections to traverse.
     * @param input A list of Aurkitu annotations.
     * @return A list of classes which are annotated with the above annotations.
     */
    private static Set<Class<?>> findAnnotatedClasses(Reflections reflections,
                                                      Class<? extends Annotation> input) {

        Set<Class<?>> classes = reflections.getTypesAnnotatedWith(input);
        for (Class<?> clazz : classes) {
            String prefix = "Find: " + input.getName();
            Application.getLogger().debug(prefix + " -> Found annotated class: " + clazz.getName());
        }

        return classes;
    }
}
