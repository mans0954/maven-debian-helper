package org.debian.maven.plugin;

import java.io.IOException;

/**
 * Install the javadoc jar.
 * 
 * @author Ludovic Claude
 *
 * @goal sysinstall-doc
 */
public class SysInstallDocMojo extends SysInstallMojo
{

  protected String jarName()
  {
    return getArtifactId() + "-javadoc-" + getVersion() + ".jar";
  }

  protected String debianJarName()
  {
    return getArtifactId() + "-javadoc-" + getDebianVersion() + ".jar";
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
