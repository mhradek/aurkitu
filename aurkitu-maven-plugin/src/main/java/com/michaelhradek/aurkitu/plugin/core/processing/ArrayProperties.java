package com.michaelhradek.aurkitu.plugin.core.processing;

import com.michaelhradek.aurkitu.annotations.types.FieldType;
import com.michaelhradek.aurkitu.plugin.core.Processor;
import com.michaelhradek.aurkitu.plugin.core.Utilities;
import com.michaelhradek.aurkitu.plugin.core.output.TypeDeclaration;
import com.michaelhradek.aurkitu.plugin.core.processing.interfaces.PropertyExtractor;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;

@Slf4j
@AllArgsConstructor
public class ArrayProperties implements PropertyExtractor {

    private Processor processor;

    @Override
    public TypeDeclaration.Property process(TypeDeclaration.Property property, Field field, boolean useFullName) {
        log.debug("Found array (e.g. int[]) type. Setting FieldType.ARRAY and processing: " + field.getName());
        property.name = field.getName();
        property.type = FieldType.ARRAY;

        // Determine type of the array
        String name;
        if (Utilities.isPrimitiveOrWrapperType(field.getType().getComponentType())) {
            log.debug("Array parameter is primitive, wrapper, or String: " + field.getName());
            name = Utilities.getPrimitiveNameForWrapperType(field.getType().getComponentType());
        } else {
            // It may be a Class<?> which isn't a primitive (i.e. lowerCaseType)
            if (useFullName) {
                name = field.getType().getComponentType().getName();

                String simpleName = field.getType().getComponentType().getName()
                        .substring(field.getType().getComponentType().getName().lastIndexOf(".") + 1);
                String packageName = field.getType().getComponentType().getName()
                        .substring(0, field.getType().getComponentType().getName().lastIndexOf(".") + 1);
                log.debug(String
                        .format("Using full name; reviewing simpleName: %s and package: %s", simpleName, packageName));

                if (processor.getNamespaceOverrideMap() != null && processor.getNamespaceOverrideMap().containsKey(packageName)) {
                    name = processor.getNamespaceOverrideMap().get(packageName) + simpleName;
                    log.debug("Override located; using it: " + name);
                }
            } else {
                name = field.getType().getComponentType().getSimpleName();
            }
        }

        // In the end Array[] and List<?> are represented the same way.
        property.options.put(TypeDeclaration.Property.PropertyOptionKey.ARRAY, name);
        return property;
    }
}
