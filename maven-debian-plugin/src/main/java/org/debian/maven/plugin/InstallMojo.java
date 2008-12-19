package org.debian.maven.plugin;

/**
 * Install pom and jar files into the debian/ directory
 *
 * @goal install
 */
public class InstallMojo extends SysInstallMojo
{
  // ----------------------------------------------------------------------
  // Mojo parameters
  // ----------------------------------------------------------------------

  /**
   * $(CURDIR)/debian - must be supplied because $(CURDIR) is not known
   *
   * @parameter expression="${debian.dir}"
   * @required
   */
  private String debianDir;

  /**
   * name of the debian binary package, e.g. libfoobar-java
   *
   * @parameter expression="${debian.package}"
   * @required
   */
  private String debianPackage;

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
    return debianDir + "/" + debianPackage;
  }
}
