package com.michaelhradek.aurkitu.plugin.core.parsing;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.net.URL;

@Getter
@AllArgsConstructor
public class ClasspathReference {
    private URL url;
    private String artifact;
    private String groupId;
}
