package org.debian.maven.plugin;

import java.io.IOException;
import org.apache.maven.bootstrap.util.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * Remove installed files and symlinks from the /usr/share hierarchy
 *
 * @goal sysuninstall
 */
public class SysUninstallMojo extends SysInstallMojo
{
  // ----------------------------------------------------------------------
  // Mojo parameters
  // ----------------------------------------------------------------------

  // ----------------------------------------------------------------------
  // Public methods
  // ----------------------------------------------------------------------

  public void execute() throws MojoExecutionException
  {
    try
    {
      FileUtils.forceDelete(fullRepoPath());
      FileUtils.fileDelete(fullCompatPath());
    }
    catch(IOException e)
    {
      getLog().error("uninstallation failed", e);
      throw new MojoExecutionException("IOException catched");
    }
  }

  // ----------------------------------------------------------------------
  // Private methods
  // ----------------------------------------------------------------------

}
