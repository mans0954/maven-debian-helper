/*
 * Copyright 2011 Ludovic Claude.
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

package org.debian.maven.plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

class DirectoryUtils {

    /**
     * Returns the relative path to a file relatively to a source directory.
     * The path returned can be used to create a relative symbolic link
     * in the source directory to the target file.
     * 
     * @param absSrcDir      the absolute path of the source directory (e.g. /usr/share/java)
     * @param absTargetPath  the absolute path of the target file (e.g. /usr/share/maven-repo/foo/foo.jar)
     */
    public static String relativePath(String absSrcDir, String absTargetPath) {
        List<String> src = splitDirs(absSrcDir);
        List<String> target = splitDirs(absTargetPath);
        int common = 0;
        while (common < src.size() && common < target.size() && src.get(common).equals(target.get(common))) {
            common++;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = common; i < src.size(); i++) {
            sb.append("../");
        }
        for (int j = common; j < target.size(); j++) {
            sb.append(target.get(j));
            if (j + 1 < target.size()) {
                sb.append("/");
            }
        }
        return sb.toString();
    }

    private static List<String> splitDirs(String path) {
        List<String> result = new ArrayList<String>();
        StringTokenizer st = new StringTokenizer(path, "/", false);
        while (st.hasMoreTokens()) {
            result.add(st.nextToken());
        }
        return result;
    }
}
