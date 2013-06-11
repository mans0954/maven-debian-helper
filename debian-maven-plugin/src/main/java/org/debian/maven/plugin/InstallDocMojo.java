/*
 * Copyright 2009 Torsten Werner.
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

import java.io.File;

/**
 * Install the javadoc jar files into the debian/ directory
 *
 * @author Ludovic Claude
 * @goal install-doc
 */
public class InstallDocMojo extends SysInstallDocMojo {

    /**
     * Maven repository root
     *
     * @parameter expression="${maven.repo.local}"
     */
    private File mavenRepoLocal;

    /**
     * If true, use local Maven repository for installation
     *
     * @parameter expression="${use.maven.repo.local}"
     */
    private boolean useMavenRepoLocal;

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    // ----------------------------------------------------------------------
    // Private methods
    // ----------------------------------------------------------------------

    /**
     * Returns e.g. $CURDIR/debian/libfoobar-java
     */
    protected String packagePath() {
        if (useMavenRepoLocal) {
            return mavenRepoLocal.getAbsolutePath();
        } else {
            return getDebianDir() + "/" + getDestPackage();
        }
    }

}
