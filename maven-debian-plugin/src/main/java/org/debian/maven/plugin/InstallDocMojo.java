package org.debian.maven.plugin;

import java.io.File;

/**
 * Install the javadoc jar files into the debian/ directory
 *
 * @goal install-doc
 *
 * @author Ludovic Claude
 */
public class InstallDocMojo extends SysInstallDocMojo
{

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

  /* returns e.g. $CURDIR/debian/libfoobar-java
   */

  protected String packagePath()
  {
      if (useMavenRepoLocal) {
          return mavenRepoLocal.getAbsolutePath();
      } else {
          return getDebianDir() + "/" + getDestPackage();
      }
  }

}
