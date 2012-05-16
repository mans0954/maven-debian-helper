package org.debian.maven.packager;

/*
 * Copyright 2009 Ludovic Claude.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicab le law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Reader;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.debian.maven.repo.DependencyRule;

public class DependenciesSolverTest extends TestCase {

    private File testDir = new File("tmp");
    private File pomFile = new File(testDir, "pom.xml");
    private List<Reader> openedReaders = new ArrayList<Reader>();

    protected void setUp() throws Exception {
        super.setUp();
        testDir.mkdirs();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        for (Iterator<Reader> i = openedReaders.iterator(); i.hasNext(); ) {
            Reader reader = i.next();
            try {
                reader.close();
            } catch (IOException ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            }
        }
        openedReaders.clear();
        FileUtils.deleteDirectory(testDir);
    }

    /**
     * Test of solveDependencies method, of class DependenciesSolver.
     */
    public void testSolvePlexusActiveCollectionsDependencies() throws Exception {
        useFile("plexus-active-collections/pom.xml", pomFile);
        DependenciesSolver solver = new DependenciesSolver();
        solver.setMavenRepo(getFileInClasspath("repository/root.dir").getParentFile());
        solver.setOutputDirectory(testDir);
        solver.setExploreProjects(true);
        solver.setPackageName("libplexus-active-collections-java");
        solver.setPackageType("maven");
        File listOfPoms = getFileInClasspath("libplexus-active-collections-java.poms");
        solver.setBaseDir(getFileInClasspath("plexus-active-collections/pom.xml").getParentFile());
        solver.setListOfPoms(new File(listOfPoms.getParent(), listOfPoms.getName()));
        solver.setInteractive(false);
        solver.setOffline(true);

        solver.solveDependencies();

        assertTrue("Did not expect any issues", solver.getIssues().isEmpty());

        solver.setBaseDir(testDir);
        solver.setListOfPoms(new File(testDir, "libplexus-active-collections-java.poms"));

        solver.saveListOfPoms();
        solver.saveMavenRules();
        solver.saveSubstvars();

        assertFileEquals("libplexus-active-collections-java.poms", "libplexus-active-collections-java.poms");
        assertFileEquals("libplexus-active-collections-java.substvars", "libplexus-active-collections-java.substvars");
        assertFileEquals("libplexus-active-collections-java.rules", "maven.rules");
    }

    /**
     * Test of solveDependencies method, of class DependenciesSolver.
     */
    public void testSolvePlexusUtils2Dependencies() throws Exception {
        useFile("plexus-utils2/pom.xml", pomFile);
        DependenciesSolver solver = new DependenciesSolver();
        solver.setMavenRepo(getFileInClasspath("repository/root.dir").getParentFile());
        solver.setOutputDirectory(testDir);
        solver.setExploreProjects(true);
        solver.setPackageName("libplexus-utils2-java");
        solver.setPackageType("maven");
        solver.getPomTransformer().addIgnoreRule(new DependencyRule("org.apache.maven.plugins maven-release-plugin * *"));
        File listOfPoms = getFileInClasspath("libplexus-utils2-java.poms");
        solver.setBaseDir(getFileInClasspath("plexus-utils2/pom.xml").getParentFile());
        solver.setListOfPoms(new File(listOfPoms.getParent(), listOfPoms.getName()));
        solver.setInteractive(false);
        solver.setOffline(true);

        solver.solveDependencies();

        assertTrue("Did not expect any issues", solver.getIssues().isEmpty());

        solver.setBaseDir(testDir);
        solver.setListOfPoms(new File(testDir, "libplexus-utils2-java.poms"));

        solver.saveListOfPoms();
        solver.saveMavenRules();
        solver.saveSubstvars();

        assertFileEquals("libplexus-utils2-java.poms", "libplexus-utils2-java.poms");
        assertFileEquals("libplexus-utils2-java.substvars", "libplexus-utils2-java.substvars");
        assertFileEquals("libplexus-utils2-java.rules", "maven.rules");
    }

    /**
     * Test of solveDependencies method, of class DependenciesSolver.
     */
    public void testSolveOpenMRSDependenciesWithErrors() throws Exception {
        useFile("openmrs/pom.xml", pomFile);
        DependenciesSolver solver = new DependenciesSolver();
        solver.setMavenRepo(getFileInClasspath("repository/root.dir").getParentFile());
        solver.setOutputDirectory(testDir);
        solver.setExploreProjects(false);
        solver.setPackageName("openmrs");
        solver.setPackageType("maven");
        //solver.getPomTransformer().addIgnoreRule(new DependencyRule("org.apache.maven.plugins maven-release-plugin * *"));
        File listOfPoms = getFileInClasspath("openmrs.poms");
        solver.setBaseDir(getFileInClasspath("openmrs/pom.xml").getParentFile());
        solver.setListOfPoms(new File(listOfPoms.getParent(), listOfPoms.getName()));
        solver.setInteractive(false);
        solver.setOffline(true);

        solver.solveDependencies();

        assertEquals(1, solver.getIssues().size());
        assertTrue(solver.getIssues().get(0).toString().indexOf("buildnumber-maven-plugin") > 0);
    }

    public void testSolveOpenMRSDependencies() throws Exception {
        useFile("openmrs/pom.xml", pomFile);
        DependenciesSolver solver = new DependenciesSolver();
        solver.setMavenRepo(getFileInClasspath("repository/root.dir").getParentFile());
        solver.setOutputDirectory(testDir);
        solver.setExploreProjects(false);
        solver.setPackageName("openmrs");
        solver.setPackageType("maven");
        solver.getPomTransformer().addIgnoreRule(new DependencyRule("org.openmrs.codehaus.mojo buildnumber-maven-plugin * *"));
        solver.getPomTransformer().addIgnoreRule(new DependencyRule("org.codehaus.mojo build-helper-maven-plugin * *"));
        solver.getPomTransformer().addIgnoreRule(new DependencyRule("org.apache.maven.plugins maven-assembly-plugin * *"));
        File listOfPoms = getFileInClasspath("openmrs.poms");
        solver.setBaseDir(getFileInClasspath("openmrs/pom.xml").getParentFile());
        solver.setListOfPoms(new File(listOfPoms.getParent(), listOfPoms.getName()));
        solver.setInteractive(false);
        solver.setOffline(true);

        solver.solveDependencies();

        assertTrue("Did not expect any issues", solver.getIssues().isEmpty());

        solver.setBaseDir(testDir);
        solver.setListOfPoms(new File(testDir, "openmrs.poms"));

        solver.saveListOfPoms();
        solver.saveMavenRules();
        solver.saveSubstvars();

        assertFileEquals("openmrs.poms", "openmrs.poms");
        assertFileEquals("openmrs.substvars", "openmrs.substvars");
        assertFileEquals("openmrs.rules", "maven.rules");
    }

    public void testSolveOpenMRSApiDependencies() throws Exception {
        useFile("openmrs/api/pom.xml", pomFile);
        DependenciesSolver solver = new DependenciesSolver();
        solver.setMavenRepo(getFileInClasspath("repository/root.dir").getParentFile());
        solver.setOutputDirectory(testDir);
        solver.setExploreProjects(false);
        solver.setPackageName("openmrs");
        solver.setPackageType("maven");
        solver.setVerbose(true);
        solver.getPomTransformer().addRule(new DependencyRule("cglib s/cglib-nodep/cglib jar s/.*/debian/ * *"));
        // Some dependencies are ignored here because there's a long list of libraries not packaged yet in Debian
        solver.getPomTransformer().addIgnoreRule(new DependencyRule("org.openmrs.test openmrs-test * *"));
        solver.getPomTransformer().addIgnoreRule(new DependencyRule("org.openmrs.codehaus.mojo buildnumber-maven-plugin * *"));
        solver.getPomTransformer().addIgnoreRule(new DependencyRule("org.codehaus.mojo build-helper-maven-plugin * *"));
        solver.getPomTransformer().addIgnoreRule(new DependencyRule("org.apache.maven.plugins maven-assembly-plugin * *"));
        solver.getPomTransformer().addIgnoreRule(new DependencyRule("org.springframework * * *"));
        solver.getPomTransformer().addIgnoreRule(new DependencyRule("ca.uhn.hapi hapi * *"));
        solver.getPomTransformer().addIgnoreRule(new DependencyRule("org.openmrs.simpleframework simple-xml * *"));
        solver.getPomTransformer().addIgnoreRule(new DependencyRule("org.openmrs.hibernate * * *"));
        solver.getPomTransformer().addIgnoreRule(new DependencyRule("stax stax* * *"));
        solver.getPomTransformer().addIgnoreRule(new DependencyRule("dom4j dom4j * *"));
        solver.getPomTransformer().addIgnoreRule(new DependencyRule("c3p0 c3p0 * *"));
        solver.getPomTransformer().addIgnoreRule(new DependencyRule("net.sf.ehcache * * *"));
        solver.getPomTransformer().addIgnoreRule(new DependencyRule("javax.mail mail * *"));
        solver.getPomTransformer().addIgnoreRule(new DependencyRule("javax.mail mail * *"));
        solver.getPomTransformer().addIgnoreRule(new DependencyRule("org.openmrs.liquibase * * *"));
        solver.getPomTransformer().addIgnoreRule(new DependencyRule("xml-resolver xml-resolver * *"));
        File listOfPoms = getFileInClasspath("openmrs-api.poms");
        solver.setBaseDir(getFileInClasspath("openmrs/pom.xml").getParentFile());
        solver.setListOfPoms(new File(listOfPoms.getParent(), listOfPoms.getName()));
        solver.setInteractive(false);
        solver.setOffline(true);

        solver.solveDependencies();

        assertTrue("Did not expect any issues", solver.getIssues().isEmpty());

        solver.setBaseDir(testDir);
        solver.setListOfPoms(new File(testDir, "openmrs-api.poms"));

        solver.saveListOfPoms();
        solver.saveMavenRules();
        solver.saveSubstvars();

        assertFileEquals("openmrs-api.poms", "openmrs-api.poms");
        assertFileEquals("openmrs-api.substvars", "openmrs.substvars");
        assertFileEquals("openmrs-api.rules", "maven.rules");
    }

    public void testSolveBuildhelperPluginDependencies() throws Exception {
        useFile("buildhelper-maven-plugin/pom.xml", pomFile);
        DependenciesSolver solver = new DependenciesSolver();
        solver.setMavenRepo(getFileInClasspath("repository/root.dir").getParentFile());
        solver.setOutputDirectory(testDir);
        solver.setExploreProjects(false);
        solver.setPackageName("buildhelper-maven-plugin");
        solver.setPackageType("maven");
        solver.getPomTransformer().addIgnoreRule(new DependencyRule("org.apache.maven.plugins maven-changelog-plugin * * * *"));
        solver.getPomTransformer().addIgnoreRule(new DependencyRule("org.apache.maven.plugins maven-changes-plugin * * * *"));
        solver.getPomTransformer().addIgnoreRule(new DependencyRule("org.apache.maven.plugins maven-checkstyle-plugin * * * *"));
        solver.getPomTransformer().addIgnoreRule(new DependencyRule("org.apache.maven.plugins maven-enforcer-plugin * * * *"));
        solver.getPomTransformer().addIgnoreRule(new DependencyRule("org.apache.maven.plugins maven-project-info-reports-plugin * * * *"));
        File listOfPoms = getFileInClasspath("buildhelper-maven-plugin.poms");
        solver.setBaseDir(getFileInClasspath("buildhelper-maven-plugin/pom.xml").getParentFile());
        solver.setListOfPoms(new File(listOfPoms.getParent(), listOfPoms.getName()));
        solver.setInteractive(false);
        solver.setOffline(true);
        solver.setVerbose(true);

        solver.solveDependencies();

        assertTrue("Did not expect any issues", solver.getIssues().isEmpty());

        solver.setBaseDir(testDir);
        solver.setListOfPoms(new File(testDir, "buildhelper-maven-plugin.poms"));

        solver.saveListOfPoms();
        solver.saveMavenRules();
        solver.saveSubstvars();

        assertFileEquals("buildhelper-maven-plugin.poms", "buildhelper-maven-plugin.poms");
        assertFileEquals("buildhelper-maven-plugin.substvars", "buildhelper-maven-plugin.substvars");
        assertFileEquals("buildhelper-maven-plugin.rules", "maven.rules");
    }

    public void testSolvePlexusCompilerDependencies() throws Exception {
        useFile("plexus-compiler/pom.xml", pomFile);
        DependenciesSolver solver = new DependenciesSolver();
        solver.setMavenRepo(getFileInClasspath("repository/root.dir").getParentFile());
        solver.setOutputDirectory(testDir);
        // libplexus-compiler-java.poms already contains some POMs but we want to discover them all 
        solver.setExploreProjects(true);
        solver.setPackageName("libplexus-compiler-java");
        solver.setPackageType("maven");
        solver.getPomTransformer().addIgnoreRule(new DependencyRule("junit junit jar s/3\\..*/3.x/ * *"));
        solver.getPomTransformer().addIgnoreRule(new DependencyRule("org.codehaus.plexus plexus-compiler-api jar s/1\\..*/1.x/ * *"));
        solver.getPomTransformer().addIgnoreRule(new DependencyRule("org.codehaus.plexus plexus-compiler-aspectj jar s/1\\..*/1.x/ * *"));
        solver.getPomTransformer().addIgnoreRule(new DependencyRule("org.codehaus.plexus plexus-compiler-csharp jar s/1\\..*/1.x/ * *"));
        solver.getPomTransformer().addIgnoreRule(new DependencyRule("org.codehaus.plexus plexus-compiler-eclipse jar s/1\\..*/1.x/ * *"));
        solver.getPomTransformer().addIgnoreRule(new DependencyRule("org.codehaus.plexus plexus-compiler-javac jar s/1\\..*/1.x/ * *"));
        solver.getPomTransformer().addIgnoreRule(new DependencyRule("org.codehaus.plexus plexus-compiler-jikes jar s/1\\..*/1.x/ * *"));
        solver.getPomTransformer().addIgnoreRule(new DependencyRule("org.codehaus.plexus plexus-compiler-manager jar s/1\\..*/1.x/ * *"));
        solver.getPomTransformer().addIgnoreRule(new DependencyRule("org.codehaus.plexus plexus-compiler-test jar s/1\\..*/1.x/ * *"));
        solver.getPomTransformer().addIgnoreRule(new DependencyRule("org.codehaus.plexus plexus-compiler pom s/1\\..*/1.x/ * *"));
        solver.getPomTransformer().addIgnoreRule(new DependencyRule("org.codehaus.plexus plexus-compilers pom s/1\\..*/1.x/ * *"));
        solver.getPomTransformer().addIgnoreRule(new DependencyRule("org.codehaus.plexus plexus-components pom s/1\\..*/1.x/ * *"));
        solver.getPomTransformer().addIgnoreRule(new DependencyRule("org.codehaus.plexus plexus-container-default jar s/1\\.0-alpha.*/1.0-alpha/ * *"));
        solver.getPomTransformer().addIgnoreRule(new DependencyRule("s/org.eclipse.jdt/org.eclipse.jdt.core.compiler/ s/core/ecj/ jar s/.*/debian/ * *"));
        solver.getPomTransformer().addIgnoreRule(new DependencyRule("org.apache.maven.plugins maven-gpg-plugin * * * *"));
        // Ignore those plugins for Ant builds
        solver.getPomTransformer().addIgnoreRule(new DependencyRule("org.codehaus.plexus plexus-component-metadata * * * *"));
        solver.getPomTransformer().addIgnoreRule(new DependencyRule("org.apache.maven maven-artifact-test * * * *"));
        solver.getPomTransformer().addIgnoreRule(new DependencyRule("org.apache.maven.plugins maven-surefire-plugin * * * *"));
        solver.getPomTransformer().addIgnoreRule(new DependencyRule("org.codehaus.plexus plexus-compiler-test * * * *"));

        File listOfPoms = getFileInClasspath("libplexus-compiler-java.poms");
        solver.setBaseDir(getFileInClasspath("plexus-compiler/pom.xml").getParentFile());
        solver.setListOfPoms(new File(listOfPoms.getParent(), listOfPoms.getName()));
        solver.setInteractive(false);
        solver.setOffline(true);
        solver.setRunTests(true);
        solver.setVerbose(true);

        solver.solveDependencies();

        assertTrue("Did not expect any issues", solver.getIssues().isEmpty());

        solver.setBaseDir(testDir);
        solver.setListOfPoms(new File(testDir, "libplexus-compiler-java.poms"));

        solver.saveListOfPoms();
        solver.saveMavenRules();
        solver.saveSubstvars();

        assertFileEquals("libplexus-compiler-java.poms", "libplexus-compiler-java.poms");
        assertFileEquals("libplexus-compiler-java.substvars", "libplexus-compiler-java.substvars");
        assertFileEquals("libplexus-compiler-java.rules", "maven.rules");
    }

    protected void assertFileEquals(String resource, String fileName) throws Exception {
        File file = new File(testDir, fileName);
        assertTrue(file.exists());
        LineNumberReader fileReader = new LineNumberReader(new FileReader(file));
        LineNumberReader refReader = new LineNumberReader(read(resource));

        String ref, test = null;
        boolean skipReadTest = false;
        while (true) {
            if (!skipReadTest) {
                test = fileReader.readLine();

                if (test != null && (test.startsWith("#") || test.trim().isEmpty())) {
                    continue;
                }
            }
            skipReadTest = false;

            ref = refReader.readLine();
            if (ref == null) {
                return;
            }
            if (ref.startsWith("#") || ref.trim().isEmpty()) {
                skipReadTest = true;
                continue;
            }
            assertNotNull("Error in " + fileName + ": expected " + ref.trim() + " but found nothing", test);
            assertEquals("Error in " + fileName, ref.trim(), test.trim());
        }
    }

    protected void useFile(String resource, File file) throws IOException {
        final FileWriter out = new FileWriter(file);
        final Reader in = read(resource);
        IOUtils.copy(in,out);
        in.close();
        out.close();
    }

    protected Reader read(String resource) {
        Reader r = new InputStreamReader(this.getClass().getResourceAsStream("/" + resource));
        openedReaders.add(r);
        return r;
    }

    protected File getFileInClasspath(String resource) {
        if (! resource.startsWith("/")) {
            resource = "/" + resource;
        }
        URL url = this.getClass().getResource(resource);
        File f;
        try {
          f = new File(url.toURI());
        } catch(URISyntaxException e) {
          f = new File(url.getPath());
        }
        return f;
    }

}
