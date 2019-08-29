package com.michaelhradek.aurkitu.reflections.serializers;

import com.michaelhradek.aurkitu.reflections.Reflections;

import java.io.File;
import java.io.InputStream;

/**
 * Serilizer of a {@link Reflections} instance
 */
public interface Serializer {
    /**
     * reads the input stream into a new Reflections instance, populating it's store
     */
    Reflections read(InputStream inputStream);

    /**
     * saves a Reflections instance into the given filename
     */
    File save(Reflections reflections, String filename);

    /**
     * returns a string serialization of the given Reflections instance
     */
    String toString(Reflections reflections);
}
