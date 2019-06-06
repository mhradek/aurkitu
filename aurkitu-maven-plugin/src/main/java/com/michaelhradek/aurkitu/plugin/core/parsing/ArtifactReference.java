package com.michaelhradek.aurkitu.plugin.core.parsing;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

import java.util.List;

/**
 * @author m.hradek
 */
@Getter
@AllArgsConstructor
@EqualsAndHashCode
public class ArtifactReference {
    private MavenProject mavenProject;
    private RepositorySystem repoSystem;
    private RepositorySystemSession repoSession;
    private List<RemoteRepository> repositories;
    private List<String> specifiedDependencies;
}
