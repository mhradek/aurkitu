package com.michaelhradek.aurkitu.plugin.core.output;

import com.michaelhradek.aurkitu.annotations.FlatBufferTable.TableStructureType;
import com.michaelhradek.aurkitu.annotations.types.FieldType;
import com.michaelhradek.aurkitu.plugin.core.output.TypeDeclaration.Property.PropertyOptionKey;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author m.hradek
 *
 */
@Getter
@Setter
public class TypeDeclaration {

    private String name;
    private boolean isRoot;
    private TableStructureType structure;
    private List<Property> properties = new ArrayList<Property>();
    private String comment;

    public TypeDeclaration(TableStructureType structure) {
        this.structure = structure;
    }

    public TypeDeclaration() {
        this.structure = TableStructureType.TABLE;
    }

    /**
     * @param input a property that is to be added to the list of declared poperties.
     */
    public void addProperty(Property input) {
        properties.add(input);
    }

    /**
     * A TypeDeclaration contains a list of Property in addition to top level fields such as the Type's name, whether or
     * not it is root, and potentially a comment.
     *
     */
    public static class Property {
        public String name;
        public FieldType type;
        public Map<PropertyOptionKey, String> options = new HashMap<PropertyOptionKey, String>();

        public enum PropertyOptionKey {
            ARRAY,
            IDENT,
            DEFAULT_VALUE,
            COMMENT,
            MAP
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (comment != null && !comment.isEmpty()) {
            builder.append("// ");
            builder.append(comment);
            builder.append(System.lineSeparator());
        }

        builder.append(structure.name().toLowerCase());
        builder.append(" ");
        builder.append(name);
        builder.append(" {");
        builder.append(System.lineSeparator());

        if (!properties.isEmpty()) {
            for (Property property : properties) {
                builder.append("  ");
                builder.append(property.name);
                builder.append(":");
                if (property.type == FieldType.ARRAY) {
                    builder.append("[");
                    builder.append(property.options.get(PropertyOptionKey.ARRAY));
                    builder.append("]");
                } else if (property.type == FieldType.IDENT) {
                    builder.append(property.options.get(PropertyOptionKey.IDENT));
                } else if (property.type == FieldType.MAP) {
                    builder.append("[");
                    builder.append(property.options.get(PropertyOptionKey.MAP));
                    builder.append("]");
                } else {
                    builder.append(property.type.toString());
                }

                // This needs to be right before the property terminating character (i.e. ";")
                if (property.options.get(PropertyOptionKey.DEFAULT_VALUE) != null) {
                    builder.append(" = ");
                    builder.append(property.options.get(PropertyOptionKey.DEFAULT_VALUE));
                }

                builder.append(";");

                if (property.options.get(PropertyOptionKey.COMMENT) != null) {
                    builder.append("\t// ");
                    builder.append(property.options.get(PropertyOptionKey.COMMENT));
                }

                builder.append(System.lineSeparator());
            }
        }

        builder.append("}");
        builder.append(System.lineSeparator());
        builder.append(System.lineSeparator());

        return builder.toString();
    }

    public static class MapValueSet {

        public String key;
        public String value;
    }

    @Override
    public int hashCode() {
        if (name == null) {
            return super.hashCode();
        }

        return name.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof TypeDeclaration) {
            TypeDeclaration toCompare = (TypeDeclaration) o;
            return this.name.equals(toCompare.name);
        }

        return false;
    }
}
