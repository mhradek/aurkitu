package com.michaelhradek.aurkitu.plugin.core.parsing;

import com.michaelhradek.aurkitu.plugin.core.Utilities;
import com.michaelhradek.aurkitu.reflections.Reflections;
import com.michaelhradek.aurkitu.reflections.util.ClasspathHelper;
import com.michaelhradek.aurkitu.reflections.util.ConfigurationBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;

/**
 * @author m.hradek
 */
@Slf4j
public class AnnotationParser {

    /**
     * @param artifactReference      The ArtifactReference
     * @param classpathReferenceList A list of paths to consider when searching for annotations
     * @param input                  A list of Aurkitu annotations to search for
     * @return A list of classes which are annotated with the above annotations.
     * @throws MojoExecutionException when there is a MalformedURLException in the classpathElements
     */
    public static Set<Class<?>> findAnnotatedClasses(ArtifactReference artifactReference,
                                                     List<ClasspathReference> classpathReferenceList, Class<?
            extends Annotation> input) throws MojoExecutionException {
        try {
            return findAnnotatedClasses(Utilities.buildReflections(artifactReference, classpathReferenceList), input);
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
     * @param path  The path to traverse.
     * @param input A list of Aurkitu annotations.
     * @return A list of classes which are annotated with the above annotations.
     */
    public static Set<Class<?>> findAnnotatedClasses(String path, Class<? extends Annotation> input) {
        return findAnnotatedClasses(new Reflections(path), input);
    }

    /**
     * @param reflections Reflections to traverse.
     * @param input       A list of Aurkitu annotations.
     * @return A list of classes which are annotated with the above annotations.
     */
    private static Set<Class<?>> findAnnotatedClasses(Reflections reflections, Class<? extends Annotation> input) {

        Set<Class<?>> classes = reflections.getTypesAnnotatedWith(input);
        for (Class<?> clazz : classes) {
            String prefix = "Find: " + input.getName();
            log.debug(prefix + " -> Found annotated class: " + clazz.getName());
        }

        return classes;
    }
}
