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

public class GetPackageContainingPatternResultTest {

    private GetPackageContainingPatternResult result;

    @Test
    public void testFilterDpkgOutput() throws Exception {
        // dpkg --search /usr/share/maven-repo/commons-lang/commons-lang/*/*
        List<String> dpkgOut = new ArrayList<String>();

        dpkgOut.add("libcommons-lang-java: /usr/share/maven-repo/commons-lang/commons-lang/2.6/commons-lang-2.6.jar");
        dpkgOut.add("libcommons-lang-java-doc: /usr/share/maven-repo/commons-lang/commons-lang/2.6/commons-lang-2.6-javadoc.jar");
        dpkgOut.add("libcommons-lang-java: /usr/share/maven-repo/commons-lang/commons-lang/2.6/commons-lang-2.6.pom");
        dpkgOut.add("libcommons-lang-java: /usr/share/maven-repo/commons-lang/commons-lang/debian/commons-lang-debian.jar");
        dpkgOut.add("libcommons-lang-java-doc: /usr/share/maven-repo/commons-lang/commons-lang/debian/commons-lang-debian-javadoc.jar");
        dpkgOut.add("libcommons-lang-java: /usr/share/maven-repo/commons-lang/commons-lang/debian/commons-lang-debian.pom");

        result = new GetPackageContainingPatternResult(".pom");
        for (String line : dpkgOut) {
            result.newLine(line);
        }

        assertEquals(2, result.getPackagesAndFiles().size());
        assertEquals("libcommons-lang-java", result.getPackagesAndFiles().get("/usr/share/maven-repo/commons-lang/commons-lang/2.6/commons-lang-2.6.pom"));
        assertEquals("libcommons-lang-java", result.getPackagesAndFiles().get("/usr/share/maven-repo/commons-lang/commons-lang/debian/commons-lang-debian.pom"));
        assertEquals("libcommons-lang-java", result.getPackages().iterator().next());
    }

    @Test
    public void testFilterAptFileOutput() throws Exception {
        // apt-file search /usr/share/maven-repo/commons-lang/commons-lang
        List<String> dpkgOut = new ArrayList<String>();

        dpkgOut.add("libcommons-lang-java: /usr/share/maven-repo/commons-lang/commons-lang/2.4/commons-lang-2.4.jar");
        dpkgOut.add("libcommons-lang-java: /usr/share/maven-repo/commons-lang/commons-lang/2.4/commons-lang-2.4.pom");
        dpkgOut.add("libcommons-lang-java: /usr/share/maven-repo/commons-lang/commons-lang/debian/commons-lang-debian.jar");
        dpkgOut.add("libcommons-lang-java: /usr/share/maven-repo/commons-lang/commons-lang/debian/commons-lang-debian.pom");

        result = new GetPackageContainingPatternResult(".pom");
        for (String line : dpkgOut) {
            result.newLine(line);
        }

        assertEquals("libcommons-lang-java", result.getPackagesAndFiles().get("/usr/share/maven-repo/commons-lang/commons-lang/2.4/commons-lang-2.4.pom"));
        assertEquals("libcommons-lang-java", result.getPackagesAndFiles().get("/usr/share/maven-repo/commons-lang/commons-lang/debian/commons-lang-debian.pom"));
        assertEquals("libcommons-lang-java", result.getPackages().iterator().next());
    }

}
