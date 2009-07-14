package org.debian.maven.plugin;

/**
 * Install the javadoc jar files into the debian/ directory
 *
 * @goal install-doc
 *
 * @author Ludovic Claude
 */
public class InstallDocMojo extends SysInstallDocMojo
{

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
    return getDebianDir() + "/" + getDebianPackage();
  }

}
