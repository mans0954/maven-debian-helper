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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GetPackageContainingPatternResult implements OutputHandler {
    private final String extension;
    private final Map<String, String> result = new HashMap<String, String>();

    public GetPackageContainingPatternResult(String extension) {
        this.extension = extension;
    }

    public void newLine(String line) {
        // Clean up lines of the form <pkg>: <file>
        int colon = line.indexOf(':');
        if (colon > 0 && line.indexOf(' ') > colon) {
            String candidatePkg = line.substring(0, colon);
            // Ignore lines such as 'dpkg : xxx'
            if (candidatePkg.equals(candidatePkg.trim()) && !candidatePkg.startsWith("dpkg")) {
                String match = matchFile(line.substring(colon + 1).trim(), candidatePkg);
                if (match != null) {
                    result.put(match, candidatePkg);
                }
            }
        }
    }

    protected String matchFile(String potentialMatch, String candidatePkg) {
        if (potentialMatch.endsWith(extension)) {
          System.out.println("Found " + potentialMatch + " in " + candidatePkg);
          return potentialMatch;
        } else {
          return null;
        }
    }

    public void failure() {
    }

    public Map<String, String> getPackagesAndFiles() {
        return result;
    }

    public Set<String> getPackages() {
        return new HashSet<String>(result.values());
    }
}
