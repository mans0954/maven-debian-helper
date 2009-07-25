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
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
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
        String javadocsTemplate = "java-doc.docs.vm";
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
            context.put("packagerLicense", packagerLicense);
            context.put("project", project);
            context.put("runTests", Boolean.valueOf(runTests));
            context.put("generateJavadoc", Boolean.valueOf(generateJavadoc));

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
            context.put("licenses", new TreeSet());
            List description = new ArrayList();
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
                if ("maven".equals(packageType)) {
                    // Remove dependencies that are implied by maven-debian-helper
                    depends.remove("libmaven-clean-plugin-java");
                    depends.remove("libmaven-resources-plugin-java");
                    depends.remove("libmaven-compiler-plugin-java");
                    depends.remove("libmaven-jar-plugin-java");
                    depends.remove("libmaven-site-plugin-java");
                    depends.remove("libsurefire-java");
                    depends.remove("velocity");
                    depends.remove("libplexus-velocity-java");
                    if (generateJavadoc) {
                        depends.add("libmaven-javadoc-plugin-java");
                    }
                } else if ("ant".equals(packageType)) {
                    // Remove dependencies that are implied by ant packaging
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
                    context.put("buildJars", buildJars);
                }
            } else {
                System.err.println("Cannot find file " + substvarsFile);
            }

            if ("ant".equals(packageType)) {
                List pomDirs = new ArrayList();
                if (collectedProjects.isEmpty()) {
                    pomDirs.add("");
                }
                for (Iterator i = collectedProjects.iterator(); i.hasNext();) {
                    MavenProject mavenProject = (MavenProject) i.next();
                    String basedir = project.getBasedir().getAbsolutePath();
                    String dirRelPath = mavenProject.getBasedir().getAbsolutePath().substring(basedir.length() + 1);
                    pomDirs.add(dirRelPath);
                }
                context.put("pomDirs", pomDirs);
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

            FileWriter out = new FileWriter(new File(outputDirectory, "README.source"));
            Velocity.mergeTemplate("README.source.vm", "UTF8", context, out);
            out.flush();
            out.close();

            out = new FileWriter(new File(outputDirectory, "copyright"));
            Velocity.mergeTemplate("copyright.vm", "UTF8", context, out);
            out.flush();
            out.close();

            out = new FileWriter(new File(outputDirectory, "compat"));
            Velocity.mergeTemplate("compat.vm", "UTF8", context, out);
            out.flush();
            out.close();

            out = new FileWriter(new File(outputDirectory, "rules"));
            Velocity.mergeTemplate(rulesTemplate, "UTF8", context, out);
            out.flush();
            out.close();

            makeExecutable("debian/rules");

            String debianVersion = projectVersion.replace("-alpha-", "~alpha");
            debianVersion = debianVersion.replace("-beta-", "~beta");
            debianVersion += "-1";
            context.put("debianVersion", debianVersion);
            out = new FileWriter(".debianVersion");
            Velocity.mergeTemplate("version.vm", "UTF8", context, out);
            out.flush();
            out.close();

            if (generateJavadoc) {
                out = new FileWriter(new File(outputDirectory, binPackageName + "-doc.doc-base"));
                Velocity.mergeTemplate("java-doc.doc-base.vm", "UTF8", context, out);
                out.flush();
                out.close();

                out = new FileWriter(new File(outputDirectory, binPackageName + "-doc.docs"));
                Velocity.mergeTemplate(javadocsTemplate, "UTF8", context, out);
                out.flush();
                out.close();
            }

            if ("ant".equals(packageType)) {
                out = new FileWriter(new File(outputDirectory, "build.properties"));
                Velocity.mergeTemplate("build.properties.ant.vm", "UTF8", context, out);
                out.flush();
                out.close();
            } else {
                out = new FileWriter(new File(outputDirectory, "maven.properties"));
                Velocity.mergeTemplate("maven.properties.vm", "UTF8", context, out);
                out.flush();
                out.close();
            }

            out = new FileWriter(new File(outputDirectory, "control"));
            Velocity.mergeTemplate(controlTemplate, "UTF8", context, out);
            out.flush();
            out.close();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private List listSharedJars(String library) {
        final List jars = new ArrayList();
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
