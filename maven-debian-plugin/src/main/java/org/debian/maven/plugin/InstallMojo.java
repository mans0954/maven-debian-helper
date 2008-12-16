package org.debian.maven.plugin;

import java.io.File;
import java.io.IOException;
import org.apache.maven.bootstrap.util.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * Install pom and jar files into the debian/ directory
 *
 * @goal install
 */
public class InstallMojo
    extends AbstractMojo
{
  // ----------------------------------------------------------------------
  // Mojo parameters
  // ----------------------------------------------------------------------

  /**
   * groupId
   *
   * @parameter expression="${project.groupId}"
   * @required
   * @readonly
   */
  private String groupId;

  /**
   * artifactId
   *
   * @parameter expression="${project.artifactId}"
   * @required
   * @readonly
   */
  private String artifactId;

  /**
   * version
   *
   * @parameter expression="${project.version}"
   * @required
   * @readonly
   */
  private String version;

  /**
   * directory where the current pom.xml can be found
   *
   * @parameter expression="${project.build.directory}"
   * @required
   * @readonly
   */
  private String buildDir;

  /**
   * directory of the jar file
   *
   * @parameter expression="${project.build.directory}"
   * @required
   * @readonly
   */
  private String jarDir;

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

  public void execute() throws MojoExecutionException
  {
    try
    {
      copyPom();
      copyAndSymlinkJar();
    }
    catch(IOException e)
    {
      getLog().error("installation failed", e);
      throw new MojoExecutionException("IOException catched");
    }
  }

  // ----------------------------------------------------------------------
  // Private methods
  // ----------------------------------------------------------------------

  /* returns e.g. $CURDIR/debian/libfoobar-java
   */

  private String packagePath()
  {
    return debianDir + "/" + debianPackage;
  }

  /* returns e.g. /org/debian/maven/maven-debian-plugin/0.1/
   */

  private String repoPath()
  {
    return "/" + groupId.replace( '.', '/' ) + "/" + artifactId + "/" + version + "/";
  }

  /* absolute path to destination dir
   */

  private String fullRepoPath()
  {
    return packagePath() + "/usr/share/maven-repo" + repoPath();
  }

  private String pomName()
  {
    return artifactId + "-" + version + ".pom";
  }

  private String pomDir()
  {
    return buildDir.replaceFirst("[^/]*$", "");
  }

  private String pomSrcPath()
  {
    return pomDir() + "/pom.xml";
  }

  private String pomDestPath()
  {
    return fullRepoPath() + pomName();
  }

  private String jarName()
  {
    return artifactId + "-" + version + ".jar";
  }

  private String fullJarName()
  {
    return jarDir + "/" + jarName();
  }

  private String jarDestPath()
  {
    return fullRepoPath() + jarName();
  }

  /* jar file name without version number
   */

  private String compatName()
  {
    return artifactId + ".jar";
  }

  private String compatSharePath()
  {
    return packagePath() + "/usr/share/java/";
  }

  private String compatRelPath()
  {
    return "../maven-repo" + repoPath() + jarName();
  }

  /* command for creating the relative symlink
   */

  private String[] linkCommand()
  {
    String[] command = {"ln", "-s", compatRelPath(), compatSharePath() + compatName()};
    return command;
  }

  /* copy the pom.xml
   */

  private void copyPom() throws IOException
  {
    FileUtils.copyFile(new File(pomSrcPath()), new File(pomDestPath()));
  }

  /* if a jar exists: copy it and symlink it to the compat share dir
   */

  private void copyAndSymlinkJar() throws IOException
  {
    File jarFile = new File(fullJarName());
    if (jarFile.exists())
    {
      FileUtils.copyFile(jarFile, new File(jarDestPath()));
      new File(compatSharePath()).mkdirs();
      Runtime.getRuntime().exec(linkCommand(), null);
    }
  }
}
