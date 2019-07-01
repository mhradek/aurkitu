package com.michaelhradek.aurkitu.plugin.core.processing;

import com.michaelhradek.aurkitu.annotations.types.FieldType;
import com.michaelhradek.aurkitu.plugin.core.Processor;
import com.michaelhradek.aurkitu.plugin.core.Utilities;
import com.michaelhradek.aurkitu.plugin.core.output.TypeDeclaration;
import com.michaelhradek.aurkitu.plugin.core.parsing.ClasspathSearchType;
import com.michaelhradek.aurkitu.plugin.core.processing.interfaces.PropertyExtractor;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.plugin.MojoExecutionException;

import java.lang.reflect.Field;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@AllArgsConstructor
public class MapProperties implements PropertyExtractor {

    private Processor processor;

    @Override
    public TypeDeclaration.Property process(TypeDeclaration.Property property, Field field, boolean useFullName) {
        log.debug("Found map type. Setting FieldType.MAP and processing: " + field.getName());
        property.name = field.getName();
        property.type = FieldType.MAP;

        // Stuff the types into this list
        List<TypeDeclaration.Property> properties = new ArrayList<>();

        // Get the type for the key and value
        String[] parametrizedTypeStrings = new String[]{String.class.getName(), String.class.getName()};
        try {
            parametrizedTypeStrings = Processor.parseFieldSignatureForParametrizedTypeStringsOnMap(field);
        } catch (Exception e) {
            log.warn("Unable to determine classes for Map<?, ?> parameter types", e);
        }

        // Attempt to load each type (technically will run twice as in A and B in example Map<A, B>)
        for (int i = 0; i < parametrizedTypeStrings.length; i++) {
            Class<?> mapTypeClass;
            TypeDeclaration.Property mapTypeProperty = new TypeDeclaration.Property();

            try {
                // Load all paths into custom classloader
                ClassLoader urlClassLoader = Thread.currentThread().getContextClassLoader();
                if (processor.getArtifactReference() != null && processor.getArtifactReference().getMavenProject() != null) {
                    urlClassLoader = URLClassLoader
                            .newInstance(Utilities.arrayForClasspathReferenceList(
                                    Utilities.buildProjectClasspathList(processor.getArtifactReference(), ClasspathSearchType.BOTH)), urlClassLoader);
                }

                // Parse Field signature
                mapTypeClass = urlClassLoader.loadClass(parametrizedTypeStrings[i]);

                if (mapTypeClass.getName().equals(Object.class.getName())) {
                    log.warn(
                            "Using Map<?, ?> where either `?` is `java.lang.Object` is not permitted; using `java.lang.String`");
                    mapTypeClass = String.class;
                }
            } catch (Exception e) {
                log
                        .warn("Unable to find and load class for Map<?, ?> parameter, using <String, String> instead: ",
                                e);
                mapTypeClass = String.class;
            }

            String name;
            try {
                if (!processor.isConsolidatedSchemas()) {
                    log.debug("Separated schemas requested; reviewing class");
                    Processor.ExternalClassDefinition externalClassDefinition = processor.getExternalClassDefinitionDetails(mapTypeClass);
                    if (externalClassDefinition.locatedOutside) {
                        name = externalClassDefinition.targetNamespace + "." + mapTypeClass.getSimpleName();
                    } else {
                        name = processor.getName(mapTypeClass, field, useFullName);
                    }
                } else {
                    name = processor.getName(mapTypeClass, field, useFullName);
                }
            } catch (MojoExecutionException e) {
                name = processor.getName(mapTypeClass, field, useFullName);
            }

            // Stuffing...
            if (i == 0) {
                mapTypeProperty.name = "key";
            } else {
                mapTypeProperty.name = "value";
            }

            mapTypeProperty.type = FieldType.IDENT;
            mapTypeProperty.options.put(TypeDeclaration.Property.PropertyOptionKey.IDENT, name);
            properties.add(mapTypeProperty);
        }

        // Create a new type and add it to the list of types
        TypeDeclaration mapType = new TypeDeclaration();
        final String mapTypeName = TypeDeclaration.MapValueSet.class.getSimpleName() + "_"
                + field.getDeclaringClass().getSimpleName() + "_" + field.getName();
        mapType.setName(mapTypeName);
        mapType.setComment("Auto-generated type for use with Map<?, ?>");

        // Set in this type the various types for the K/Vs used in this map
        mapType.setProperties(properties);

        try {
            if (processor.isConsolidatedSchemas() || !processor.getExternalClassDefinitionDetails(field.getDeclaringClass()).locatedOutside) {
                if (processor == null) {
                    System.out.println("NULL NULL NULL NULL NULL PROCESSOR");
                }

                if (processor.getCurrentSchema() == null) {
                    System.out.println("NULL NULL NULL NULL NULL CURRENT SCHEMA");
                }

                if (processor.getCurrentSchema().getTypeDeclarations() == null) {
                    System.out.println("NULL NULL NULL NULL NULL TYPES");
                }

                processor.getCurrentSchema().addTypeDeclaration(mapType);
            }
        } catch (MojoExecutionException e) {
            log.debug("Unable to determine if declaring class is located outside - skipped adding type [{}] to schema [{}] type definition list",
                    mapType.getName(), processor.getCurrentSchema().getName());
        }

        // Need a way to reference back to the new generated type
        property.options.put(TypeDeclaration.Property.PropertyOptionKey.MAP, mapTypeName);
        return property;
    }
}
