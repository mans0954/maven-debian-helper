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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GetChangelogVersionResult implements OutputHandler {

    private String result;
    private final Pattern pattern;

    public GetChangelogVersionResult(String pkg) {
        this.pattern = Pattern.compile(pkg + "\\s\\(.*\\)");
    }

    public void newLine(String line) {
        if (result != null) {
            return;
        }
        Matcher match = pattern.matcher(line);
        if (match.find()) {
            result = match.group(1);
        }
    }

    public void failure() {
    }

    public String getResult() {
        return result;
    }
}
