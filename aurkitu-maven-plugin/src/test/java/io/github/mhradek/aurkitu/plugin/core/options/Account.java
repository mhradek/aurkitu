package io.github.mhradek.aurkitu.plugin.core.options;

import io.github.mhradek.aurkitu.annotations.FlatBufferFieldOptions;
import io.github.mhradek.aurkitu.annotations.FlatBufferTable;
import io.github.mhradek.aurkitu.annotations.types.FieldType;

import java.util.Date;
import java.util.Map;
import java.util.Optional;

/**
 * A model every generator option visibly changes; see {@code OptionMatrixTest}.
 * Field order matters: inherited fields come last, so validation warns about them.
 */
@FlatBufferTable(rootType = true)
public class Account extends AccountBase {
    private long userId;                         // snakeCaseFields
    private Date createdAt;                      // mapCommonTypes
    private Integer loginCount;                  // optionalScalars
    @FlatBufferFieldOptions(fieldType = FieldType.INT, defaultValue = "3")
    private Integer retryLimit;                  // optionalScalars: never with a default
    @FlatBufferFieldOptions(fieldType = FieldType.STRING, required = true)
    private String displayName;
    private Map<String, Integer> scoreTotals;    // snakeCaseFields: map table name too
    private Point home;
    static int instanceCount;                    // ignoreStaticMembers
    private Optional<String> nickname;           // always unsupported: validate reports it

    public long getUserId() {
        return userId;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public Integer getLoginCount() {
        return loginCount;
    }

    public Integer getRetryLimit() {
        return retryLimit;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Map<String, Integer> getScoreTotals() {
        return scoreTotals;
    }

    public Point getHome() {
        return home;
    }

    public Optional<String> getNickname() {
        return nickname;
    }
}
