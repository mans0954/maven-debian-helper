/*
 * Copyright 2013 Emmanuel Bourg
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

import java.io.File;

import org.debian.maven.packager.DebianDependency;
import org.junit.Test;

import static org.junit.Assert.*;

public class PackageScannerTest {

    @Test
    public void testFindExistingFile() throws Exception {
        PackageScanner scanner = new PackageScanner(false);
        
        DebianDependency dependency = scanner.searchPkg(new File("/usr/share/java/ant.jar"));
        
        assertNotNull("Package not found", dependency);
        assertEquals("Package", "ant", dependency.getPackageName());
    }

    @Test
    public void testFindNonExistingFile() throws Exception {
        PackageScanner scanner = new PackageScanner(false);
        
        DebianDependency dependency = scanner.searchPkg(new File("/usr/share/java/azertyuiop-123.jar"));
        
        assertNull("Package should be null", dependency);
    }
}
