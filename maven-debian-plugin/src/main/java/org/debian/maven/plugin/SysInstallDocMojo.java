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
