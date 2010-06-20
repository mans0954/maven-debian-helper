package org.debian.maven.plugin;

import java.io.File;

/**
 * Install pom and jar files into the debian/ directory
 *
 * @goal install
 */
public class InstallMojo extends SysInstallMojo
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
        return getDebianDir() + "/" + getDebianPackage();
    }
  }

    /**
     * absolute path to destination dir
     */
    protected String fullRepoPath()
    {
        if (useMavenRepoLocal) {
            return packagePath() + destRepoPath();
        } else {
            return super.fullRepoPath();
        }
    }

    /**
     * absolute path to destination dir
     */
    protected String debianFullRepoPath()
    {
        if (useMavenRepoLocal) {
            return packagePath() + debianRepoPath();
        } else {
            return super.debianFullRepoPath();
        }
    }


}
