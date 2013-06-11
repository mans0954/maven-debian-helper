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

import java.util.List;

public class SharedJarOutputHandler implements OutputHandler {
    private final List<String> jars;

    public SharedJarOutputHandler(List<String> jars) {
        this.jars = jars;
    }

    public void newLine(String line) {
        if (line.startsWith("/usr/share/java/") && line.endsWith(".jar")) {
            String jar = line.substring("/usr/share/java/".length());
            jar = jar.substring(0, jar.length() - 4);
            if (!line.matches(".*/.*-\\d.*")) {
                jars.add(jar);
                System.out.println("  Add " + jar + " to the classpath");
            }
        }
    }

    public void failure() {
    }
}
