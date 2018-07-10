package com.michaelhradek.aurkitu.core.output;

import com.michaelhradek.aurkitu.annotations.types.EnumType;
import com.michaelhradek.aurkitu.annotations.FlatBufferEnum.EnumStructureType;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * @author m.hradek
 */
@Getter
@Setter
public class EnumDeclaration {

    private String name;
    private EnumStructureType structure;
    private EnumType type;
    private List<String> values = new ArrayList<String>();
    private String comment;

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
        StringBuilder builder = new StringBuilder();
        if (comment != null && !comment.isEmpty()) {
            builder.append("// ");
            builder.append(comment);
            builder.append(System.lineSeparator());
        }

        builder.append(structure.name().toLowerCase());
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

    @Override
    public int hashCode() {
        if (name == null) {
            return super.hashCode();
        }

        return name.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof EnumDeclaration) {
            EnumDeclaration toCompare = (EnumDeclaration) o;
            return this.hashCode() == o.hashCode();
        }

        return false;
    }
}
