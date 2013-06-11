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

import junit.framework.TestCase;

import java.util.HashSet;
import java.util.Set;

public class LicensesScannerTest extends TestCase {

    public void testRecognizeLicense() throws Exception {
        LicensesScanner scanner = new LicensesScanner();

        Set<String> licenses = new HashSet<String>();
        assertTrue(scanner.recognizeLicense(licenses, "Apache (v2.0)", ""));
        assertEquals("Apache-2.0", licenses.iterator().next());
        licenses.clear();

        assertTrue(scanner.recognizeLicense(licenses, "", "http://www.apache.org/licenses/LICENSE-2.0"));
        assertEquals("Apache-2.0", licenses.iterator().next());
        licenses.clear();
    }
}
