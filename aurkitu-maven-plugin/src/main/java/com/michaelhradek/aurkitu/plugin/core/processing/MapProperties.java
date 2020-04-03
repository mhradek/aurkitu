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
        String[] typeClassNames = new String[]{String.class.getName(), String.class.getName()};
        try {
            typeClassNames = Processor.getTypeClassNamesFromParameterizedType(field);
        } catch (Exception e) {
            log.warn("Unable to determine classes for Map<?, ?> parameter types", e);
        }

        // Attempt to load each type (usually will run twice as in A and B in example Map<A, B>)
        for (int i = 0; i < typeClassNames.length; i++) {
            Class<?> mapTypeClass;
            TypeDeclaration.Property mapTypeProperty = new TypeDeclaration.Property();

            try {
                // load the type class
                if (typeClassNames[i].equals(Object.class.getName())) {
                    log.warn(
                            "Using Map<?, ?> where either `?` is `java.lang.Object` is not permitted; using `java.lang.String`");
                    mapTypeClass = String.class;
                } else {
                    // Load all paths into custom classloader
                    ClassLoader urlClassLoader = Thread.currentThread().getContextClassLoader();
                    if (processor.getArtifactReference() != null && processor.getArtifactReference().getMavenProject() != null) {
                        urlClassLoader = URLClassLoader
                                .newInstance(Utilities.arrayForClasspathReferenceList(
                                        Utilities.buildProjectClasspathList(processor.getArtifactReference(), ClasspathSearchType.BOTH)), urlClassLoader);
                    }

                    mapTypeClass = urlClassLoader.loadClass(typeClassNames[i]);
                }
            } catch (Exception e) {
                log
                        .warn("Unable to find and load class [" + typeClassNames[i] + "] for Map<?, ?> parameter, using <String, String> instead: ",
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
                mapTypeProperty.name = "key"; // String
            } else {
                mapTypeProperty.name = "value";
            }

            mapTypeProperty.type = FieldType.IDENT;
            mapTypeProperty.options.put(TypeDeclaration.Property.PropertyOptionKey.IDENT, name);

            // We can only hav 2. If there's more than 2 then we're dealing with complex types inside the map
            if(i > 1) {
                TypeDeclaration.Property tempProperty = properties.get(1);

                // Handle lists
                if(tempProperty.options.get(TypeDeclaration.Property.PropertyOptionKey.IDENT).equalsIgnoreCase("list") && i == 2) {
                    tempProperty.type = FieldType.ARRAY;
                    tempProperty.options.put(TypeDeclaration.Property.PropertyOptionKey.ARRAY, name);
                    properties.set(1, tempProperty);
                }
            } else {
                properties.add(mapTypeProperty);
            }
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
