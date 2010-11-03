package org.debian.maven.packager;

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
import org.apache.maven.model.Developer;
import org.apache.maven.model.License;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.project.MavenProject;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
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
public class GenerateDebianFilesMojo
        extends AbstractMojo {

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
     * @parameter expression="${project.collectedProjects}"
     */
    protected List collectedProjects;
    /**
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    protected ArtifactRepository localRepository;
    /**
     * Location of the file.
     * @parameter expression="${debian.directory}"
     *   default-value="debian"
     */
    protected File outputDirectory;
    /**
     * Name of the packager (e.g. 'Ludovic Claude')
     * @parameter expression="${packager}"
     * @required
     */
    protected String packager;
    /**
     * Email of the packager (e.g. 'ludovic.claude@laposte.net')
     * @parameter expression="${email}"
     * @required
     */
    protected String email;
    /**
     * License used by the packager (e.g. 'GPL-3' or 'Apache-2.0')
     * See http://dep.debian.net/deps/dep5/ for the list of licenses.
     * @parameter expression="${packagerLicense}" default-value="GPL-3"
     * @required
     */
    protected String packagerLicense;
    /**
     * Name of the source package (e.g. 'commons-lang')
     * @parameter expression="${package}"
     * @required
     */
    protected String packageName;
    /**
     * Name of the binary package (e.g. 'libcommons-lang-java')
     * @parameter expression="${bin.package}"
     * @required
     */
    protected String binPackageName;
    /**
     * Type of the package (e.g. 'maven' or 'ant')
     * @parameter expression="${packageType}" default-value="maven"
     */
    protected String packageType;
    /**
     * URL for downloading the source code, in the form scm:[svn|cvs]:http://xxx/
     * for downloads using a source code repository,
     * or http://xxx.[tar|zip|gz|tgz] for downloads using source tarballs.
     * @parameter expression="${downloadUrl}"
     */
    protected String downloadUrl;
    /**
     * If true, include running the tests during the build.
     * @parameter expression="${runTests}" default-value="false"
     */
    protected boolean runTests;
    /**
     * If true, generate the Javadoc packaged in a separate package.
     * @parameter expression="${generateJavadoc}" default-value="false"
     */
    protected boolean generateJavadoc;

    public void execute()
            throws MojoExecutionException {
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
            context.put("runTests", Boolean.valueOf(runTests));
            context.put("generateJavadoc", Boolean.valueOf(generateJavadoc));

            if (project.getName() == null || project.getName().isEmpty()) {
                System.out.println("POM does not contain the project name. Please enter the name of the project:");
                project.setName(readLine());
            }
            if (project.getUrl() == null || project.getUrl().isEmpty()) {
                System.out.println("POM does not contain the project URL. Please enter the URL of the project:");
                project.setUrl(readLine());
            }

            Set licenses = new TreeSet();
            for (Iterator i = project.getLicenses().iterator(); i.hasNext(); ) {
                License license = (License) i.next();
                String licenseName = "";
                if (license.getName() != null) {
                    licenseName = license.getName() + " ";
                }
                String licenseUrl = "";
                if (license.getUrl() != null) {
                    licenseUrl = license.getUrl().toLowerCase();
                }
                boolean recognized = false;
                if (licenseName.indexOf("mit ") >= 0 || licenseUrl.indexOf("mit-license") >= 0) {
                    licenses.add("MIT");
                    recognized = true;
                } else if (licenseName.indexOf("bsd ") >= 0 || licenseUrl.indexOf("bsd-license") >= 0) {
                    licenses.add("BSD");
                    recognized = true;
                } else if (licenseName.indexOf("artistic ") >= 0 || licenseUrl.indexOf("artistic-license") >= 0) {
                    licenses.add("Artistic");
                    recognized = true;
                } else if (licenseName.indexOf("apache ") >= 0 || licenseUrl.indexOf("apache") >= 0) {
                    if (licenseName.indexOf("2.") >= 0 || licenseUrl.indexOf("2.") >= 0) {
                        licenses.add("Apache-2.0");
                        recognized = true;
                    } else if (licenseName.indexOf("1.0") >= 0 || licenseUrl.indexOf("1.0") >= 0) {
                        licenses.add("Apache-1.0");
                        recognized = true;
                    } else if (licenseName.indexOf("1.1") >= 0 || licenseUrl.indexOf("1.1") >= 0) {
                        licenses.add("Apache-1.1");
                        recognized = true;
                    }
                } else if (licenseName.indexOf("lgpl ") >= 0 || licenseUrl.indexOf("lgpl") >= 0) {
                    if (licenseName.indexOf("2.1") >= 0 || licenseUrl.indexOf("2.1") >= 0) {
                        licenses.add("LGPL-2.1");
                        recognized = true;
                    } else if (licenseName.indexOf("2") >= 0 || licenseUrl.indexOf("2") >= 0) {
                        licenses.add("LGPL-2");
                        recognized = true;
                    } else if (licenseName.indexOf("3") >= 0 || licenseUrl.indexOf("3") >= 0) {
                        licenses.add("LGPL-2");
                        recognized = true;
                    }
                } else if (licenseName.indexOf("gpl ") >= 0 || licenseUrl.indexOf("gpl") >= 0) {
                    if (licenseName.indexOf("2") >= 0 || licenseUrl.indexOf("2") >= 0) {
                        licenses.add("GPL-2");
                        recognized = true;
                    } else if (licenseName.indexOf("3") >= 0 || licenseUrl.indexOf("3") >= 0) {
                        licenses.add("GPL-3");
                        recognized = true;
                    }
                }
                if (!recognized) {
                    System.out.println("License " + licenseName + licenseUrl + " was not recognized, please enter a license name preferably in one of:");
                    System.out.println("Apache Artistic BSD FreeBSD ISC CC-BY CC-BY-SA CC-BY-ND CC-BY-NC CC-BY-NC-SA CC-BY-NC-ND CC0 CDDL CPL Eiffel");
                    System.out.println("Expat GPL LGPL GFDL GFDL-NIV LPPL MPL Perl PSF QPL W3C-Software ZLIB Zope");
                    String s = readLine();
                    if (s.length() > 0) {
                        licenses.add(s);
                    }
                }
            }
            if (licenses.isEmpty()) {
                System.out.println("License was not found, please enter a license name preferably in one of:");
                System.out.println("Apache Artistic BSD FreeBSD ISC CC-BY CC-BY-SA CC-BY-ND CC-BY-NC CC-BY-NC-SA CC-BY-NC-ND CC0 CDDL CPL Eiffel");
                System.out.println("Expat GPL LGPL GFDL GFDL-NIV LPPL MPL Perl PSF QPL W3C-Software ZLIB Zope");
                String s = readLine();
                if (s.length() > 0) {
                    licenses.add(s);
                }
            }
            context.put("licenses", licenses);

            if (licenses.size() == 1) {
                packagerLicense = (String) licenses.iterator().next();
            }
            if (packagerLicense == null) {
                System.out.println("Packager license for the debian/ filse was not found, please enter a license name preferably in one of:");
                System.out.println("Apache Artistic BSD FreeBSD ISC CC-BY CC-BY-SA CC-BY-ND CC-BY-NC CC-BY-NC-SA CC-BY-NC-ND CC0 CDDL CPL Eiffel");
                System.out.println("Expat GPL LGPL GFDL GFDL-NIV LPPL MPL Perl PSF QPL W3C-Software ZLIB Zope");
                String s = readLine();
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
                Iterator devs = project.getDevelopers().iterator();
                if (devs.hasNext()) {
                    Developer dev = (Developer) devs.next();
                    copyrightOwner = dev.getName();
                    if (dev.getEmail() != null && !dev.getEmail().isEmpty()) {
                        copyrightOwner += " <" + dev.getEmail() + ">";
                    }
                }
            }
            if (copyrightOwner == null || copyrightOwner.isEmpty()) {
                System.out.println("Could not find who owns the copyright for the upstream sources, please enter his name:");
                copyrightOwner = readLine();
            }
            context.put("copyrightOwner", copyrightOwner);

            if (projectTeam == null || projectTeam.isEmpty()) {
                projectTeam = project.getName() + " developers";
            }
            context.put("copyrightOwner", copyrightOwner);
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

            List description = new ArrayList();
            if (project.getDescription() == null || project.getDescription().trim().isEmpty()) {
                System.out.println("Please enter a short description of the project, press Enter twice to stop.");
                StringBuffer sb = new StringBuffer();
                int emptyEnterCount = 0;
                while (emptyEnterCount < 2) {
                    String s = readLine();
                    if (s.isEmpty()) {
                        emptyEnterCount++;
                    } else {
                        if (emptyEnterCount > 0) {
                            emptyEnterCount = 0;
                            sb.append("\n");
                        }
                        sb.append(s);
                        sb.append("\n");
                    }
                }
                project.setDescription(sb.toString());
            }
            if (project.getDescription() != null) {
                StringTokenizer st = new StringTokenizer(project.getDescription().trim(), "\n\t ");
                StringBuffer descLine = new StringBuffer();
                while (st.hasMoreTokens()) {
                    descLine.append(st.nextToken());
                    descLine.append(" ");
                    if (descLine.length() > 70 || !st.hasMoreTokens()) {
                        String line = descLine.toString().trim();
                        if (line.isEmpty()) {
                            line = ".";
                        }
                        description.add(line);
                        descLine = new StringBuffer();
                    }
                }
            }
            context.put("description", description);

            File substvarsFile = new File(outputDirectory, binPackageName + ".substvars");
            if (substvarsFile.exists()) {
                Properties substvars = new Properties();
                substvars.load(new FileReader(substvarsFile));
                List depends = new ArrayList();
                depends.addAll(split(substvars.getProperty("maven.CompileDepends")));
                depends.addAll(split(substvars.getProperty("maven.Depends")));
                if (runTests) {
                    depends.addAll(split(substvars.getProperty("maven.TestDepends")));
                }
                if (generateJavadoc) {
                    depends.addAll(split(substvars.getProperty("maven.DocDepends")));
                    depends.addAll(split(substvars.getProperty("maven.DocOptionalDepends")));
                }
                if ("maven".equals(packageType)) {
                    boolean seenJavadocPlugin = false;
                    // Remove dependencies that are implied by maven-debian-helper
                    for (Iterator i = depends.iterator(); i.hasNext();) {
                        String dependency = (String) i.next();
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
                        depends.add("libmaven-javadoc-plugin-java");
                    }
                } else if ("ant".equals(packageType)) {
                    // Remove dependencies that are implied by ant packaging
                    for (Iterator i = depends.iterator(); i.hasNext(); ) {
                        String dependency = (String) i.next();
                        if (dependency.equals("ant") ||
                                dependency.startsWith("ant ") ||
                                dependency.startsWith("ant-optional")) {
                            i.remove();
                        }
                    }
                    depends.remove("ant");
                    depends.remove("ant-optional");
                }
                context.put("compileDependencies", depends);
                context.put("runtimeDependencies", split(substvars.getProperty("maven.Depends")));
                context.put("optionalDependencies", split(substvars.getProperty("maven.OptionalDepends")));

                if ("ant".equals(packageType)) {
                    Set buildJars = new TreeSet();
                    for (Iterator i = depends.iterator(); i.hasNext();) {
                        String library = (String) i.next();
                        buildJars.addAll(listSharedJars(library));
                    }
                    buildJars.add("ant-junit");
                    context.put("buildJars", buildJars);
                }
            } else {
                System.err.println("Cannot find file " + substvarsFile);
            }

            if ("ant".equals(packageType)) {
                ListOfPOMs listOfPOMs = new ListOfPOMs(new File(outputDirectory, binPackageName + ".poms"));
                for (Iterator i = collectedProjects.iterator(); i.hasNext();) {
                    MavenProject mavenProject = (MavenProject) i.next();
                    String basedir = project.getBasedir().getAbsolutePath();
                    String dirRelPath = mavenProject.getBasedir().getAbsolutePath().substring(basedir.length() + 1);
                    if (! "pom".equals(mavenProject.getPackaging())) {
                        String pomFile = dirRelPath + "/pom.xml";
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
                        listOfPOMs.getOrCreatePOMOptions(pomFile).setArtifact(dirRelPath + "/" + mavenProject.getArtifactId() + "-*."
                            + extension);
                    }
                }
                listOfPOMs.save();
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
                if (tagPos < 0 && downloadUrl.indexOf("/trunk") >= 0) {
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

                    FileWriter out = new FileWriter(new File(outputDirectory, "watch"));
                    Velocity.mergeTemplate("watch.svn.vm", "UTF8", context, out);
                    out.flush();
                    out.close();

                    out = new FileWriter(new File(outputDirectory, "orig-tar.sh"));
                    Velocity.mergeTemplate("orig-tar.svn.vm", "UTF8", context, out);
                    out.flush();
                    out.close();

                    makeExecutable("debian/orig-tar.sh");

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

            makeExecutable("debian/rules");

            String debianVersion = projectVersion.replace("-alpha-", "~alpha");
            debianVersion = debianVersion.replace("-beta-", "~beta");
            debianVersion += "-1";
            context.put("version.vm", debianVersion);

            generateFile(context, rulesTemplate, new File("."), ".debianVersion");

            if (generateJavadoc) {
                generateFile(context, "java-doc.doc-base.api.vm", outputDirectory, binPackageName + "-doc.doc-base.api");
                generateFile(context, "java-doc.install.vm", outputDirectory, binPackageName + "-doc.install");
            }

            if ("ant".equals(packageType)) {
                generateFile(context, "build.properties.ant.vm", outputDirectory, "build.properties");
            } else {
                generateFile(context, "maven.properties.vm", outputDirectory, "maven.properties");
            }

            generateFile(context, controlTemplate, outputDirectory, "control");
            generateFile(context, "format.vm", new File(outputDirectory, "source"), "format");

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void generateFile(VelocityContext context, String templateName, File destDir, String fileName) throws Exception {
        destDir.mkdirs();
        FileWriter out = new FileWriter(new File(destDir, fileName));
        Velocity.mergeTemplate(templateName, "UTF8", context, out);
        out.flush();
        out.close();
    }

    private List listSharedJars(String library) {
        final List jars = new ArrayList();
        if (library.indexOf("(") > 0) {
            library = library.substring(0, library.indexOf("(")).trim();
        }
        DependenciesSolver.executeProcess(new String[]{"/usr/bin/dpkg", "--listfiles", library},
                new DependenciesSolver.OutputHandler() {

                    public void newLine(String line) {
                        if (line.startsWith("/usr/share/java/") && line.endsWith(".jar")) {
                            String jar = line.substring("/usr/share/java/".length());
                            jar = jar.substring(0, jar.length() - 4);
                            if (!line.matches(".*/.*-\\d.*")) {
                                jars.add(jar);
                            }
                        }
                    }
                });
        return jars;
    }

    private String readLine() {
        LineNumberReader consoleReader = new LineNumberReader(new InputStreamReader(System.in));
        try {
            return consoleReader.readLine().trim();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    private List split(String s) {
        List l = new ArrayList();
        StringTokenizer st = new StringTokenizer(s, ",");
        while (st.hasMoreTokens()) {
            l.add(st.nextToken().trim());
        }
        return l;
    }

    private void makeExecutable(String file) {
        DependenciesSolver.executeProcess(new String[]{"chmod", "+x", file}, new DependenciesSolver.NoOutputHandler());
    }

    interface DownloadType {

        int UNKNOWN = 0;
        int SVN = 1;
        int CVS = 2;
        int TARBALL = 3;
    }
}

