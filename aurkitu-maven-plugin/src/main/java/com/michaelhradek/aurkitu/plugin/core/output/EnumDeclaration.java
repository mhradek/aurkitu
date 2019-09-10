package com.michaelhradek.aurkitu.plugin.core.output;

import com.michaelhradek.aurkitu.annotations.FlatBufferEnum.EnumStructureType;
import com.michaelhradek.aurkitu.annotations.types.EnumType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author m.hradek
 */
@Getter
@Setter
@EqualsAndHashCode
public class EnumDeclaration {

    private String name;
    private EnumStructureType structure;
    private EnumType type;
    private List<String> values = new ArrayList<>();
    private String comment;

    private boolean isDependencyEnum;

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
        if (!StringUtils.isEmpty(comment)) {
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

        if (builder.toString().contains(",")) {
            builder.deleteCharAt(builder.lastIndexOf(","));
        }

        builder.append("}");
        builder.append(System.lineSeparator());
        builder.append(System.lineSeparator());

        return builder.toString();
    }
}
