package org.debian.maven.plugin;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.codehaus.plexus.util.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.debian.maven.repo.POMCleaner;
import org.debian.maven.repo.POMTransformer;

/**
 * Install pom and jar files into the /usr/share/hierarchy
 *
 * @goal sysinstall
 */
public class SysInstallMojo extends AbstractMojo
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
   * destGroupId
   * 
   * @parameter expression="${project.groupId}"
   * @required
   */
  private String destGroupId;

  /**
   * destArtifactId
   * 
   * @parameter expression="${project.artifactId}"
   * @required
   */
  private String destArtifactId;

  /**
   * version
   *
   * @parameter expression="${project.version}"
   * @required
   * @readonly
   */
  private String version;

  /**
   * debianVersion
   *
   * @parameter
   */
  private String debianVersion;

  /**
   * directory where the current pom.xml can be found
   *
   * @parameter expression="${basedir}"
   * @required
   * @readonly
   */
  private File basedir;

  /**
   * directory of the jar file
   *
   * @parameter expression="${project.build.directory}"
   * @required
   * @readonly
   */
  private String jarDir;

  /**
   * Debian directory
   *
   * @parameter expression="${debian.dir}"
   */
  private File debianDir;

  /**
   * Debian package
   *
   * @parameter expression="${debian.package}"
   */
  private String debianPackage;

  /**
   * @parameter expression="${maven.rules}" default-value="maven.rules"
   * @required
   */
  private String mavenRules;

  /**
   * root directory of the Maven repository
   *
   * @parameter expression="${basedir}"
   * @readonly
   */
  private File repoDir;

  // ----------------------------------------------------------------------
  // Public methods
  // ----------------------------------------------------------------------

  public void execute() throws MojoExecutionException
  {
    try
    {
      runMojo();
    }
    catch(IOException e)
    {
      getLog().error("execution failed", e);
      throw new MojoExecutionException("IOException catched");
    }
  }

  // ----------------------------------------------------------------------
  // Private methods
  // ----------------------------------------------------------------------

  protected String getArtifactId()
  {
      return artifactId;
  }

  protected String getVersion()
  {
      return version;
  }

  protected String getDebianVersion()
  {
      return debianVersion;
  }

  protected File getDebianDir()
  {
      return debianDir;
  }

  protected String getDebianPackage()
  {
      return debianPackage;
  }

  /**
   * optional destination prefix, empty by default
   */
  protected String packagePath()
  {
    return "";
  }

  /**
   * returns e.g. /org/debian/maven/maven-debian-plugin/0.1/
   */
  private String repoPath()
  {
    return "/" + groupId.replace( '.', '/' ) + "/" + artifactId + "/" + version + "/";
  }
  
  /**
   * returns e.g. /org/debian/maven/maven-debian-plugin/0.1/
   */
  private String destRepoPath()
  {
    return "/" + destGroupId.replace( '.', '/' ) + "/" + destArtifactId + "/" + version + "/";
  }

  /**
   * returns e.g. /org/debian/maven/maven-debian-plugin/debian/
   */
  private String debianRepoPath()
  {
    return "/" + destGroupId.replace( '.', '/' ) + "/" + destArtifactId + "/" + debianVersion + "/";
  }

  /**
   * absolute path to destination dir
   */
  protected String fullRepoPath()
  {
    return packagePath() + "/usr/share/maven-repo" + destRepoPath();
  }

  /**
   * absolute path to destination dir
   */
  protected String debianFullRepoPath()
  {
    return packagePath() + "/usr/share/maven-repo" + debianRepoPath();
  }

  protected String pomName()
  {
    return artifactId + "-" + version + ".pom";
  }
  
  protected String destPomName()
  {
    return destArtifactId + "-" + version + ".pom";
  }

  protected String debianPomName()
  {
    return destArtifactId + "-" + debianVersion + ".pom";
  }

  private String pomSrcPath()
  {
    return basedir.getAbsolutePath() + "/pom.xml";
  }

  private String cleanedPomSrcPath()
  {
    return basedir.getAbsolutePath() + "/target/pom.xml";
  }

  private String cleanedPomPropertiesSrcPath()
  {
    return basedir.getAbsolutePath() + "/target/pom.properties";
  }

  private String debianPomSrcPath()
  {
    return basedir.getAbsolutePath() + "/target/pom.debian.xml";
  }

  private String debianPomPropertiesSrcPath()
  {
    return basedir.getAbsolutePath() + "/target/pom.debian.properties";
  }

  private String pomDestPath()
  {
    return fullRepoPath() + destPomName();
  }

  private String debianPomDestPath()
  {
    return debianFullRepoPath() + debianPomName();
  }

  protected String jarName()
  {
    return artifactId + "-" + version + ".jar";
  }
  
  protected String destJarName()
  {
    return destArtifactId + "-" + version + ".jar";
  }

  protected String debianJarName()
  {
    return destArtifactId + "-" + debianVersion + ".jar";
  }

  private String fullJarName()
  {
    return jarDir + "/" + jarName();
  }

  private String jarDestPath()
  {
    return fullRepoPath() + destJarName();
  }

  private String jarDestRelPath()
  {
    return "../" + version + "/" + destJarName();
  }

  private String debianJarDestPath()
  {
    return debianFullRepoPath() + debianJarName();
  }

  /** 
   * jar file name without version number
   */
  private String compatName()
  {
    return destArtifactId + ".jar";
  }

  private String compatSharePath()
  {
    return packagePath() + "/usr/share/java/";
  }

  private String compatRelPath()
  {
    return "../maven-repo" + destRepoPath() + destJarName();
  }

  protected String fullCompatPath()
  {
    return compatSharePath() + compatName();
  }

  protected String versionedFullCompatPath()
  {
    return compatSharePath() + destJarName();
  }

  /**
   * command for creating the relative symlink
   */
  private String[] linkCommand(String source, String dest)
  {
    String[] command = {"ln", "-s", source, dest};
    return command;
  }

  private void mkdir(String path) throws IOException
  {
    File destinationDirectory = new File(path);
    if (destinationDirectory.isDirectory())
    {
      return;
    }
    if (!destinationDirectory.mkdirs())
    {
      throw new IOException("cannot create destination directory " + path);
    }
  }

  private void run(String[] command) throws IOException
  {
    Runtime.getRuntime().exec(command, null);
  }

  /**
   * if a jar exists: copy it to the Maven repository
   */
  protected void copyJar() throws IOException
  {
    File jarFile = new File(fullJarName());
    if (jarFile.exists())
    {
      FileUtils.copyFile(jarFile, new File(jarDestPath()));
      if (debianVersion != null && !debianVersion.equals(version))
      {
        mkdir(debianFullRepoPath());
        run(linkCommand(jarDestRelPath(), debianJarDestPath()));
      }
    }
  }

  /**
   * if a jar exists: symlink it to the compat share dir
   */
  private void symlinkJar() throws IOException
  {
    File jarFile = new File(fullJarName());
    if (jarFile.exists())
    {
      mkdir(compatSharePath());
      run(linkCommand(compatRelPath(), fullCompatPath()));
      run(linkCommand(compatRelPath(), versionedFullCompatPath()));
    }
  }

  /**
   * clean the pom.xml
   */
  private void cleanPom()
  {
    File pomOptionsFile = new File(debianDir, debianPackage + ".poms");
    Map pomOptions = POMTransformer.getPomOptions(pomOptionsFile);
    // Use the saved pom before cleaning as it was untouched by the transform operation
    File pom = new File(pomSrcPath() + ".save");
    File originalPom = new File(pomSrcPath()).getAbsoluteFile();
    if (! pom.exists())
    {
        pom = originalPom;
    }

    String pomOption = (String) pomOptions.get(originalPom);

    List params = new ArrayList();
    params.add("--keep-pom-version");
    params.add("--package=" + debianPackage);
    String mavenRulesPath = new File(debianDir, mavenRules).getAbsolutePath();
    params.add("--rules=" + mavenRulesPath);

    System.out.println("Cleaning pom file: " + pom + " with options:");
    System.out.println("\t--keep-pom-version --package=" + debianPackage);
    System.out.println("\t--rules=" + mavenRulesPath);

    // add optional --no-parent option
    if (pomOption != null && !pomOption.isEmpty()) {
        params.add(pomOption);
        System.out.println("\t" + pomOption);
    }
    
    params.add(pom.getAbsolutePath());
    params.add(cleanedPomSrcPath());
    params.add(cleanedPomPropertiesSrcPath());

    POMCleaner.main((String[]) params.toArray(new String[params.size()]));

    Properties pomProperties = new Properties();
    try {
      pomProperties.load(new FileReader(cleanedPomPropertiesSrcPath()));
    } catch (IOException ex) {
      ex.printStackTrace();
    }
    destGroupId = pomProperties.getProperty("groupId");
    destArtifactId = pomProperties.getProperty("artifactId");
    debianVersion = pomProperties.getProperty("debianVersion");

    if (debianVersion != null && !debianVersion.equals(version))
    {
      params.remove(0);
      params.remove(params.size() -1);
      params.remove(params.size() -1);
      params.add(debianPomSrcPath());
      params.add(debianPomPropertiesSrcPath());

      POMCleaner.main((String[]) params.toArray(new String[params.size()]));
    }
  }
 
  /**
   * copy the pom.xml
   */
  protected void copyPom() throws IOException
  {
    FileUtils.copyFile(new File(cleanedPomSrcPath()), new File(pomDestPath()));
    if (debianVersion != null && !debianVersion.equals(version))
    {
      FileUtils.copyFile(new File(debianPomSrcPath()), new File(debianPomDestPath()));
    }
  }

  /**
   * Initialize some properties which don't seem to be set automatically
   * by Maven Mojo mechanism.
   */
  protected void initProperties()
  {
    if (debianDir == null)
    {
      debianDir = new File(System.getProperty("debian.dir"));
    }
    if (debianPackage == null)
    {
      debianPackage = System.getProperty("debian.package");
    }
    if (repoDir == null)
    {
      repoDir = new File(System.getProperty("maven.repo.local"));
    }
  }

  /**
   * do the actual work
   */
  protected void runMojo() throws IOException
  {
    //initProperties();
    cleanPom();
    copyPom();
    copyJar();
    symlinkJar();
  }
}
