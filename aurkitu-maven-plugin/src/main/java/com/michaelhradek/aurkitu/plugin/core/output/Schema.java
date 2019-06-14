package com.michaelhradek.aurkitu.plugin.core.output;

import com.michaelhradek.aurkitu.plugin.Config;
import com.michaelhradek.aurkitu.plugin.core.Validator;
import com.michaelhradek.aurkitu.plugin.core.output.components.Namespace;
import com.michaelhradek.aurkitu.plugin.core.parsing.ClasspathReference;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

/**
 * @author m.hradek
 */
@Getter
@Setter
@EqualsAndHashCode
public class Schema {

    // IDL values
    private String name;
    private String fileIdentifier;
    private String fileExtension;
    private Namespace namespace;
    private String rootType;
    private List<EnumDeclaration> enums;
    private List<TypeDeclaration> types;
    private Set<String> includes;
    private List<String> attributes;
    private List<Constant<Integer>> integerConstants;
    private List<Constant<Float>> floatConstants;

    // Aurkitu values
    private boolean generateVersion;
    private Boolean isValid;
    private Validator validator;
    private boolean isDependency;
    private boolean isEmpty;

    // Classpath references used to create this schema
    private List<ClasspathReference> classpathReferenceList;

    public Schema() {
        enums = new ArrayList<>();
        types = new ArrayList<>();
        includes = new HashSet<>();
        attributes = new ArrayList<>();
        integerConstants = new ArrayList<>();
        floatConstants = new ArrayList<>();

        classpathReferenceList = new ArrayList<>();
    }

    /**
     * @param isEmpty If the schema had no classes to review from the classpath. We can check this also be reviewing the various lists but that could be a false negative/positive.
     */
    public void isEmpty(boolean isEmpty) {
        this.isEmpty = isEmpty;
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
     * @param input Set the 4 character file identifier.
     */
    public void setFileIdentifier(String input) {
        if (StringUtils.isEmpty(input)) {
            fileIdentifier = null;
            return;
        }

        if (input.length() != 4) {
            return;
        }

        fileIdentifier = input.toUpperCase();
    }

    /**
     * @param input Set the file extension. Default is {@link Config#FILE_EXTENSION}
     */
    public void setFileExtension(String input) {
        if (StringUtils.isEmpty(input)) {
            fileExtension = null;
            return;
        }

        fileExtension = input.toLowerCase();
    }

    /**
     * @param input The namespace for the schema. Dashes are replaced with underscores - otherwise flatc compilation will fail.
     */
    public void setNamespace(String input) {
        this.namespace = Namespace.parse(input);
    }

    /**
     * @param namespace The namespace. See {@link Schema#setNamespace(String)}
     */
    public void setNamespace(Namespace namespace) {
        this.namespace = namespace;
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
                builder.append("include \"");
                builder.append(include);
                builder.append("." + Config.FILE_EXTENSION);
                if (!include.endsWith(";"))
                    builder.append("\";");
                else
                    builder.insert(builder.length(), "\";");
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

        if (namespace != null && !namespace.isEmpty()) {
            builder.append("namespace ");
            final String outputNamespace = namespace.toString();
            builder.append(outputNamespace);
            if (!outputNamespace.endsWith(";"))
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
            builder.append("root_type ");
            builder.append(rootType);
            builder.append(";");
            builder.append(System.lineSeparator());
            builder.append(System.lineSeparator());
        }

        if (fileIdentifier != null) {
            builder.append("file_identifier ");
            builder.append("\"");
            builder.append(fileIdentifier);
            builder.append("\"");
            builder.append(";");
            builder.append(System.lineSeparator());
            builder.append(System.lineSeparator());
        }

        if (fileExtension != null) {
            builder.append("file_extension ");
            builder.append("\"");
            builder.append(fileExtension);
            builder.append("\"");
            builder.append(";");
            builder.append(System.lineSeparator());
            builder.append(System.lineSeparator());
        }

        if (isValid != null) {
            builder.append(validator.getErrorComments());
        }

        String result = builder.toString();

        if (generateVersion) {
            return result.replace(Config.SCHEMA_VERSION_PLACEHOLDER,
                    Integer.toHexString(result.hashCode()));
        }

        return result;
    }

    /**
     * @param <T> A class which contains the name, value, and options used to define Numbers at the schema level
     */
    public static class Constant<T extends Number> {
        public String name;
        public T value;
        public Map<String, String> options = new HashMap<>();
    }
}
