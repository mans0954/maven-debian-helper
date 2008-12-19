package org.debian.maven.plugin;

/**
 * Install pom and jar files into the /usr/share hierarchy
 *
 * @goal sysinstall
 */
public class SysInstallMojo extends AbstractInstallMojo
{
  // ----------------------------------------------------------------------
  // Mojo parameters
  // ----------------------------------------------------------------------

  // ----------------------------------------------------------------------
  // Public methods
  // ----------------------------------------------------------------------

  // ----------------------------------------------------------------------
  // Private methods
  // ----------------------------------------------------------------------

  /* returns empty prefix
   */

  protected String packagePath()
  {
    return "";
  }
}
