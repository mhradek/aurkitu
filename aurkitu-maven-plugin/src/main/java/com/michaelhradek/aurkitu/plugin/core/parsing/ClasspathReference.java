package com.michaelhradek.aurkitu.plugin.core.parsing;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.net.URL;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
public class ClasspathReference {
    private URL url;
    private String artifact;
    private String groupId;

    /**
     * @return a valid package identifier.
     */
    public String getDerivedNamespace() {
        if (artifact == null || groupId == null) {
            return null;
        }

        return artifact + "." + groupId;
    }
}
