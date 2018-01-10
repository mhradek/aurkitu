/**
 *
 */
package com.michaelhradek.aurkitu.core;

import java.util.ArrayList;
import java.util.List;

import com.michaelhradek.aurkitu.Application;
import com.michaelhradek.aurkitu.core.output.EnumDeclaration;
import com.michaelhradek.aurkitu.core.output.FieldType;
import com.michaelhradek.aurkitu.core.output.Schema;
import com.michaelhradek.aurkitu.core.output.TypeDeclaration;
import com.michaelhradek.aurkitu.core.output.TypeDeclaration.Property;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Wither;

/**
 * @author m.hradek
 *
 */
@Getter
@Setter
@Wither
@AllArgsConstructor
public class Validator {

    Schema schema;
    boolean checkTables;
    boolean checkEnums;
    List<Error> errors;

    public Validator() {
        errors = new ArrayList<Error>();
        checkTables = true;
        checkEnums = true;
    }

    /**
     *
     *
     */
    public void validateSchema() {
        if (schema == null) {
            return;
        }

        if (checkTables) {
            for (TypeDeclaration type : schema.getTypes()) {
                Application.getLogger().debug("Looking at: " + type.getName());
                for (Property property : type.getProperties()) {
                    Application.getLogger().debug("  Examining property: " + property.name);
                    Application.getLogger().debug("  with type: " + property.type);
                    if (property.type != FieldType.IDENT && property.type != FieldType.ARRAY
                            && Utilities.isLowerCaseType(property.type.targetClass)) {
                        Application.getLogger().debug("    Property is lower case type: " + property.name);
                        continue;
                    }

                    if (definitionExists(property)) {
                        Application.getLogger().debug("    Type definition exists: " + property.name);
                        continue;
                    }

                    Application.getLogger().debug("    Error located in: " + property.name);
                    Error error = new Error();
                    error.setLocation(type.name);
                    error.setType(ErrorType.TYPE_DEFINITION_NOT_DEFINED);
                    error.setProperty(property);
                    errors.add(error);
                }
            }
        }

        if (checkEnums) {
            // TODO
        }
    }

    /**
     *
     * @param input
     * @return
     */
    boolean definitionExists(Property input) {
        Application.getLogger().debug("    Checking TypeDeclaration list for: " + input.name);
        if (input.type == FieldType.ARRAY) {
            Application.getLogger().debug("    is an array: " + input.name);
            String listTypeName = input.options.get(FieldType.ARRAY.toString());
            Application.getLogger().debug("    with type name: " + listTypeName);
            if (Character.isUpperCase(listTypeName.charAt(0))) {
                for (TypeDeclaration type : schema.getTypes()) {
                    Application.getLogger().debug("    against type (array): " + type.name);
                    if (type.name.equalsIgnoreCase(listTypeName)) {
                        return true;
                    }
                }

                for (EnumDeclaration enumD : schema.getEnums()) {
                    Application.getLogger().debug("    against enum (array): " + enumD.name);
                    if (enumD.name.equalsIgnoreCase(listTypeName)) {
                        return true;
                    }
                }
            } else {
                return true;
            }
        }

        if (input.type == FieldType.IDENT) {
            Application.getLogger().debug("    is an ident: " + input.name);
            String identTypeName = input.options.get(FieldType.IDENT.toString());
            Application.getLogger().debug("    with type name: " + identTypeName);
            if (Character.isUpperCase(identTypeName.charAt(0))) {
                for (TypeDeclaration type : schema.getTypes()) {
                    Application.getLogger().debug("    against type (ident): " + type.name);
                    if (type.name.equalsIgnoreCase(identTypeName)) {
                        return true;
                    }
                }

                for (EnumDeclaration enumD : schema.getEnums()) {
                    Application.getLogger().debug("    against enum (ident): " + enumD.name);
                    if (enumD.name.equalsIgnoreCase(identTypeName)) {
                        return true;
                    }
                }
            } else {
                return true;
            }
        }

        for (TypeDeclaration type : schema.getTypes()) {
            Application.getLogger().debug("    against type: " + type.name);
            if (type.name.equalsIgnoreCase(input.name)) {
                return true;
            }
        }

        for (EnumDeclaration enumD : schema.getEnums()) {
            Application.getLogger().debug("    against enum: " + enumD.name);
            if (enumD.name.equalsIgnoreCase(input.name)) {
                return true;
            }
        }

        return false;
    }

    @Getter
    @Setter
    class Error {
        String location;
        Property property;
        ErrorType type;

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder("// ");
            builder.append("Issue : ");
            builder.append(type);
            builder.append(" Location: ");
            builder.append(location);
            builder.append(" Name: ");
            builder.append(property.name);
            builder.append(System.lineSeparator());

            return builder.toString();
        }
    }

    enum ErrorType {
        TYPE_DEFINITION_NOT_DEFINED, ENUM_DEFINITION_NOT_DEFINED
    }

    /**
     *
     * @return Any error comments generated during Schema validation
     */
    public String getErrorComments() {
        if (errors.isEmpty()) {
            return "// Schema passed validation";
        }

        StringBuilder builder = new StringBuilder("// Schema failed validation: ");
        builder.append(System.lineSeparator());
        for (Error error : errors) {
            builder.append(error.toString());
        }
        builder.append(System.lineSeparator());

        return builder.toString();
    }
}
