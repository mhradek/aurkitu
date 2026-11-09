package io.github.mhradek.aurkitu.gradle;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

/**
 * The {@code aurkitu { }} block. The options and their defaults are those of the Maven plugin's
 * {@code build-schema} goal; see the README for what each one does.
 */
public abstract class AurkituExtension {

    /** The schema's file name, without {@code .fbs}. Defaults to the project's name. */
    public abstract Property<String> getSchemaName();

    /** The schema's {@code namespace}, for every type without its own. Default {@code generated.flatbuffers}. */
    public abstract Property<String> getSchemaNamespace();

    /** Where the schema is written. Default {@code build/aurkitu/schemas}. */
    public abstract DirectoryProperty getOutputDirectory();

    /** Packages to scan, with their subpackages. Empty (the default) scans the whole classpath. */
    public abstract ListProperty<String> getSpecifiedPackages();

    /** Report schema problems as comments in the schema and in the log. Default true. */
    public abstract Property<Boolean> getValidate();

    /** Leave {@code static} fields out. Default true. */
    public abstract Property<Boolean> getIgnoreStaticMembers();

    /** Four characters written as {@code file_identifier}. */
    public abstract Property<String> getFileIdentifier();

    /** Schema files to {@code include}, e.g. {@code common.fbs}. */
    public abstract ListProperty<String> getIncludes();

    /** Write JDK value types ({@code UUID}, {@code BigDecimal}, {@code java.time}, ...) as {@code string}. Default true. */
    public abstract Property<Boolean> getMapCommonTypes();

    /** Path to {@code flatc}: with validate, the schema is compiled with it and the build fails if it is rejected. */
    public abstract Property<String> getFlatcPath();

    /** Only rewrite the schema file when its content changes. Default false. */
    public abstract Property<Boolean> getCacheEnabled();

    /** Write field names in snake_case. Default true. */
    public abstract Property<Boolean> getSnakeCaseFields();

    /** With snakeCaseFields, keep a run of capitals as one word ({@code userID} as {@code user_id}). Default true. */
    public abstract Property<Boolean> getSnakeCaseAcronyms();

    /** Write map keys with their own type, marked {@code (key)}. Default false. */
    public abstract Property<Boolean> getTypedMapKeys();

    /** Write a hash of the schema as a {@code // @version:} comment. Default false. */
    public abstract Property<Boolean> getGenerateVersion();

    /** With validate, fail the build when there are validation issues. Default false. */
    public abstract Property<Boolean> getFailOnValidationIssues();

    /** Write boxed scalars ({@code Integer}, ...) in tables as optional scalars ({@code = null}). Default false. */
    public abstract Property<Boolean> getOptionalScalars();

    /** Extension for binary files of this schema written by flatc ({@code file_extension}). */
    public abstract Property<String> getFileExtension();
}
