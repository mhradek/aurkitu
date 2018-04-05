package com.michaelhradek.aurkitu.core.output;

import com.michaelhradek.aurkitu.Config;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

/**
 * @author m.hradek
 */
@Getter
@Setter
public class Schema {

    // IDL values
    String name;
    String fileIdentifier;
    String fileExtension;
    String namespace;
    String rootType;
    List<EnumDeclaration> enums;
    List<TypeDeclaration> types;
    List<String> includes;
    List<String> attributes;
    List<Constant<Integer>> integerConstants;
    List<Constant<Float>> floatConstants;

    // Aurkitu values
    boolean generateVersion;
    boolean isValidSchema;

    public Schema() {
        enums = new ArrayList<EnumDeclaration>();
        types = new ArrayList<TypeDeclaration>();
        includes = new ArrayList<String>();
        attributes = new ArrayList<String>();
        integerConstants = new ArrayList<Constant<Integer>>();
        floatConstants = new ArrayList<Constant<Float>>();
    }

    /**
     * @param input Add an enum declaration to the schema
     */
    public void addEnumDeclaration(EnumDeclaration input) {
        if (!enums.contains(input)) {
            enums.add(input);
        }
    }

    /**
     * @param input Add a type (i.e. class) declaration to the schema
     */
    public void addTypeDeclaration(TypeDeclaration input) {
        if (!types.contains(input)) {
            types.add(input);
        }
    }

    /**
     * @param input Add another schema to include within this schema
     */
    public void addInclude(String input) {
        includes.add(input);
    }

    /**
     * @param input Add an attribute to the schema
     */
    public void addAttribute(String input) {
        attributes.add(input);
    }

    /**
     * @param input Add an integer constant to the schema
     */
    public void addIntegerConstant(Constant<Integer> input) {
        integerConstants.add(input);
    }

    /**
     * @param input Add a float constant to the schema
     */
    public void addFloatConstant(Constant<Float> input) {
        floatConstants.add(input);
    }

    /**
     * @param <T> A class which contains the name, value, and options used to define Numbers at the schema level
     *
     */
    public static class Constant<T extends Number> {
        public String name;
        public T value;
        public Map<String, String> options = new HashMap<String, String>();
    }

    /**
     * @param input Set the 4 character file identifier.
     */
    public void setFileIdentifier(String input) {
        if(input == null) {
            fileIdentifier = null;
            return;
        }

        if (input.length() != 4) {
            return;
        }

        fileIdentifier = input.toUpperCase();
    }

    /**
     * @param input Set the file extension. Default is {@link com.michaelhradek.aurkitu.Application#fileExtension}
     */
    public void setFileExtension(String input) {
        if(input == null) {
            fileExtension = null;
            return;
        }

        if (input.length() < 1) {
            return;
        }

        fileExtension = input.toLowerCase();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(Config.SCHEMA_INTRO_COMMENT);
        builder.append(System.lineSeparator());

        if (generateVersion) {
            builder.append(Config.SCHEMA_VERSION_COMMENT);
            builder.append(System.lineSeparator());
        }
        builder.append(System.lineSeparator());

        if (includes != null && includes.size() > 0) {
            for (String include : includes) {
                builder.append("include ");
                builder.append(include);
                if(!include.endsWith(";"))
                    builder.append(";");
                builder.append(System.lineSeparator());
            }

            builder.append(System.lineSeparator());
        }

        if (attributes != null && attributes.size() > 0) {
            for (String attribute : attributes) {
                builder.append("attribute \"");
                builder.append(attribute);
                builder.append("\"");
                builder.append(";");
                builder.append(System.lineSeparator());
            }

            builder.append(System.lineSeparator());
        }

        if (integerConstants.size() > 0) {
            for (Constant<Integer> constant : integerConstants) {
                builder.append("int ");
                builder.append(constant.name);
                builder.append(" ");
                builder.append(constant.value);
                builder.append(";");
                builder.append(System.lineSeparator());
            }

            builder.append(System.lineSeparator());
        }

        if (floatConstants.size() > 0) {
            for (Constant<Float> constant : floatConstants) {
                builder.append("float ");
                builder.append(constant.name);
                builder.append(" ");
                builder.append(constant.value);
                builder.append(";");
                builder.append(System.lineSeparator());
            }

            builder.append(System.lineSeparator());
        }

        if (namespace != null) {
            builder.append("namespace ");
            builder.append(namespace);
            if(!namespace.endsWith(";"))
                builder.append(";");
            builder.append(System.lineSeparator());
            builder.append(System.lineSeparator());
        }

        for (EnumDeclaration enumD : enums) {
            builder.append(enumD.toString());
        }

        for (TypeDeclaration typeD : types) {
            builder.append(typeD.toString());
        }

        if (rootType != null) {
            builder.append("root_type: ");
            builder.append(rootType);
            builder.append(";");
            builder.append(System.lineSeparator());
        }

        if (fileIdentifier != null) {
            builder.append("file_identifier ");
            builder.append("\"");
            builder.append(fileIdentifier);
            builder.append("\"");
            builder.append(";");
            builder.append(System.lineSeparator());
        }

        if (fileExtension != null) {
            builder.append("file_extension ");
            builder.append("\"");
            builder.append(fileExtension);
            builder.append("\"");
            builder.append(";");
            builder.append(System.lineSeparator());
        }

        String result = builder.toString();

        if (generateVersion) {
            return result.replace(Config.SCHEMA_VERSION_PLACEHOLDER,
                    Integer.toHexString(result.hashCode()));
        }

        return result;
    }
}
