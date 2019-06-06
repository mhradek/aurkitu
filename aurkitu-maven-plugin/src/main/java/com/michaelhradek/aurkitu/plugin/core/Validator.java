package com.michaelhradek.aurkitu.plugin.core;

import com.michaelhradek.aurkitu.annotations.types.FieldType;
import com.michaelhradek.aurkitu.plugin.core.output.EnumDeclaration;
import com.michaelhradek.aurkitu.plugin.core.output.Schema;
import com.michaelhradek.aurkitu.plugin.core.output.TypeDeclaration;
import com.michaelhradek.aurkitu.plugin.core.output.TypeDeclaration.Property;
import com.michaelhradek.aurkitu.plugin.core.output.TypeDeclaration.Property.PropertyOptionKey;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Wither;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author m.hradek
 */
@Slf4j
@Getter
@Setter
@Wither
@AllArgsConstructor
public class Validator {

    private final static String REGEX_NAMESPACE = "[a-zA-Z_\\.]{1,}";

    private Schema schema;
    private boolean checkTables;
    private boolean checkEnums;
    private boolean checkNamespace;
    private List<Error> errors;

    public Validator() {
        errors = new ArrayList<>();

        // Default is to run all testing
        checkTables = true;
        checkEnums = true;
        checkNamespace = true;
    }

    /**
     *
     */
    public void validateSchema() {
        log.debug("Starting validator...");

        if (schema == null) {
            log.debug(" Schema was null; ending");
            return;
        }

        if (checkTables) {
            for (TypeDeclaration type : schema.getTypes()) {
                log.debug("Looking at type: " + type.getName());
                for (Property property : type.getProperties()) {
                    log.debug("  Examining property: " + property.name);
                    log.debug("  with type: " + property.type);
                    if (property.type != FieldType.IDENT && property.type != FieldType.ARRAY
                            && property.type != FieldType.MAP
                            && Utilities.isLowerCaseType(property.type.targetClass)) {
                        log.debug("    Property is lower case type: " + property.name);
                        continue;
                    }

                    if (property.options.get(PropertyOptionKey.IDENT) != null && property.options
                            .get(PropertyOptionKey.IDENT).contains("$")) {
                        log.debug("    Error located in IDENT: " + property.name);
                        Error error = new Error();
                        error.setLocation(type.getName());
                        error.setType(ErrorType.INVALID_PATH);
                        error.setProperty(property);
                        error.setComment(
                                "Ident type name contains '$'; using '@FlatBufferOptions(useFullName = true)' on inner not recommended: "
                                        + property.options.get(PropertyOptionKey.IDENT));
                        errors.add(error);
                    }

                    if (property.options.get(PropertyOptionKey.ARRAY) != null && property.options
                            .get(PropertyOptionKey.ARRAY).contains("$")) {
                        log.debug("    Error located in ARRAY: " + property.name);
                        Error error = new Error();
                        error.setLocation(type.getName());
                        error.setType(ErrorType.INVALID_PATH);
                        error.setProperty(property);
                        error.setComment(
                                "Array type name contains '$'; using '@FlatBufferOptions(useFullName = true)' on inner not recommended: "
                                        + property.options.get(PropertyOptionKey.ARRAY));
                        errors.add(error);
                    }

                    if (definitionExists(property)) {
                        log.debug("    Type definition exists: " + property.name);
                        continue;
                    }

                    log.debug("    Error located in: " + property.name);
                    Error error = new Error();
                    error.setLocation(type.getName());
                    error.setType(ErrorType.TYPE_DEFINITION_NOT_DEFINED);
                    error.setProperty(property);
                    errors.add(error);
                }
            }
        }

        if (checkEnums) {
            for (EnumDeclaration enumD : schema.getEnums()) {
                log.debug("Looking at enum: " + enumD.getName());

                if (enumD.getType() == null) {
                    log.debug("    Error located in enum: " + enumD.getName());
                    Error error = new Error();
                    error.setLocation(enumD.getName());
                    error.setType(ErrorType.MISCONFIGURED_DEFINITION);
                    errors.add(error);
                }

                if (enumD.getValues() == null || enumD.getValues().size() < 1) {
                    log.debug("    Error located in enum: " + enumD.getName());
                    Error error = new Error();
                    error.setComment("The enum contains no values.");
                    error.setLocation(enumD.getName());
                    error.setType(ErrorType.ENUM_DEFINITION_NOT_DEFINED);
                    errors.add(error);
                }
            }
        }

        // Check the namespace
        if (checkNamespace && !StringUtils.isEmpty(schema.getNamespace()) && !schema.getNamespace().matches(REGEX_NAMESPACE)) {
            Error error = new Error();
            error.setLocation("Schema -> namespace");
            error.setType(ErrorType.INVALID_NAMESPACE);
            error.setComment(String.format("If specified, namespace must be %s was [%s]", REGEX_NAMESPACE, schema.getNamespace()));
            errors.add(error);
        }
    }

