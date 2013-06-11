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

public class LicenseCheckResult implements OutputHandler {

    private final Set<String> licenses = new TreeSet<String>();
    private final Set<String> copyrightOwners = new TreeSet<String>();

    public void newLine(String line) {
        if (line.startsWith(".") && line.indexOf(":") > 0) {
            int col = line.lastIndexOf(":");
            String license = line.substring(col + 1).trim();
            if (license.contains("UNKNOWN")) {
                return;
            }
            if (license.contains("*")) {
                license = license.substring(license.lastIndexOf("*") + 1).trim();
            }
            licenses.add(license);
        }
    }

    public void failure() {
    }
    
    public Set<String> getLicenses() {
        return licenses;
    }

    public Set<String> getCopyrightOwners() {
        return copyrightOwners;
    }
}
