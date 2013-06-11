/*
 * Copyright 2013 Emmanuel Bourg
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

package org.debian.maven.packager;

import java.util.List;

import junit.framework.TestCase;

/**
 * @author Emmanuel Bourg
 * @version $Revision$, $Date$
 */
public class GenerateDebianFilesMojoTest extends TestCase {

    public void testFormatDescription() {
        GenerateDebianFilesMojo mojo = new GenerateDebianFilesMojo();
        
        String description = "A framework for constructing recognizers, compilers, and translators from grammatical descriptions containing Java, C#, C++, or Python actions.";

        List<String> lines = mojo.formatDescription(description);
        assertNotNull(lines);
        assertEquals("number of lines", 2, lines.size());
        assertEquals("formatted description - line 1", "A framework for constructing recognizers, compilers, and translators from", lines.get(0));
        assertEquals("formatted description - line 2", "grammatical descriptions containing Java, C#, C++, or Python actions.", lines.get(1));
    }
    
    public void testFormatNullDescription() {
        GenerateDebianFilesMojo mojo = new GenerateDebianFilesMojo();
        
        List<String> lines = mojo.formatDescription(null);
        assertNotNull(lines);
        assertEquals("number of lines", 0, lines.size());
    }
}
