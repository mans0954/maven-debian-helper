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

import java.util.Set;
import java.util.TreeSet;

/**
 * Parses the output of the <tt>dpkg --search</tt> and <tt>apt-file find</tt>
 * commands and extracts the name of the packages matching the search criteria.
 * <p>
 * The format expected is:
 * <pre>
 *     &lt;pkg>: &lt;file>
 * </pre>
 */
public class GetPackageResult implements OutputHandler {

    private final Set<String> result = new TreeSet<String>();

    public void newLine(String line) {
        // Clean up lines of the form <pkg>: <file>
        int colon = line.indexOf(':');
        if (colon > 0 && line.indexOf(' ') > colon) {
            String candidatePkg = line.substring(0, colon);
            // Ignore lines such as 'dpkg : xxx'
            if (candidatePkg.equals(candidatePkg.trim()) && !candidatePkg.startsWith("dpkg")) {
                System.out.println("Found " + candidatePkg);
                result.add(candidatePkg);
            }
        }
    }

    public void failure() {
    }

    public Set<String> getResult() {
        return result;
    }

}
