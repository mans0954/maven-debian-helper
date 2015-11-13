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

package org.debian.maven.packager.util;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class GetPackageResultTest {

    private GetPackageResult result = new GetPackageResult();

    @Test
    public void testFilterDpkgOutputFindCommonsIoPom() throws Exception {
        //dpkg --search /usr/share/maven-repo/commons-io/commons-io/1.4/commons-io-1.4.pom
        List<String> dpkgOut = new ArrayList<String>();
        dpkgOut.add("libcommons-io-java: /usr/share/maven-repo/commons-io/commons-io/1.4/commons-io-1.4.pom");

        for (String line : dpkgOut) {
            result.newLine(line);
        }
        assertEquals(1, result.getResult().size());
        assertEquals("libcommons-io-java", result.getResult().iterator().next());
    }

    @Test
    public void testFilterDpkgOutputFindAnyJavaccMavenPlugin() throws Exception {
        //dpkg --search /usr/share/maven-repo/org/codehaus/mojo/javacc-maven-plugin/*/*
        List<String> dpkgOut = new ArrayList<String>();
        dpkgOut.add("libjavacc-maven-plugin-java: /usr/share/maven-repo/org/codehaus/mojo/javacc-maven-plugin/2.6/javacc-maven-plugin-2.6.jar");
        dpkgOut.add("libjavacc-maven-plugin-java: /usr/share/maven-repo/org/codehaus/mojo/javacc-maven-plugin/2.6/javacc-maven-plugin-2.6.pom");

        for (String line : dpkgOut) {
            result.newLine(line);
        }
        assertEquals(1, result.getResult().size());
        assertEquals("libjavacc-maven-plugin-java", result.getResult().iterator().next());
    }

    @Test
    public void testFilterAptFileOutput() throws Exception {
        // apt-file search /usr/share/maven-repo/org/codehaus/mojo/javacc-maven-plugin/
        List<String> dpkgOut = new ArrayList<String>();
        dpkgOut.add("libjavacc-maven-plugin-java: /usr/share/maven-repo/org/codehaus/mojo/javacc-maven-plugin/2.6/javacc-maven-plugin-2.6.jar");
        dpkgOut.add("libjavacc-maven-plugin-java: /usr/share/maven-repo/org/codehaus/mojo/javacc-maven-plugin/2.6/javacc-maven-plugin-2.6.pom");

        for (String line : dpkgOut) {
            result.newLine(line);
        }
        assertEquals(1, result.getResult().size());
        assertEquals("libjavacc-maven-plugin-java", result.getResult().iterator().next());
    }

}
