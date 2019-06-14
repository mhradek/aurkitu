package com.michaelhradek.aurkitu.plugin.core.output.components;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@Getter
@NoArgsConstructor
@EqualsAndHashCode
public class Namespace {

    private static final String NAMESPACE_SEPARATOR = ".";

    private String groupId;
    private String identifier;
    private String artifactId;

    /**
     * @param groupId    the group id. See {@link Namespace#setGroupId(String)}
     * @param identifier the identifier. See {@link Namespace#setIdentifier(String)}
     * @param artifactId the artifact id. See {@link Namespace#setArtifactId(String)}
     */
    public Namespace(String groupId, String identifier, String artifactId) {
        setGroupId(groupId);
        setIdentifier(identifier);
        setArtifactId(artifactId);
    }

    /**
     * @param namespace takes a string and parse a namespace from it. We use : to separate the parts of a namespace
     * @return a constructed namespace
     */
    public static Namespace parse(String namespace) {
        if (StringUtils.isEmpty(namespace)) {
            return null;
        }

        // Dashes will cause build issues
        namespace = namespace.replaceAll("-", "_");
        if (!namespace.contains(":")) {
            return new Namespace(namespace.trim().toLowerCase(), null, null);
        }

        String[] namespaceParts = namespace.split(":");
        for (int i = 0; i < namespaceParts.length; i++) {
            String part = namespaceParts[i].trim().toLowerCase();
            namespaceParts[i] = StringUtils.isEmpty(part) ? null : part;
        }

        if (namespaceParts.length < 3) {
            return new Namespace(namespaceParts[0], namespaceParts[1], null);
        }

        return new Namespace(namespaceParts[0], namespaceParts[1], namespaceParts[2]);
    }

    /**
     * @return if the namespace is empty. The group, identifier, and artifact must all be missing.
     */
    public boolean isEmpty() {
        return StringUtils.isEmpty(groupId) && StringUtils.isEmpty(identifier) && StringUtils.isEmpty(artifactId);

    }

    /**
     * @param groupId which is typically the beginning of a package name (e.g. [org.whitehouse].president)
     */
    public void setGroupId(String groupId) {
        if (StringUtils.isEmpty(groupId)) {
            this.groupId = null;
            return;
        }

        groupId = groupId.replaceAll("-", "_");
        this.groupId = groupId.trim().toLowerCase();
    }

    /**
     * @param identifier which is used to differentiate these generated classes and namespace from the originating code.
     *                   See {@link com.michaelhradek.aurkitu.plugin.Config#SCHEMA_NAMESPACE_IDENTIFIER_DEFAULT}
     */
    public void setIdentifier(String identifier) {
        if (StringUtils.isEmpty(identifier)) {
            this.identifier = null;
            return;
        }

        identifier = identifier.replaceAll("-", "_");
        this.identifier = identifier.trim().toLowerCase();
    }

    /**
     * @param artifactId which is typcially the actual target compilation's name (e.g. org.whitehouse.[president])
     */
    public void setArtifactId(String artifactId) {
        if (StringUtils.isEmpty(artifactId)) {
            this.artifactId = null;
            return;
        }

        artifactId = artifactId.replaceAll("-", "_");
        this.artifactId = artifactId.trim().toLowerCase();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        if (isEmpty()) {
            return builder.toString();
        }

        if (groupId != null) {
            builder.append(groupId);
            builder.append(NAMESPACE_SEPARATOR);
        }

        if (identifier != null) {
            builder.append(identifier);
            builder.append(NAMESPACE_SEPARATOR);
        }

        if (artifactId != null) {
            builder.append(artifactId);
            builder.append(NAMESPACE_SEPARATOR);
        }

        builder.replace(builder.lastIndexOf(NAMESPACE_SEPARATOR), builder.lastIndexOf(NAMESPACE_SEPARATOR) + 1, "");
        return builder.toString();
    }
}
