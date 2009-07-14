package org.debian.maven.plugin;

import java.io.IOException;
import org.codehaus.plexus.util.FileUtils;

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

  // ----------------------------------------------------------------------
  // Private methods
  // ----------------------------------------------------------------------

  protected void runMojo() throws IOException
  {
    FileUtils.forceDelete(fullRepoPath());
    FileUtils.fileDelete(fullCompatPath());
  }
}
