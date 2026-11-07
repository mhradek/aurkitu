package com.example.kitchensink;

import io.github.mhradek.aurkitu.annotations.FlatBufferComment;
import io.github.mhradek.aurkitu.annotations.FlatBufferFieldOptions;
import io.github.mhradek.aurkitu.annotations.FlatBufferIgnore;
import io.github.mhradek.aurkitu.annotations.FlatBufferTable;
import io.github.mhradek.aurkitu.annotations.types.FieldType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@FlatBufferComment(comment = "A player profile; balances are in $")
@FlatBufferTable(rootType = true)
public class Player extends Entity {

    // Scalars and wrappers
    private boolean active;
    private byte mood;
    private short floor;
    private int lives;
    private long xp;
    private float speed;
    private double ratio;
    private Boolean verified;
    private Integer rank;
    private Double rating;

    // Field options
    @FlatBufferFieldOptions(fieldType = FieldType.STRING, required = true)
    private String name;

    @FlatBufferComment(comment = "Starting health")
    @FlatBufferFieldOptions(fieldType = FieldType.INT, defaultValue = "100", attributes = {"priority: 2"})
    private int health;

    @FlatBufferFieldOptions(fieldType = FieldType.UBYTE)
    private byte level;

    @FlatBufferFieldOptions(fieldType = FieldType.USHORT)
    private short port;

    @FlatBufferFieldOptions(fieldType = FieldType.UINT, hash = "fnv1a_32")
    private int nameHash;

    @FlatBufferFieldOptions(fieldType = FieldType.ULONG)
    private long score;

    @FlatBufferFieldOptions(fieldType = FieldType.INT, deprecated = true)
    private int legacyRank;

    // Enums, unions, structs and tables
    @FlatBufferFieldOptions(fieldType = FieldType.IDENT, defaultValue = "GREEN")
    private Color favouriteColor;
    // Status and Priority have no value 0 (the implicit default), so a default is required.
    @FlatBufferFieldOptions(fieldType = FieldType.IDENT, defaultValue = "ACTIVE")
    private Status status;

    @FlatBufferFieldOptions(fieldType = FieldType.IDENT, defaultValue = "LOW")
    private Priority priority;
    private Shape avatar;
    private Perm permissions;
    private Mood temper;
    private Vec3 spawn;
    private Transform transform;
    private PlayerList friends;
    private SharedMeta meta;
    private AuditEntry lastAudit;

    @FlatBufferFieldOptions(fieldType = FieldType.IDENT, useFullName = true)
    private Item mainHand;

    // Vectors and maps
    private List<String> tags;
    private Set<Integer> badges;
    private String[] aliases;
    private List<Item> inventory;
    private Achievement[] achievements;
    private Map<String, Integer> stats;
    private Map<String, Map<String, Integer>> levels;

    // Binary data
    private byte[] thumbnail;

    @FlatBufferFieldOptions(fieldType = FieldType.ARRAY, nestedFlatbuffer = "Item")
    private byte[] featuredItem;

    @FlatBufferFieldOptions(fieldType = FieldType.ARRAY, flexbuffer = true)
    private byte[] extras;

    @FlatBufferFieldOptions(fieldType = FieldType.ARRAY, forceAlign = 16)
    private byte[] aligned;

    // Common types (mapCommonTypes defaults to true)
    private UUID externalId;
    private Date lastLogin;
    private BigDecimal balance;
    private Instant joinedAt;

    // Excluded
    @FlatBufferIgnore
    private String sessionToken;

    private static int instances;

    /** Nested and not annotated: declared automatically because Player is annotated. */
    public enum Mood {
        CALM, ANGRY
    }

    /** Nested type: declared and referenced as "Achievement". */
    @FlatBufferTable
    public static class Achievement {
        private String title;
        private long unlockedAt;
    }
}
