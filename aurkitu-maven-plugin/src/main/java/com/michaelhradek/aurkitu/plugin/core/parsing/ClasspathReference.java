package com.michaelhradek.aurkitu.plugin.core.parsing;

import com.michaelhradek.aurkitu.plugin.core.output.components.Namespace;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.net.URL;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
public class ClasspathReference {
    private URL url;
    private String groupId;
    private String artifactId;

    /**
     *
     * @return a constructed namespace from this class path reference. No identifier is set.
     */
    public Namespace getNamespace() {
        return new Namespace(groupId, null, artifactId);
    }
}
