package org.debian.maven.plugin;

/*
 * Copyright 2009 Torsten Werner, Ludovic Claude.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.FileUtils;
import org.debian.maven.repo.ListOfPOMs;
import org.debian.maven.repo.POMCleaner;

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
   * finalname of the artifact
   *
   * @parameter expression="${project.build.finalName}"
   * @required
   * @readonly
   */
  private String finalName;

  /**
   * Debian directory
   *
   * @parameter expression="${debian.dir}"
   */
  private File debianDir;

  /**
   * Debian package (send from commande line)
   *
   * @parameter expression="${debian.package}"
   */
  private String debianPackage;
  
  /**
   * Debian package destination (set by xxx.poms file).
   * By defaul, equals to <code>debianPackage</code> attribute.
   *
   * @parameter expression="${debian.package}"
   */
  private String destPackage;

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

    /**
     * Install the jar to /usr/share/java if true. Default is true
     * @parameter expression="${install.to.usj}" default-value="true"
     */
  private boolean installToUsj = true;

  /**
   * Basename of the JAR inside /usr/share/java
   */
	private String usjName;

	/**
	 * Version of the JAR install /usr/share/java
	 */
    private String usjVersion;

    /**
     * If true, disable installation of version-less JAR into /usr/share/java
     */
	private boolean noUsjVersionless;

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
  
  protected String getDestArtifactId()
  {
      return destArtifactId;
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

  protected String getDestPackage()
  {
      return destPackage;    
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
  protected final String repoPath()
  {
    return "/" + groupId.replace( '.', '/' ) + "/" + artifactId + "/" + version + "/";
  }
  
  /**
   * returns e.g. /org/debian/maven/maven-debian-plugin/0.1/
   */
  protected final String destRepoPath()
  {
    return "/" + destGroupId.replace( '.', '/' ) + "/" + destArtifactId + "/" + version + "/";
  }

  /**
   * returns e.g. /org/debian/maven/maven-debian-plugin/debian/
   */
  protected final String debianRepoPath()
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
	  String jarName = "";
	  if (finalName != null && finalName.length() > 0)
	  {
		  jarName += finalName;
	  }
	  else
	  {
		  jarName += artifactId + "-" + version;
	  }
    return jarName + ".jar";
  }
  
  protected String destJarName()
  {
    return destArtifactId + "-" + version + ".jar";
  }

  protected String debianJarName()
  {
    return destArtifactId + "-" + debianVersion + ".jar";
  }

  protected final String fullJarName()
  {
    return jarDir + "/" + jarName();
  }

  protected final String jarDestPath()
  {
    return fullRepoPath() + destJarName();
  }

  protected final String jarDestRelPath()
  {
    return "../" + version + "/" + destJarName();
  }

  protected final String debianJarDestPath()
  {
    return debianFullRepoPath() + debianJarName();
  }

  /** 
   * jar file name without version number
   */
  protected final String compatName()
  {
    return destArtifactId + ".jar";
  }

  protected final String compatSharePath()
  {
    return packagePath() + "/usr/share/java/";
  }

  protected final String compatRelPath()
  {
    return "../maven-repo" + destRepoPath() + destJarName();
  }

  /**
   * Example: /usr/share/java/xml-apis.jar
   */
  protected String fullCompatPath()
  {
    return compatSharePath() + destUsjJarName();
  }

  /**
   * Example: /usr/share/java/xml-apis-1.3.04.jar
   */
  protected String versionedFullCompatPath()
  {
    return compatSharePath() + destUsjVersionnedJarName();
  }
  
  /**
   * Compute version-less filename for installation into /usr/share/java
   */
  private String destUsjJarName() {
	  String usjJarName = "";
	  if (usjName != null && usjName.length() > 0)
	  {
		  usjJarName += usjName;
	  }
	  else
	  {
		  usjJarName += destArtifactId;
	  }
		  
	  return usjJarName + ".jar";
  }

  /**
   * Compute versionned filename for installation into /usr/share/java
   */
  private String destUsjVersionnedJarName() {
	  String usjJarName = "";
	  if (usjName != null && usjName.length() > 0)
	  {
		  usjJarName += usjName;
	  }
	  else
	  {
		  usjJarName += destArtifactId;
	  }
	  
	  if (usjVersion != null && usjVersion.length() > 0)
	  {
		  usjJarName += "-" + usjVersion;
	  }
	  else
	  {
		  usjJarName += "-" + version;
	  }
		  
	  return usjJarName + ".jar";
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
      System.out.println("Install link to " + artifactId + " into /usr/share/java");
      if (!noUsjVersionless) {
    	  run(linkCommand(compatRelPath(), fullCompatPath()));
      }
      run(linkCommand(compatRelPath(), versionedFullCompatPath()));
    }
  }

  /**
   * clean the pom.xml
   */
  private void cleanPom()
  {
    File pomOptionsFile = new File(debianDir, debianPackage + ".poms");
    ListOfPOMs listOfPOMs = new ListOfPOMs(pomOptionsFile);

    // Use the saved pom before cleaning as it was untouched by the transform operation
    String pomPath = pomSrcPath() + ".save";
    File pomFile = new File(pomPath);
    String originalPomPath = pomSrcPath();
    File originalPom = new File(originalPomPath);
    if (! pomFile.exists())
    {
        pomFile = originalPom;
        pomPath = originalPomPath;
    }

    String relativePomPath = originalPom.getAbsolutePath();
    relativePomPath = relativePomPath.substring(debianDir.getParentFile().getAbsolutePath().length() + 1);

    ListOfPOMs.POMOptions pomOption = listOfPOMs.getPOMOptions(relativePomPath);

    if (pomOption != null && pomOption.isIgnore()) {
        throw new RuntimeException("POM file " + pomFile + " should be ignored");
    }
    if (pomOption != null && pomOption.getDestPackage() != null) {
      destPackage = pomOption.getDestPackage();
    }
    
    // handle usj-name
    if (pomOption != null && pomOption.getUsjName() != null) {
      usjName = pomOption.getUsjName();
    }
    
    // handle usj-version
    if (pomOption != null && pomOption.getUsjVersion() != null) {
      usjVersion = pomOption.getUsjVersion();
    }
    
    // handle no-usj-versionless
    if (pomOption != null) {
      noUsjVersionless = pomOption.isNoUsjVersionless();
    }

    List params = new ArrayList();
    params.add("--keep-pom-version");
      
    params.add("--package=" + destPackage);
    String mavenRulesPath = new File(debianDir, mavenRules).getAbsolutePath();
    params.add("--rules=" + mavenRulesPath);

    System.out.println("Cleaning pom file: " + pomFile + " with options:");
    System.out.println("\t--keep-pom-version --package=" + destPackage);
    System.out.println("\t--rules=" + mavenRulesPath);

    // add optional --no-parent option
    if (pomOption != null && pomOption.isNoParent()) {
        params.add("--no-parent");
        System.out.println("\t--no-parent");
    }
    
    // add options --keep-elements option
    if (pomOption != null && pomOption.getKeepElements() != null) {
        params.add("--keep-elements=" + pomOption.getKeepElements());
        System.out.println("\t--keep-elements=" + pomOption.getKeepElements());
    }
    
    params.add(pomFile.getAbsolutePath());
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
     * Prepare the destination  directories: remove the directory symlinks that were created
     * by copy-repo.sh if they exist as they point to a directory owned by root and that cannot
     * be modified.
     */
  protected void prepareDestDirs() {
      // Simply try to delete the path. If it's a symlink, it will work, otherwise delete() returns false
      new File(fullRepoPath()).delete();
      new File(debianFullRepoPath()).delete();
  }
  /**
   * do the actual work
   */
  protected void runMojo() throws IOException
  {
    cleanPom();
    prepareDestDirs();
    copyPom();
    copyJar();
    if (installToUsj) {
      symlinkJar();
    }
  }
}
