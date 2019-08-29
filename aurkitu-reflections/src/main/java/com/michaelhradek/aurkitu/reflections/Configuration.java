package com.michaelhradek.aurkitu.reflections;

import com.google.common.base.Predicate;
import com.michaelhradek.aurkitu.reflections.adapters.MetadataAdapter;
import com.michaelhradek.aurkitu.reflections.scanners.Scanner;
import com.michaelhradek.aurkitu.reflections.serializers.Serializer;
import com.michaelhradek.aurkitu.reflections.util.ConfigurationBuilder;

import javax.annotation.Nullable;
import java.net.URL;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * Configuration is used to create a configured instance of {@link Reflections}
 * <p>it is preferred to use {@link ConfigurationBuilder}
 */
public interface Configuration {
    /**
     * the scanner instances used for scanning different metadata
     */
    Set<Scanner> getScanners();

    /**
     * the urls to be scanned
     */
    Set<URL> getUrls();

    /**
     * the metadata adapter used to fetch metadata from classes
     */
    @SuppressWarnings({"RawUseOfParameterizedType"})
    MetadataAdapter getMetadataAdapter();

    /**
     * get the fully qualified name filter used to filter types to be scanned
     */
    @Nullable
    Predicate<String> getInputsFilter();

    /**
     * executor service used to scan files. if null, scanning is done in a simple for loop
     */
    ExecutorService getExecutorService();

    /**
     * the default serializer to use when saving Reflection
     */
    Serializer getSerializer();

    /**
     * get class loaders, might be used for resolving methods/fields
     */
    @Nullable
    ClassLoader[] getClassLoaders();

    /**
     * if true (default), expand super types after scanning, for super types that were not scanned.
     * <p>see {@link Reflections#expandSuperTypes()}
     */
    boolean shouldExpandSuperTypes();
}
