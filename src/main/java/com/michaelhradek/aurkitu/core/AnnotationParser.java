/**
 *
 */
package com.michaelhradek.aurkitu.core;

import java.io.File;
import java.io.FileInputStream;
import java.lang.annotation.Annotation;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
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
     * @param mavenProject The MavenProject
     * @param input A list of Aurkitu annotations.
     * @return A list of classes which are annotated with the above annotations.
     * @throws MojoExecutionException
     */
    public static Set<Class<?>> findAnnotatedClasses(MavenProject mavenProject, Class<? extends Annotation> input) throws MojoExecutionException {
        List<String> classpathElements = null;

        try {
            classpathElements = mavenProject.getCompileClasspathElements();
            List<URL> projectClasspathList = new ArrayList<URL>();
            for (String element : classpathElements) {
                try {
                    projectClasspathList.add(new File(element).toURI().toURL());
                } catch (MalformedURLException e) {
                    throw new MojoExecutionException(element + " is an invalid classpath element", e);
                }
            }

            URLClassLoader loader = new URLClassLoader(projectClasspathList.toArray(new URL[0]));
            return findAnnotatedClasses(new Reflections(loader), input);

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
