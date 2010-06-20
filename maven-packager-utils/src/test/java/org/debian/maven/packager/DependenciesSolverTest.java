/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.debian.maven.packager;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

/**
 *
 * @author ludo
 */
public class DependenciesSolverTest extends TestCase {

    private File testDir = new File("tmp");
    private File pomFile = new File(testDir, "pom.xml");
    private List openedReaders = new ArrayList();

    protected void setUp() throws Exception {
        super.setUp();
        testDir.mkdir();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        for (Iterator i = openedReaders.iterator(); i.hasNext(); ) {
            Reader reader = (Reader) i.next();
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
    public void testSolveDependencies() throws Exception {
        useFile("plexus-active-collections.pom", pomFile);
        DependenciesSolver solver = new DependenciesSolver();
        solver.setBaseDir(testDir);
        solver.setOutputDirectory(testDir);
        solver.setExploreProjects(true);
        solver.setPackageName("libplexus-active-collections-java");
        solver.setPackageType("maven");
        solver.setListOfPoms(new File(testDir, "libplexus-active-collections-java.poms"));
        solver.setNonInteractive(true);

        solver.solveDependencies();

        assertTrue("Did not expect any issues", solver.getIssues().isEmpty());

        solver.saveListOfPoms();
        solver.saveMavenRules();
        solver.saveSubstvars();

        assertFileEquals("libplexus-active-collections-java.poms", "libplexus-active-collections-java.poms");
        assertFileEquals("libplexus-active-collections-java.substvars", "libplexus-active-collections-java.substvars");
        assertFileEquals("libplexus-active-collections-java.rules", "maven.rules");
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

                if (test != null && test.startsWith("#")) {
                    continue;
                }
            }
            skipReadTest = false;

            ref = refReader.readLine();
            if (ref == null) {
                return;
            }
            if (ref.startsWith("#")) {
                skipReadTest = true;
                continue;
            }
            assertEquals("Error in " + fileName, ref, test);
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

}
