package com.michaelhradek.aurkitu.plugin.core.processing;

import com.michaelhradek.aurkitu.annotations.types.FieldType;
import com.michaelhradek.aurkitu.plugin.core.Processor;
import com.michaelhradek.aurkitu.plugin.core.Utilities;
import com.michaelhradek.aurkitu.plugin.core.output.TypeDeclaration;
import com.michaelhradek.aurkitu.plugin.core.parsing.ClasspathReference;
import com.michaelhradek.aurkitu.plugin.core.parsing.ClasspathSearchType;
import com.michaelhradek.aurkitu.plugin.core.processing.interfaces.PropertyExtractor;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

@Slf4j
@AllArgsConstructor
public class ListProperties implements PropertyExtractor {

    private Processor processor;

    @Override
    public TypeDeclaration.Property process(TypeDeclaration.Property property, Field field, boolean useFullName) {
        log.debug("Found set or list type. Setting FieldType.ARRAY and processing: " + field.getName());
        property.name = field.getName();
        property.type = FieldType.ARRAY;

        Class<?> listTypeClass;

        try {
            // Load all paths into custom classloader
            ClassLoader urlClassLoader = Thread.currentThread().getContextClassLoader();
            if (processor.getArtifactReference() != null && processor.getArtifactReference().getMavenProject() != null) {

                List<ClasspathReference> classpathReferenceList = Utilities.buildProjectClasspathList(processor.getArtifactReference(), ClasspathSearchType.BOTH);
                URL[] targetUrlArray = new URL[classpathReferenceList.size()];
                for (int i = 0; i < classpathReferenceList.size(); i++) {
                    targetUrlArray[i] = classpathReferenceList.get(i).getUrl();
                }

                urlClassLoader = URLClassLoader.newInstance(targetUrlArray, urlClassLoader);
            }

            // Get type class name
            String[] typeClassNames = Processor.getTypeClassNamesFromParameterizedType(field);
            if (typeClassNames.length > 1) {
                log.warn("Field " + field.getName()
                    + " has more than one type class; only first one supported. Type classes: "
                    + String.join(",", typeClassNames));
            }
            listTypeClass = urlClassLoader.loadClass(typeClassNames[0]);
        } catch (Exception e) {
            log.warn("Unable to find and load class for List<?> parameter, using String instead (field name): " + field.getName());
            log.warn("Exception:", e);
            listTypeClass = String.class;
        }

        // Consolidated versus separated schema check
        String name = processor.getName(listTypeClass, field, useFullName);
        try {
            if (!processor.isConsolidatedSchemas()) {
                log.debug("Separated schemas requested; reviewing class");
                Processor.ExternalClassDefinition externalClassDefinition = processor.getExternalClassDefinitionDetails(listTypeClass);
                if (externalClassDefinition.locatedOutside) {
                    log.debug(" Located outside this schema; using other namespace");
                    name = externalClassDefinition.targetNamespace + "." + listTypeClass.getSimpleName();
                }
            }
        } catch (Exception e) {
            log.warn("Unable to get external class definition for unconsolidated schema", e);
        }


        property.options.put(TypeDeclaration.Property.PropertyOptionKey.ARRAY, name);
        return property;
    }
}
