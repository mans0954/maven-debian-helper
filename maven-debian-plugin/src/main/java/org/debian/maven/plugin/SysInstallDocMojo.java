package org.debian.maven.plugin;

import java.io.IOException;

/*
 * Copyright 2009 Torsten Werner, Ludovic Claude.
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

/**
 * Install the javadoc jar.
 * 
 * @author Ludovic Claude
 *
 * @goal sysinstall-doc
 */
public class SysInstallDocMojo extends SysInstallMojo
{

  protected String destJarName()
  {
    return getDestArtifactId() + "-" + getVersion() + "-javadoc.jar";
  }

  protected String debianJarName()
  {
    return getDestArtifactId() + "-" + getDebianVersion() + "-javadoc.jar";
  }

  /**
   * do the actual work
   */
  protected void runMojo() throws IOException
  {
    initProperties();
    copyJar();
  }

}
