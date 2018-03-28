/**
 *
 */
package com.michaelhradek.aurkitu.core;

import java.lang.annotation.Annotation;
import java.net.MalformedURLException;
import java.util.*;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import com.michaelhradek.aurkitu.Application;

/**
 * @author m.hradek
 */
public class AnnotationParser {

    /**
     *
     * @param artifactReference The ArtifactReference
     * @param input A list of Aurkitu annotations.
     * @return A list of classes which are annotated with the above annotations.
     * @throws MojoExecutionException when there is a MalformedURLException in the classpathElements
     */
    public static Set<Class<?>> findAnnotatedClasses(ArtifactReference artifactReference, Class<? extends Annotation> input) throws MojoExecutionException {
        try {
            return findAnnotatedClasses(Utilities.buildReflections(artifactReference), input);
        } catch (DependencyResolutionRequiredException e) {
            throw new MojoExecutionException("Dependency resolution failed", e);
        } catch (ArtifactResolutionException e) {
            throw new MojoExecutionException("Artifact resolution failed", e);
        } catch (MalformedURLException e) {
            throw new MojoExecutionException("Malformed URL", e);
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
    private static Set<Class<?>> findAnnotatedClasses(Reflections reflections, Class<? extends Annotation> input) {

        Set<Class<?>> classes = reflections.getTypesAnnotatedWith(input);
        for (Class<?> clazz : classes) {
            String prefix = "Find: " + input.getName();
            Application.getLogger().debug(prefix + " -> Found annotated class: " + clazz.getName());
        }

        return classes;
    }
}
