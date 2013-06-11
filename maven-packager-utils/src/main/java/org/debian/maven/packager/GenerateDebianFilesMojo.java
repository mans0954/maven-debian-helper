/*
 * Copyright 2009 Ludovic Claude.
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

package org.debian.maven.packager;

import org.apache.maven.model.Developer;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.*;
import java.util.*;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.project.MavenProject;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.debian.maven.packager.util.LicensesScanner;
import org.debian.maven.packager.util.PackageScanner;
import org.debian.maven.packager.util.UserInteraction;
import org.debian.maven.repo.ListOfPOMs;

/**
 * Generate the Debian files for packaging the current Maven project.
 *
 * @goal generate
 * @aggregator
 * @requiresDependencyResolution
 * @phase process-sources
 * 
 * @author Ludovic Claude
 */
public class GenerateDebianFilesMojo extends AbstractMojo {

    /**
     * The Maven Project Object
     *
     * @parameter expression="${project}"
     * @readonly
     * @required
     */
    protected MavenProject project;
    
    /**
     * A list of every project in this reactor; provided by Maven
     * 
     * @parameter expression="${project.collectedProjects}"
     */
    protected List<MavenProject> collectedProjects;
    
    /**
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    protected ArtifactRepository localRepository;
    
    /**
     * Location of the file.
     * 
     * @parameter expression="${debian.directory}"
     *   default-value="debian"
     */
    protected File outputDirectory;
    
    /**
     * Name of the packager (e.g. 'Ludovic Claude')
     * 
     * @parameter expression="${packager}"
     * @required
     */
    protected String packager;
    
    /**
     * Email of the packager (e.g. 'ludovic.claude@laposte.net')
     * 
     * @parameter expression="${email}"
     * @required
     */
    protected String email;
    
    /**
     * License used by the packager (e.g. 'GPL-3' or 'Apache-2.0')
     * See http://dep.debian.net/deps/dep5/ for the list of licenses.
     * 
     * @parameter expression="${packagerLicense}" default-value="GPL-3"
     * @required
     */
    protected String packagerLicense;
    
    /**
     * Name of the source package (e.g. 'commons-lang')
     * 
     * @parameter expression="${package}"
     * @required
     */
    protected String packageName;
    
    /**
     * Name of the binary package (e.g. 'libcommons-lang-java')
     * 
     * @parameter expression="${bin.package}"
     * @required
     */
    protected String binPackageName;
    
    /**
     * Type of the package (e.g. 'maven' or 'ant')
     * 
     * @parameter expression="${packageType}" default-value="maven"
     */
    protected String packageType;
    
    /**
     * URL for downloading the source code, in the form scm:[svn|cvs]:http://xxx/
     * for downloads using a source code repository,
     * or http://xxx.[tar|zip|gz|tgz] for downloads using source tarballs.
     * 
     * @parameter expression="${downloadUrl}"
     */
    protected String downloadUrl;
    
    /**
     * If true, include running the tests during the build.
     * 
     * @parameter expression="${runTests}" default-value="false"
     */
    protected boolean runTests;
    
    /**
     * If true, generate the Javadoc packaged in a separate package.
     * @parameter expression="${generateJavadoc}" default-value="false"
     */
    protected boolean generateJavadoc;

    private PackageScanner scanner = new PackageScanner(false);
    private LicensesScanner licensesScanner = new LicensesScanner();
    private UserInteraction userInteraction = new UserInteraction();

