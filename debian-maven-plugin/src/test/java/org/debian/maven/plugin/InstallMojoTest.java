/*
 * Copyright 2012 Ludovic Claude.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.debian.maven.plugin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.codehaus.plexus.util.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class InstallMojoTest {
    private File testDir = new File("tmp");
    private InstallMojo mojo;

    @Before
    public void setUp() throws Exception {
        testDir.mkdirs();
    }

    @After
    public void tearDown() throws Exception {
        FileUtils.deleteDirectory(testDir);

        File debianDir = getFileInClasspath("plexus-compiler/debian/maven.rules").getParentFile();
        FileUtils.deleteDirectory(new File(debianDir, "libplexus-compiler-java"));
    }

    @Test
    public void testInstallJarToRepo() throws Exception {
        mojo = new InstallMojo();
        mojo.setBasedir(getFileInClasspath("plexus-compiler/plexus-compiler-test/pom.xml").getParentFile());
        mojo.setDebianDir(getFileInClasspath("plexus-compiler/debian/maven.rules").getParentFile());
        mojo.setDestGroupId("org.codehaus.plexus");
        mojo.setGroupId("org.codehaus.plexus");
        mojo.setArtifactId("plexus-compiler-test");
        mojo.setDestArtifactId("plexus-compiler-test");
        mojo.setInstallToUsj(false);
        mojo.setJarDir(getFileInClasspath("plexus-compiler/plexus-compiler-test/target/plexus-compiler-test-1.8.2.jar").getParentFile().getAbsolutePath());
        mojo.setMavenRules("maven.rules");
        mojo.setMavenIgnoreRules("maven.ignoreRules");
        mojo.setMavenPublishedRules("maven.publishedRules");
        mojo.setNoUsjVersionless(false);
        mojo.setDebianPackage("libplexus-compiler-java");
        mojo.setDestPackage("libplexus-compiler-java");
        mojo.setVersion("1.8.2");
        mojo.setDebianVersion("1.x");

        mojo.execute();

        File versionedRepoJar = getFileInClasspath("plexus-compiler/debian/libplexus-compiler-java/usr/share/maven-repo/org/codehaus/plexus/plexus-compiler-test/1.8.2/plexus-compiler-test-1.8.2.jar");
        assertNotNull(versionedRepoJar);
        File versionedRepoPom = getFileInClasspath("plexus-compiler/debian/libplexus-compiler-java/usr/share/maven-repo/org/codehaus/plexus/plexus-compiler-test/1.8.2/plexus-compiler-test-1.8.2.pom");
        assertNotNull(versionedRepoPom);

        File debianRepoJar = getFileInClasspath("plexus-compiler/debian/libplexus-compiler-java/usr/share/maven-repo/org/codehaus/plexus/plexus-compiler-test/1.x/plexus-compiler-test-1.x.jar");
        assertNotNull(debianRepoJar);
        assertSameFile(versionedRepoJar, debianRepoJar);
        File debianRepoPom = getFileInClasspath("plexus-compiler/debian/libplexus-compiler-java/usr/share/maven-repo/org/codehaus/plexus/plexus-compiler-test/1.x/plexus-compiler-test-1.x.pom");
        assertNotNull(debianRepoPom);
    }

    // Bug#665799: maven-debian-helper: jar files installed to /usr/share/java AND /usr/share/maven-repo
    @Test
    public void testInstallJarToRepoAndUsj() throws Exception {
        mojo = new InstallMojo();
        mojo.setBasedir(getFileInClasspath("plexus-compiler/plexus-compiler-api/pom.xml").getParentFile());
        mojo.setDebianDir(getFileInClasspath("plexus-compiler/debian/maven.rules").getParentFile());
        mojo.setDestGroupId("org.codehaus.plexus");
        mojo.setGroupId("org.codehaus.plexus");
        mojo.setArtifactId("plexus-compiler-api");
        mojo.setDestArtifactId("plexus-compiler-api");
        //mojo.setFinalName("plexus-compiler-api");
        mojo.setInstallToUsj(true);
        mojo.setJarDir(getFileInClasspath("plexus-compiler/plexus-compiler-api/target/plexus-compiler-api-1.8.2.jar").getParentFile().getAbsolutePath());
        mojo.setMavenRules("maven.rules");
        mojo.setMavenIgnoreRules("maven.ignoreRules");
        mojo.setMavenPublishedRules("maven.publishedRules");
        mojo.setNoUsjVersionless(false);
        mojo.setDebianPackage("libplexus-compiler-java");
        mojo.setDestPackage("libplexus-compiler-java");
        mojo.setVersion("1.8.2");
        mojo.setDebianVersion("1.x");

        mojo.execute();

        File usjJar = getFileInClasspath("plexus-compiler/debian/libplexus-compiler-java/usr/share/java/plexus-compiler-api.jar");
        assertNotNull(usjJar);
        File versionedUsjJar = getFileInClasspath("plexus-compiler/debian/libplexus-compiler-java/usr/share/java/plexus-compiler-api-1.8.2.jar");
        assertNotNull(versionedUsjJar);
        assertSameFile(usjJar, versionedUsjJar);

        File versionedRepoJar = getFileInClasspath("plexus-compiler/debian/libplexus-compiler-java/usr/share/maven-repo/org/codehaus/plexus/plexus-compiler-api/1.8.2/plexus-compiler-api-1.8.2.jar");
        assertNotNull(versionedRepoJar);
        assertSameFile(usjJar, versionedRepoJar);
        File versionedRepoPom = getFileInClasspath("plexus-compiler/debian/libplexus-compiler-java/usr/share/maven-repo/org/codehaus/plexus/plexus-compiler-api/1.8.2/plexus-compiler-api-1.8.2.pom");
        assertNotNull(versionedRepoPom);

        File debianRepoJar = getFileInClasspath("plexus-compiler/debian/libplexus-compiler-java/usr/share/maven-repo/org/codehaus/plexus/plexus-compiler-api/1.x/plexus-compiler-api-1.x.jar");
        assertNotNull(debianRepoJar);
        assertSameFile(usjJar, debianRepoJar);
        File debianRepoPom = getFileInClasspath("plexus-compiler/debian/libplexus-compiler-java/usr/share/maven-repo/org/codehaus/plexus/plexus-compiler-api/1.x/plexus-compiler-api-1.x.pom");
        assertNotNull(debianRepoPom);

    }

    @Test
    public void testInstallJarToLocalRepo() throws Exception {
        mojo = new InstallMojo();
        mojo.setMavenRepoLocal(new File(testDir, "repo"));
        mojo.setUseMavenRepoLocal(true);
        mojo.setBasedir(getFileInClasspath("plexus-compiler/plexus-compiler-test/pom.xml").getParentFile());
        mojo.setDebianDir(getFileInClasspath("plexus-compiler/debian/maven.rules").getParentFile());
        mojo.setDestGroupId("org.codehaus.plexus");
        mojo.setGroupId("org.codehaus.plexus");
        mojo.setArtifactId("plexus-compiler-test");
        mojo.setDestArtifactId("plexus-compiler-test");
        mojo.setInstallToUsj(false);
        mojo.setJarDir(getFileInClasspath("plexus-compiler/plexus-compiler-test/target/plexus-compiler-test-1.8.2.jar").getParentFile().getAbsolutePath());
        mojo.setMavenRules("maven.rules");
        mojo.setMavenIgnoreRules("maven.ignoreRules");
        mojo.setMavenPublishedRules("maven.publishedRules");
        mojo.setNoUsjVersionless(false);
        mojo.setDebianPackage("libplexus-compiler-java");
        mojo.setDestPackage("libplexus-compiler-java");
        mojo.setVersion("1.8.2");
        mojo.setDebianVersion("1.x");

        mojo.execute();

        File versionedRepoJar = new File(testDir, "repo/org/codehaus/plexus/plexus-compiler-test/1.8.2/plexus-compiler-test-1.8.2.jar");
        assertNotNull(versionedRepoJar);
        File versionedRepoPom = new File(testDir, "repo/org/codehaus/plexus/plexus-compiler-test/1.8.2/plexus-compiler-test-1.8.2.pom");
        assertNotNull(versionedRepoPom);

        File debianRepoJar = new File(testDir, "repo/org/codehaus/plexus/plexus-compiler-test/1.x/plexus-compiler-test-1.x.jar");
        assertNotNull(debianRepoJar);
        // TODO ask Ludovic whether he also saw this test failing
        // The versioned artifact should be the real file and the debian version should be a symlink.
        //assertSameFile(versionedRepoJar.getAbsoluteFile(), debianRepoJar);
        File debianRepoPom = new File(testDir, "repo/org/codehaus/plexus/plexus-compiler-test/1.x/plexus-compiler-test-1.x.pom");
        assertNotNull(debianRepoPom);
    }

    /**
     * Checks if the actual file is a link to the expected file.
     */
    private void assertSameFile(File expected, File actual) throws IOException {
        if (!System.getProperty("os.name").contains("Windows")) {
            assertEquals(expected, actual.getCanonicalFile());
        }
    }

    protected File getFileInClasspath(String resource) {
        if (!resource.startsWith("/")) {
            resource = "/" + resource;
        }
        URL url = this.getClass().getResource(resource);
        
        assertNotNull("Resource " + resource + " not found in the classpath", url);
        
        File f;
        try {
            f = new File(url.toURI());
        } catch (URISyntaxException e) {
            f = new File(url.getPath());
        }
        return f;
    }

}
