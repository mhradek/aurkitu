package com.michaelhradek.aurkitu.plugin.core.processing;

import com.michaelhradek.aurkitu.annotations.types.FieldType;
import com.michaelhradek.aurkitu.plugin.core.Processor;
import com.michaelhradek.aurkitu.plugin.core.output.TypeDeclaration;
import com.michaelhradek.aurkitu.plugin.core.processing.interfaces.PropertyExtractor;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

@Slf4j
@AllArgsConstructor
public class ClassProperties implements PropertyExtractor {

    private Processor processor;

    @Override
    public TypeDeclaration.Property process(TypeDeclaration.Property property, Field field, boolean useFullName) {
        String name = field.getName();
        log.debug("Found unrecognized type; assuming FieldType.IDENT(IFIER) and running processClass(...): " + name);

        property.name = name;
        property.type = FieldType.IDENT;

        Type fieldType = field.getGenericType();
        String identName;

        try {
            Class<?> clazz;
            if (processor.getArtifactReference() != null && processor.getArtifactReference().getMavenProject() != null) {
                clazz = Processor.getClassForClassName(processor.getArtifactReference().getMavenProject(), processor.getCurrentSchema(), fieldType.getTypeName());
            } else {
                clazz = Thread.currentThread().getContextClassLoader().loadClass(fieldType.getTypeName());
            }

            // Consolidated versus separated schema check
            if (!processor.isConsolidatedSchemas()) {
                log.debug("Separated schemas requested; reviewing class");
                Processor.ExternalClassDefinition externalClassDefinition = processor.getExternalClassDefinitionDetails(clazz);
                if (externalClassDefinition.locatedOutside) {
                    identName = externalClassDefinition.targetNamespace + "." + clazz.getSimpleName();
                } else {
                    identName = useFullName ? clazz.getName() : clazz.getSimpleName();
                }
            } else {
                identName = useFullName ? clazz.getName() : clazz.getSimpleName();
            }

            if (useFullName) {
                String simpleName = clazz.getName().substring(clazz.getName().lastIndexOf(".") + 1);
                String packageName = clazz.getName().substring(0, clazz.getName().lastIndexOf(".") + 1);
                if (processor.getNamespaceOverrideMap() != null && processor.getNamespaceOverrideMap().containsKey(packageName)) {
                    identName = processor.getNamespaceOverrideMap().get(packageName) + simpleName;
                    log.debug("Override located; using it: " + identName);
                }
            }
        } catch (Exception e) {
            if (!processor.getWarnedTypeNames().contains(fieldType.getTypeName())) {
                if (e instanceof ClassNotFoundException) {
                    log.warn("Class not found for type name: " + fieldType.getTypeName());
                } else {
                    log.warn("Unable to get class for name: " + fieldType.getTypeName(), e);
                }
                processor.getWarnedTypeNames().add(fieldType.getTypeName());
            }

            if (useFullName) {
                identName = fieldType.getTypeName();
                String simpleName = identName.substring(identName.lastIndexOf(".") + 1);
                String packageName = identName.substring(0, identName.lastIndexOf(".") + 1);
                if (processor.getNamespaceOverrideMap() != null && processor.getNamespaceOverrideMap().containsKey(packageName)) {
                    identName = processor.getNamespaceOverrideMap().get(packageName) + simpleName;
                }
            } else {
                identName = fieldType.getTypeName().substring(fieldType.getTypeName().lastIndexOf(".") + 1);

                if (identName.contains("$")) {
                    identName = identName.substring(identName.lastIndexOf("$"));
                }

                log.debug("Trimmed: " + fieldType.getTypeName() + " to " + identName);
            }
        }

        property.options.put(TypeDeclaration.Property.PropertyOptionKey.IDENT, identName);
        return property;
    }
}