    public void execute() throws MojoExecutionException {
        File f = outputDirectory;
        if (!f.exists()) {
            f.mkdirs();
        }

        String controlTemplate = "control.vm";
        String rulesTemplate = "rules.vm";
        if ("ant".equals(packageType)) {
            controlTemplate = "control.ant.vm";
            rulesTemplate = "rules.ant.vm";
        }
        // #638788: clean up email
        if (email != null && email.indexOf('<') >= 0 && email.indexOf('>') >= 0) {
            email = email.substring(email.indexOf('<') + 1, email.indexOf('>') - 1);
        }

        try {
            Properties velocityProperties = new Properties();
            velocityProperties.put("resource.loader", "class");
            velocityProperties.put("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
            Velocity.init(velocityProperties);
            VelocityContext context = new VelocityContext();
            context.put("package", packageName);
            context.put("packageType", packageType);
            context.put("binPackage", binPackageName);
            context.put("packager", packager);
            context.put("packagerEmail", email);
            context.put("project", project);
            context.put("collectedProjects", wrapMavenProjects(collectedProjects));
            context.put("runTests", Boolean.valueOf(runTests));
            context.put("generateJavadoc", Boolean.valueOf(generateJavadoc));

            if (project.getName() == null || project.getName().isEmpty()) {
                project.setName(userInteraction.ask("POM does not contain the project name. Please enter the name of the project:"));
            }
            if (project.getUrl() == null || project.getUrl().isEmpty()) {
                project.setUrl(userInteraction.ask("POM does not contain the project URL. Please enter the URL of the project:"));
            }

            Set<String> licenses = licensesScanner.discoverLicenses(project.getLicenses());
            context.put("licenses", licenses);

            if (licenses.size() == 1) {
                packagerLicense = (String) licenses.iterator().next();
            }
            if (packagerLicense == null) {
                String q = "Packager license for the debian/ files was not found, please enter a license name preferably in one of:\n"
                 + "Apache Artistic BSD FreeBSD ISC CC-BY CC-BY-SA CC-BY-ND CC-BY-NC CC-BY-NC-SA CC-BY-NC-ND CC0 CDDL CPL Eiffel"
                 + "Expat GPL LGPL GFDL GFDL-NIV LPPL MPL Perl PSF QPL W3C-Software ZLIB Zope";
                String s = userInteraction.ask(q);
                if (s.length() > 0) {
                    packagerLicense = s;
                }
            }
            context.put("packagerLicense", packagerLicense);

            String copyrightOwner = "";
            String projectTeam = "";
            if (project.getOrganization() != null) {
                copyrightOwner = project.getOrganization().getName();
                projectTeam = project.getOrganization().getName() + " developers";
            }
            if (copyrightOwner == null || copyrightOwner.isEmpty()) {
                Iterator<Developer> devs = project.getDevelopers().iterator();
                if (devs.hasNext()) {
                    Developer dev = devs.next();
                    copyrightOwner = dev.getName();
                    if (dev.getEmail() != null && !dev.getEmail().isEmpty()) {
                        copyrightOwner += " <" + dev.getEmail() + ">";
                    }
                }
            }
            if (copyrightOwner == null || copyrightOwner.isEmpty()) {
                copyrightOwner = userInteraction.ask("Could not find who owns the copyright for the upstream sources, please enter his name:");
            }
            context.put("copyrightOwner", copyrightOwner);

            if (projectTeam == null || projectTeam.isEmpty()) {
                projectTeam = project.getName() + " developers";
            }
            context.put("projectTeam", projectTeam);

            String copyrightYear;
            int currentYear = new GregorianCalendar().get(Calendar.YEAR);
            if (project.getInceptionYear() != null) {
                copyrightYear = project.getInceptionYear();
                if (Integer.parseInt(copyrightYear) < currentYear) {
                    copyrightYear += "-" + currentYear;
                }
            } else {
                copyrightYear = String.valueOf(currentYear);
            }
            context.put("copyrightYear", copyrightYear);
            context.put("currentYear", new Integer(currentYear));

            List<String> description = new ArrayList<String>();
            if (project.getDescription() == null || project.getDescription().trim().isEmpty()) {
                project.setDescription(userInteraction.askMultiLine("Please enter a short description of the project, press Enter twice to stop."));
            }
            if (project.getDescription() != null) {
                StringTokenizer st = new StringTokenizer(project.getDescription().trim(), "\n\t ");
                StringBuilder descLine = new StringBuilder();
                while (st.hasMoreTokens()) {
                    descLine.append(st.nextToken());
                    descLine.append(" ");
                    if (descLine.length() > 70 || !st.hasMoreTokens()) {
                        String line = descLine.toString().trim();
                        if (line.isEmpty()) {
                            line = ".";
                        }
                        description.add(line);
                        descLine = new StringBuilder();
                    }
                }
            }
            context.put("description", description);

            File substvarsFile = new File(outputDirectory, binPackageName + ".substvars");
            if (substvarsFile.exists()) {
                Properties substvars = new Properties();
                substvars.load(new FileReader(substvarsFile));
                List<String> compileDepends = new ArrayList<String>();
                compileDepends.addAll(split(substvars.getProperty("maven.CompileDepends")));
                compileDepends.addAll(split(substvars.getProperty("maven.Depends")));
                List<String> buildDepends = new ArrayList<String>(compileDepends);
                List<String> testDepends = new ArrayList<String>(split(substvars.getProperty("maven.TestDepends")));
                if (runTests) {
                    buildDepends.addAll(testDepends);
                }
                if (generateJavadoc) {
                    buildDepends.addAll(split(substvars.getProperty("maven.DocDepends")));
                    buildDepends.addAll(split(substvars.getProperty("maven.DocOptionalDepends")));
                }
                if ("maven".equals(packageType)) {
                    boolean seenJavadocPlugin = false;
                    // Remove dependencies that are implied by maven-debian-helper
                    for (Iterator<String> i = buildDepends.iterator(); i.hasNext();) {
                        String dependency = i.next();
                        if (dependency.startsWith("libmaven-clean-plugin-java") ||
                                dependency.startsWith("libmaven-resources-plugin-java") ||
                                dependency.startsWith("libmaven-compiler-plugin-java") ||
                                dependency.startsWith("libmaven-jar-plugin-java") ||
                                dependency.startsWith("libmaven-site-plugin-java") ||
                                dependency.startsWith("libsurefire-java") ||
                                dependency.startsWith("velocity") ||
                                dependency.startsWith("libplexus-velocity-java")) {
                            i.remove();
                        } else if (dependency.startsWith("libmaven-javadoc-plugin-java")) {
                            seenJavadocPlugin = true;
                        }
                    }
                    if (generateJavadoc && !seenJavadocPlugin) {
                        buildDepends.add("libmaven-javadoc-plugin-java");
                    }
                } else if ("ant".equals(packageType)) {
                    // Remove dependencies that are implied by ant packaging
                    for (Iterator<String> i = buildDepends.iterator(); i.hasNext(); ) {
                        String dependency = i.next();
                        if (dependency.equals("ant") ||
                                dependency.startsWith("ant ") ||
                                dependency.startsWith("ant-optional")) {
                            i.remove();
                        }
                    }
                    buildDepends.remove("ant");
                    buildDepends.remove("ant-optional");
                }
                context.put("buildDependencies", buildDepends);
                context.put("runtimeDependencies", split(substvars.getProperty("maven.Depends")));
                context.put("testDependencies", split(substvars.getProperty("maven.TestDepends")));
                context.put("optionalDependencies", split(substvars.getProperty("maven.OptionalDepends")));
                context.put("javadocDependencies", split(substvars.getProperty("maven.DocDepends")));
                context.put("javadocOptionalDependencies", split(substvars.getProperty("maven.DocOptionalDepends")));

                if ("ant".equals(packageType)) {
                    Set<String> compileJars = new TreeSet<String>();
                    for (String library : compileDepends) {
                        compileJars.addAll(scanner.listSharedJars(library));
                    }
                    context.put("compileJars", compileJars);
                    Set<String> testJars = new TreeSet<String>();
                    for (String library : testDepends) {
                        testJars.addAll(scanner.listSharedJars(library));
                    }
                    context.put("testJars", testJars);
                }
            } else {
                System.err.println("Cannot find file " + substvarsFile);
            }

            if ("ant".equals(packageType)) {
                ListOfPOMs listOfPOMs = new ListOfPOMs(new File(outputDirectory, binPackageName + ".poms"));
                ListOfPOMs listOfJavadocPOMs = null;
                if (generateJavadoc && "ant".equals(packageType)) {
                    listOfJavadocPOMs =  new ListOfPOMs(new File(outputDirectory, binPackageName + "-doc.poms"));
                }
                setupArtifactLocation(listOfPOMs, listOfJavadocPOMs, project);
                for (MavenProject mavenProject : collectedProjects) {
                    setupArtifactLocation(listOfPOMs, listOfJavadocPOMs, mavenProject);
                }
                listOfPOMs.save();
                if (listOfJavadocPOMs != null) {
                    listOfJavadocPOMs.save();
                }
            }

            String projectVersion = project.getVersion();
            int downloadType = DownloadType.UNKNOWN;

            if (downloadUrl == null) {
                if (project.getScm() != null) {
                    downloadUrl = project.getScm().getConnection();
                }
            }
            if (downloadUrl != null && downloadUrl.startsWith("scm:svn:")) {
                downloadType = DownloadType.SVN;
                downloadUrl = downloadUrl.substring("scm:svn:".length());
                String tag = projectVersion;
                int tagPos = downloadUrl.indexOf(tag);
                String baseUrl = null;
                String suffixUrl = null;
                String tagMarker = null;
                if (tagPos >= 0) {
                    baseUrl = downloadUrl.substring(0, tagPos);
                    suffixUrl = downloadUrl.substring(tagPos + tag.length());
                    if (!suffixUrl.endsWith("/")) {
                        suffixUrl += "/";
                    }
                    int slashPos = baseUrl.lastIndexOf("/");
                    tagMarker = baseUrl.substring(slashPos + 1);
                    baseUrl = baseUrl.substring(0, slashPos);
                }
                if (tagPos < 0 && downloadUrl.contains("/trunk")) {
                    System.out.println("Download URL does not include a tagged revision but /trunk found,");
                    System.out.println("Trying to guess the address of the tagged revision.");
                    tag = "trunk";
                    tagPos = downloadUrl.indexOf(tag);
                    baseUrl = downloadUrl.substring(0, tagPos);
                    baseUrl += "tags";
                    tagMarker = packageName  + "-";
                    suffixUrl = "";
                }
                if (tagPos >= 0) {
                    context.put("baseUrl", baseUrl);
                    context.put("tagMarker", tagMarker);
                    context.put("suffixUrl", suffixUrl);

                    generateFile(context, "watch.svn.vm", outputDirectory, "watch");
                    generateFile(context, "orig-tar.svn.vm", outputDirectory, "orig-tar.sh");

                    scanner.makeExecutable("debian/orig-tar.sh");

                } else {
                    System.err.println("Cannot locate the version in the download url (" +
                            downloadUrl + ").");
                    System.err.println("Please run again and provide the download location with an explicit version tag, e.g.");
                    System.err.println("-DdownloadUrl=scm:svn:http://svn.codehaus.org/modello/tags/modello-1.0-alpha-21/");
                }
            }

            if (downloadType == DownloadType.UNKNOWN) {
                System.err.println("Cannot recognize the download url (" +
                        downloadUrl + ").");
            }

            generateFile(context, "README.source.vm", outputDirectory, "README.source");
            generateFile(context, "copyright.vm", outputDirectory, "copyright");
            generateFile(context, "compat.vm", outputDirectory, "compat");
            generateFile(context, rulesTemplate, outputDirectory, "rules");

            scanner.makeExecutable("debian/rules");

            String debianVersion = projectVersion.replace("-alpha-", "~alpha");
            debianVersion = debianVersion.replace("-beta-", "~beta");
            debianVersion += "-1";
            context.put("version.vm", debianVersion);

            generateFile(context, rulesTemplate, new File("."), ".debianVersion");

            if (generateJavadoc) {
                if (project.getPackaging().equals("pom") && collectedProjects.size() > 1) {
                    generateFile(context, "java-doc.doc-base.api.multi.vm", outputDirectory, binPackageName + "-doc.doc-base.api");
                    generateFile(context, "java-doc.install.multi.vm", outputDirectory, binPackageName + "-doc.install");
                } else {
                  generateFile(context, "java-doc.doc-base.api.vm", outputDirectory, binPackageName + "-doc.doc-base.api");
                  generateFile(context, "java-doc.install.vm", outputDirectory, binPackageName + "-doc.install");
                }
            }

            if ("ant".equals(packageType)) {
                boolean containsJars = false;
                boolean containsPlugins = false;
                if (project.getPackaging().equals("pom") && project.getModules().size() > 0) {
                    for (MavenProject module : collectedProjects) {
                        if (module.getPackaging().equals("maven-plugin")) {
                            containsPlugins = true;
                        } else if (!module.getPackaging().equals("pom")) {
                            containsJars = true;
                        }
                    }
                } else if (!project.getPackaging().equals("pom")) {
                    if (project.getPackaging().equals("maven-plugin")) {
                        containsPlugins = true;
                    } else if (!project.getPackaging().equals("pom")) {
                        containsJars = true;
                    }
                }
                context.put("containsJars", Boolean.valueOf(containsJars));
                context.put("containsPlugins", Boolean.valueOf(containsPlugins));

                if (project.getPackaging().equals("pom") && project.getModules().size() > 0) {
                    generateFile(context, "build.xml.vm", outputDirectory, "build.xml");
                }
                generateFile(context, "build.properties.ant.vm", outputDirectory, "build.properties");
                generateFile(context, "build-classpath.vm", outputDirectory, "build-classpath");
            } else {
                generateFile(context, "maven.properties.vm", outputDirectory, "maven.properties");
            }

            generateFile(context, controlTemplate, outputDirectory, "control");
            generateFile(context, "format.vm", new File(outputDirectory, "source"), "format");

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private List<WrappedProject> wrapMavenProjects(List<MavenProject> projects) {
        List<WrappedProject> wrappedProjects = new ArrayList<WrappedProject>();
        for (MavenProject aProject: projects) {
            wrappedProjects.add(new WrappedProject(this.project, aProject));
        }
        return wrappedProjects;
    }

    private void setupArtifactLocation(ListOfPOMs listOfPOMs, ListOfPOMs listOfJavadocPOMs, MavenProject mavenProject) {
        String dirRelPath = new WrappedProject(project, mavenProject).getBaseDir();

        if (! "pom".equals(mavenProject.getPackaging())) {
            String pomFile = dirRelPath + "pom.xml";
            listOfPOMs.getOrCreatePOMOptions(pomFile).setJavaLib(true);
            String extension = mavenProject.getPackaging();
            if (extension.equals("bundle")) {
                extension = "jar";
            }
            if (extension.equals("webapp")) {
                extension = "war";
            }
            if (mavenProject.getArtifact() != null && mavenProject.getArtifact().getFile() != null) {
                extension = mavenProject.getArtifact().getFile().toString();
                extension = extension.substring(extension.lastIndexOf('.') + 1);
            }
            ListOfPOMs.POMOptions pomOptions = listOfPOMs.getOrCreatePOMOptions(pomFile);
            pomOptions.setArtifact(dirRelPath + "target/" + mavenProject.getArtifactId() + "-*."
                + extension);
            if ("jar".equals(extension) && generateJavadoc && "ant".equals(packageType) && listOfJavadocPOMs != null) {
                String artifactId = mavenProject.getArtifact().getArtifactId();
                ListOfPOMs.POMOptions javadocPomOptions = listOfJavadocPOMs.getOrCreatePOMOptions(pomFile);
                javadocPomOptions.setIgnorePOM(true);
                javadocPomOptions.setArtifact(dirRelPath + "target/" + artifactId + ".javadoc.jar");
                javadocPomOptions.setClassifier("javadoc");
                javadocPomOptions.setHasPackageVersion(pomOptions.getHasPackageVersion());
            }
            pomOptions.setJavaLib(true);
            if (mavenProject.getArtifactId().matches(packageName + "\\d")) {
                pomOptions.setUsjName(packageName);
            }
        }
    }

    private void generateFile(VelocityContext context, String templateName, File destDir, String fileName) throws Exception {
        destDir.mkdirs();
        FileWriter out = new FileWriter(new File(destDir, fileName));
        Velocity.mergeTemplate(templateName, "UTF8", context, out);
        out.flush();
        out.close();
    }

    private List<String> split(String s) {
        List<String> l = new ArrayList<String>();
        if (s != null) {
            StringTokenizer st = new StringTokenizer(s, ",");
            while (st.hasMoreTokens()) {
                l.add(st.nextToken().trim());
            }
        }
        return l;
    }

    public static class WrappedProject {
        private final MavenProject baseProject;
        private final MavenProject mavenProject;

        public WrappedProject(MavenProject baseProject, MavenProject mavenProject) {
            this.baseProject = baseProject;
            this.mavenProject = mavenProject;
        }

        public String getBaseDir() {
            String basedir = baseProject.getBasedir().getAbsolutePath();
            String dirRelPath = "";
            if (! mavenProject.getBasedir().equals(baseProject.getBasedir())) {
                dirRelPath = mavenProject.getBasedir().getAbsolutePath().substring(basedir.length() + 1) + "/";
            }
            return dirRelPath;
        }

        public String getArtifactId() {
            return mavenProject.getArtifactId();
        }

        public String getGroupId() {
            return mavenProject.getGroupId();
        }

        public String getVersion() {
            return mavenProject.getVersion();
        }

        public String getPackaging() {
            return mavenProject.getPackaging();
        }
    }

    interface DownloadType {

        int UNKNOWN = 0;
        int SVN = 1;
        int CVS = 2;
        int TARBALL = 3;
    }
}

