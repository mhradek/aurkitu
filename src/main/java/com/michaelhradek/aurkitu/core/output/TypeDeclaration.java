package com.michaelhradek.aurkitu.core.output;

import com.michaelhradek.aurkitu.annotations.FlatBufferTable.TableStructureType;
import com.michaelhradek.aurkitu.core.output.TypeDeclaration.Property.PropertyOptionKey;
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
public class TypeDeclaration {

    public String name;
    public boolean isRoot;
    public TableStructureType structure;
    public List<Property> properties = new ArrayList<Property>();

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
     *
     *
     */
    public static class Property {
        public String name;
        public FieldType type;
        public Map<PropertyOptionKey, String> options = new HashMap<PropertyOptionKey, String>();

        public enum PropertyOptionKey {
            ARRAY,
            IDENT,
            DEFAULT_VALUE
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(structure.name().toLowerCase());
        builder.append(" ");
        builder.append(name);
        builder.append(" {");
        builder.append(System.lineSeparator());

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
            } else {
                builder.append(property.type.toString());
            }

            // This needs to be right before the property terminating character (i.e. ";")
            if (property.options.get(PropertyOptionKey.DEFAULT_VALUE) != null) {
                builder.append(" = ");
                builder.append(property.options.get(PropertyOptionKey.DEFAULT_VALUE));
            }

            builder.append(";");
            builder.append(System.lineSeparator());
        }

        builder.append("}");
        builder.append(System.lineSeparator());
        builder.append(System.lineSeparator());

        return builder.toString();
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
