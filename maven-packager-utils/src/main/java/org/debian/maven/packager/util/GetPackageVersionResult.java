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

public class GetPackageVersionResult implements OutputHandler {

    private static final Pattern APT_VERSION_PATTERN = Pattern.compile("^\\s+.*\\s\\((.+)-.+?\\)$");
    private String result;

    public void newLine(String line) {
        if (result != null) {
            return;
        }
        if (line.startsWith("Version:")) {
            int space = line.indexOf(' ');
            result = line.substring(space + 1, line.length()).trim();
            int dash = result.lastIndexOf('-');
            if (dash > 0) {
                result = result.substring(0, dash);
            }
            result = result.replace('~', '-');
        } else {
            Matcher matcher = APT_VERSION_PATTERN.matcher(line);
            if (matcher.find()) {
                result = matcher.group(1);
                result = result.replace('~', '-');
            }
        }
    }

    public void failure() {
    }

    public String getResult() {
        return result;
    }
}
