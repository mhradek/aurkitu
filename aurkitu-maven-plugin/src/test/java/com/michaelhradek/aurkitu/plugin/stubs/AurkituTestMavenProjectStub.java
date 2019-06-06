package com.michaelhradek.aurkitu.plugin.stubs;

import lombok.Getter;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Reporting;
import org.apache.maven.model.Resource;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.codehaus.plexus.PlexusTestCase;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Collections;

@Getter
public class AurkituTestMavenProjectStub extends MavenProjectStub {

    private Build build;

    public AurkituTestMavenProjectStub() {
        File aurkituTestDir = new File(PlexusTestCase.getBasedir() + "/src/test/resources/plugin-basic-with-project/");

        MavenXpp3Reader pomReader = new MavenXpp3Reader();
        Model model;

        try {
            File pomFile = new File(aurkituTestDir, "pom.xml");
            // TODO: Once plexus-utils has been bumped to 1.4.4, use ReaderFactory.newXmlReader()
            model = pomReader.read(new InputStreamReader(new FileInputStream(pomFile), "UTF-8"));
            setModel(model);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        setGroupId(model.getGroupId());
        setArtifactId(model.getArtifactId());
        setVersion(model.getVersion());
        setName(model.getName());
        setUrl(model.getUrl());
        setPackaging(model.getPackaging());

        build = new Build();
        Resource resource = new Resource();

        build.setFinalName(model.getArtifactId());
        build.setDirectory(getBasedir().getAbsolutePath() + "/target");

        build.setSourceDirectory(aurkituTestDir + "/src/main/java");
        resource.setDirectory(aurkituTestDir + "/src/main/resources");
        build.setResources(Collections.singletonList(resource));
        build.setOutputDirectory(getBasedir().getAbsolutePath() + "/target/classes");

        build.setTestSourceDirectory(aurkituTestDir + "/src/test/java");
        resource = new Resource();
        resource.setDirectory(aurkituTestDir + "/src/test/resources");
        build.setTestResources(Collections.singletonList(resource));
        build.setTestOutputDirectory(getBasedir().getAbsolutePath() + "/target/test-classes");

        setBuild(build);

        Reporting reporting = new Reporting();
        reporting.setOutputDirectory(getBasedir().getAbsolutePath() + "/target/site");
        getModel().setReporting(reporting);

        addCompileSourceRoot(PlexusTestCase.getBasedir());
    }
}
