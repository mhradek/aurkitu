/**
 *
 */
package com.michaelhradek.aurkitu.core.output;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.michaelhradek.aurkitu.annotations.FlatBufferTable.TableStructureType;

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
                builder.append(property.options.get(Property.PropertyOptionKey.ARRAY));
                builder.append("]");
            } else if (property.type == FieldType.IDENT) {
                builder.append(property.options.get(Property.PropertyOptionKey.IDENT));
            } else {
                builder.append(property.type.toString());
            }

            if (!property.options.isEmpty()) {
                for (Entry<TypeDeclaration.Property.PropertyOptionKey, String> option : property.options.entrySet()) {
                    if (option.getKey() == Property.PropertyOptionKey.ARRAY) {
                        // Already grabbed this if we handled arrays above
                        continue;
                    }

                    if (option.getKey() == Property.PropertyOptionKey.IDENT) {
                        // Already grabbed this if we handled indent above
                        continue;
                    }

                    builder.append(" ");
                    builder.append(option.getValue());
                    builder.append(" ");
                }
            }

            builder.append(";");
            builder.append(System.lineSeparator());
        }

        builder.append("}");
        builder.append(System.lineSeparator());
        builder.append(System.lineSeparator());

        return builder.toString();
    }
}
