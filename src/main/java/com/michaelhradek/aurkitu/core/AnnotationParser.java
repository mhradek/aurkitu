/**
 * 
 */
package com.michaelhradek.aurkitu.core;

import java.lang.annotation.Annotation;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.reflections.Reflections;

import com.michaelhradek.aurkitu.Config;

/**
 * @author m.hradek
 * @date May 22, 2017
 * 
 */
public class AnnotationParser {

  private static final Logger log = Config.getLogger(AnnotationParser.class);

  /**
   * 
   * @param input
   * @return
   */
  public static Set<Class<?>> findAnnotatedClasses(String path, Class<? extends Annotation> input) {
    Reflections reflections = new Reflections(path);

    Set<Class<?>> classes = reflections.getTypesAnnotatedWith(input);
    for (Class<?> clazz : classes) {
      String prefix = "Find: " + input.getName();
      log.log(Level.FINE, prefix + " -> Found annotated class: " + clazz.getName());
    }

    return classes;
  }
}
