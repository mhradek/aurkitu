/**
 *
 */
package com.michaelhradek.aurkitu.core.output;

import java.util.ArrayList;
import java.util.List;

import com.michaelhradek.aurkitu.annotations.FlatBufferEnum.EnumStructureType;

import lombok.Getter;
import lombok.Setter;

/**
 * @author m.hradek
 */
@Getter
@Setter
public class EnumDeclaration {

    public String name;
    public EnumStructureType structure;
    public FieldType type;
    public List<String> values = new ArrayList<String>();

    public EnumDeclaration(EnumStructureType structure) {
        this.structure = structure;
    }

    public EnumDeclaration() {
        this.structure = EnumStructureType.ENUM;
    }

    /**
     * @param value Add a value to the List of values the schema will need to include
     */
    public void addValue(String value) {
        values.add(value);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(structure.name().toLowerCase());
        builder.append(" ");
        builder.append(name);
        if (type != null) {
            builder.append(" : ");
            builder.append(type.toString());
        }

        builder.append(" { ");

        for (String value : values) {
            builder.append(value);
            builder.append(", ");
        }

        builder.deleteCharAt(builder.lastIndexOf(","));

        builder.append("}");
        builder.append(System.lineSeparator());
        builder.append(System.lineSeparator());

        return builder.toString();
    }
}
