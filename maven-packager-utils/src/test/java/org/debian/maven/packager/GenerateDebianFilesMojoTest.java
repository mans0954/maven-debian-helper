package org.debian.maven.packager;

import junit.framework.TestCase;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: ludo
 * Date: Nov 18, 2010
 * Time: 11:56:33 PM
 * To change this template use File | Settings | File Templates.
 */
public class GenerateDebianFilesMojoTest extends TestCase {

    public void testRecognizeLicense() throws Exception {
        GenerateDebianFilesMojo mojo = new GenerateDebianFilesMojo();

        Set licenses = new HashSet();
        assertTrue(mojo.recognizeLicense(licenses, "Apache (v2.0)", ""));
        assertEquals("Apache-2.0", licenses.iterator().next());
        licenses.clear();

        assertTrue(mojo.recognizeLicense(licenses, "", "http://www.apache.org/licenses/LICENSE-2.0"));
        assertEquals("Apache-2.0", licenses.iterator().next());
        licenses.clear();
                
    }
}