    /**
     * @param input the Property to validate as having a definition within the Schema's list of known TypeDeclaration and EnumDeclaration.
     * @return boolean true or false
     */
    private boolean definitionExists(Property input) {
        log.debug("    Checking TypeDeclaration list for: " + input.name);
        log.debug("      with set type of: " + input.type);

        if (input.type.equals(FieldType.MAP)) {
            log.debug("    is a map: " + input.name);
            String mapName = input.options.get(PropertyOptionKey.MAP);
            log.debug("    with type name: " + mapName);

            // If it's a map and it's an upper case then it must be defined
            if (Character.isUpperCase(mapName.charAt(0))) {
                for (TypeDeclaration type : schema.getTypes()) {
                    log.debug("    against type (map): " + type.getName());
                    if (type.getName().equalsIgnoreCase(mapName)) {
                        return true;
                    }
                }

                for (EnumDeclaration enumD : schema.getEnums()) {
                    log.debug("    against enum (map): " + enumD.getName());
                    if (enumD.getName().equalsIgnoreCase(mapName)) {
                        return true;
                    }
                }
            }
        }

        if (input.type.equals(FieldType.ARRAY)) {
            log.debug("    is an array: " + input.name);
            String listTypeName = input.options.get(PropertyOptionKey.ARRAY);
            log.debug("    with type name: " + listTypeName);

            // If it's an array and it's an upper case then it must be defined
            if (Character.isUpperCase(listTypeName.charAt(0))) {
                for (TypeDeclaration type : schema.getTypes()) {
                    log.debug("    against type (array): " + type.getName());
                    if (type.getName().equalsIgnoreCase(listTypeName)) {
                        return true;
                    }
                }

                for (EnumDeclaration enumD : schema.getEnums()) {
                    log.debug("    against enum (array): " + enumD.getName());
                    if (enumD.getName().equalsIgnoreCase(listTypeName)) {
                        return true;
                    }
                }
            }
        }

        if (input.type.equals(FieldType.IDENT)) {
            log.debug("    is an ident: " + input.name);
            String identTypeName = input.options.get(PropertyOptionKey.IDENT);

            log.debug("    with type name: " + identTypeName);
            if (StringUtils.isEmpty(identTypeName)) {
                log.debug("    NULL identType name");
                Error error = new Error();
                error.setLocation(input.name);
                error.setType(ErrorType.MISSING_OR_INVALID_TYPE);
                error.setProperty(input);
                error.setComment(
                        String.format("The field for the type [%s] exists but is defined as null or empty", input.name));
                errors.add(error);
                return false;
            }

            if (Character.isUpperCase(identTypeName.charAt(0))) {
                for (TypeDeclaration type : schema.getTypes()) {
                    log.debug("    against type (ident): " + type.getName());
                    if (type.getName().equalsIgnoreCase(identTypeName)) {
                        return true;
                    }
                }

                for (EnumDeclaration enumD : schema.getEnums()) {
                    log.debug("    against enum (ident): " + enumD.getName());
                    if (enumD.getName().equalsIgnoreCase(identTypeName)) {
                        return true;
                    }
                }
            }
        }

        for (TypeDeclaration type : schema.getTypes()) {
            log.debug("    against type: " + type.getName());
            if (type.getName().equalsIgnoreCase(input.name)) {
                return true;
            }
        }

        for (EnumDeclaration enumD : schema.getEnums()) {
            log.debug("    against enum: " + enumD.getName());
            if (enumD.getName().equalsIgnoreCase(input.name)) {
                return true;
            }
        }

        return false;
    }

    /**
     * @return Any error comments generated during Schema validation
     */
    public String getErrorComments() {
        if (errors.isEmpty()) {
            return "// Schema passed validation";
        }

        StringBuilder builder = new StringBuilder("// Schema failed validation (i.e. flatc will likely fail): ");
        builder.append(System.lineSeparator());
        for (Error error : errors) {
            builder.append(error.toString());
        }
        builder.append(System.lineSeparator());

        return builder.toString();
    }

    enum ErrorType {
        TYPE_DEFINITION_NOT_DEFINED,
        ENUM_DEFINITION_NOT_DEFINED,
        MISCONFIGURED_DEFINITION,
        INVALID_PATH,
        MISSING_OR_INVALID_TYPE,
        INVALID_NAMESPACE
    }

    @Getter
    @Setter
    class Error {
        private String location;
        private Property property;
        private ErrorType type;
        private String comment;

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder("// ");
            builder.append("Issue : ");
            builder.append(type);
            builder.append(", Location: ");
            builder.append(location);
            builder.append(", Name: ");
            builder.append((property == null ? "null" : property.name));
            if (comment != null) {
                builder.append(", Comment: ");
                builder.append(comment);
            }
            builder.append(System.lineSeparator());

            return builder.toString();
        }
    }
}
