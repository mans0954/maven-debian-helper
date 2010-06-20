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
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.stream.XMLStreamException;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import org.debian.maven.repo.Dependency;
import org.debian.maven.repo.POMInfo;
import org.debian.maven.repo.POMReader;
import org.debian.maven.repo.Repository;

/**
 * Analyze the Maven dependencies and extract the Maven rules to use
 * as well as the list of dependent packages.
 *
 * @author Ludovic Claude
 */
public class DependenciesSolver {

    // Plugins not useful for the build or whose use is against the
    // Debian policy
    private static final String[][] PLUGINS_TO_IGNORE = {
        {"org.apache.maven.plugins", "maven-ant-plugin"},
        {"org.apache.maven.plugins", "maven-archetype-plugin"},
        {"org.apache.maven.plugins", "maven-deploy-plugin"},
        {"org.apache.maven.plugins", "maven-release-plugin"},
        {"org.apache.maven.plugins", "maven-remote-resources-plugin"},
        {"org.apache.maven.plugins", "maven-repository-plugin"},
        {"org.apache.maven.plugins", "maven-scm-plugin"},
        {"org.apache.maven.plugins", "maven-stage-plugin"},
        {"org.apache.maven.plugins", "maven-eclipse-plugin"},
        {"org.apache.maven.plugins", "maven-idea-plugin"},
        {"org.codehaus.mojo", "netbeans-freeform-maven-plugin"},
        {"org.codehaus.mojo", "nbm-maven-plugin"},
        {"org.codehaus.mojo", "ideauidesigner-maven-plugin"},
        {"org.codehaus.mojo", "scmchangelog-maven-plugin"},};
    private static final String[][] PLUGINS_THAT_CAN_BE_IGNORED = {
        {"org.apache.maven.plugins", "maven-assembly-plugin"},
        {"org.codehaus.mojo", "buildnumber-maven-plugin"},
        {"org.apache.maven.plugins", "maven-verifier-plugin"},
        {"org.codehaus.mojo", "findbugs-maven-plugin"},
        {"org.codehaus.mojo", "fitnesse-maven-plugin"},
        {"org.codehaus.mojo", "selenium-maven-plugin"},
        {"org.codehaus.mojo", "dbunit-maven-plugin"},
        {"org.codehaus.mojo", "failsafe-maven-plugin"},
        {"org.codehaus.mojo", "shitty-maven-plugin"},};
    private static final String[][] DOC_PLUGINS = {
        {"org.apache.maven.plugins", "maven-changelog-plugin"},
        {"org.apache.maven.plugins", "maven-changes-plugin"},
        {"org.apache.maven.plugins", "maven-checkstyle-plugin"},
        {"org.apache.maven.plugins", "maven-docck-plugin"},
        {"org.apache.maven.plugins", "maven-javadoc-plugin"},
        {"org.apache.maven.plugins", "maven-jxr-plugin"},
        {"org.apache.maven.plugins", "maven-pmd-plugin"},
        {"org.apache.maven.plugins", "maven-project-info-reports-plugin"},
        {"org.apache.maven.plugins", "maven-surefire-report-plugin"},
        {"org.apache.maven.plugins", "maven-pdf-plugin"},
        {"org.apache.maven.plugins", "maven-site-plugin"},
        {"org.codehaus.mojo", "clirr-maven-plugin"},
        {"org.codehaus.mojo", "cobertura-maven-plugin"},
        {"org.codehaus.mojo", "taglist-maven-plugin"},
        {"org.codehaus.mojo", "dita-maven-plugin"},
        {"org.codehaus.mojo", "docbook-maven-plugin"},
        {"org.codehaus.mojo", "javancss-maven-plugin"},
        {"org.codehaus.mojo", "jdepend-maven-plugin"},
        {"org.codehaus.mojo", "dashboard-maven-plugin"},
        {"org.codehaus.mojo", "emma-maven-plugin"},
        {"org.codehaus.mojo", "sonar-maven-plugin"},};
    private static final String[][] TEST_PLUGINS = {
        {"org.apache.maven.plugins", "maven-failsafe-plugin"},
        {"org.apache.maven.plugins", "maven-surefire-plugin"},
        {"org.apache.maven.plugins", "maven-verifier-plugin"},
        {"org.codehaus.mojo", "findbugs-maven-plugin"},
        {"org.codehaus.mojo", "fitnesse-maven-plugin"},
        {"org.codehaus.mojo", "selenium-maven-plugin"},
        {"org.codehaus.mojo", "dbunit-maven-plugin"},
        {"org.codehaus.mojo", "failsafe-maven-plugin"},
        {"org.codehaus.mojo", "shitty-maven-plugin"},};
    private static final String[][] EXTENSIONS_TO_IGNORE = {
        {"org.apache.maven.wagon", "wagon-ssh"},
        {"org.apache.maven.wagon", "wagon-ssh-external"},
        {"org.apache.maven.wagon", "wagon-ftp"},
        {"org.apache.maven.wagon", "wagon-http"},
        {"org.apache.maven.wagon", "wagon-http-lightweight"},
        {"org.apache.maven.wagon", "wagon-scm"},};

    protected File baseDir;
    protected File listOfPoms;
    protected File outputDirectory;
    protected String packageName;
    protected String packageType;
    protected File mavenRepo = new File("/usr/share/maven-repo");
    protected boolean exploreProjects;
    protected List projects = new ArrayList();
    private Repository repository;
    private List issues = new ArrayList();
    private List pomsConfig = new ArrayList();
    private List projectPoms = new ArrayList();
    private List toResolve = new ArrayList();
    private Set compileDepends = new TreeSet();
    private Set testDepends = new TreeSet();
    private Set runtimeDepends = new TreeSet();
    private Set optionalDepends = new TreeSet();
    private Set rules = new TreeSet();
    private Set ignoreRules = new TreeSet();
    private Set cleanIgnoreRules = new TreeSet();
    private Set ignoredDependencies = new HashSet();
    private boolean checkedAptFile;
    private boolean runTests;
    private boolean generateJavadoc;
    private boolean nonInteractive;
    private boolean askedToFilterModules = false;
    private boolean filterModules = false;

    public void setRunTests(boolean b) {
        this.runTests = b;
    }

    private void setGenerateJavadoc(boolean b) {
        this.generateJavadoc = b;
    }

    private boolean containsPlugin(String[][] pluginDefinitions, Dependency plugin) {
        for (int i = 0; i < pluginDefinitions.length; i++) {
            if (!plugin.getGroupId().equals(pluginDefinitions[i][0])) {
                continue;
            }
            if (plugin.getArtifactId().equals(pluginDefinitions[i][1])) {
                return true;
            }
        }
        return false;
    }

    private boolean isJavadocPlugin(Dependency dependency) {
        return containsPlugin(DOC_PLUGINS, dependency);
    }

    private boolean isTestPlugin(Dependency dependency) {
        return containsPlugin(TEST_PLUGINS, dependency);
    }

    private boolean isDefaultMavenPlugin(Dependency dependency) {
        if (repository != null && repository.getSuperPOM() != null) {
            for (Iterator i = repository.getSuperPOM().getPluginManagement().iterator(); i.hasNext();) {
                Dependency defaultPlugin = (Dependency) i.next();
                if (defaultPlugin.equalsIgnoreVersion(dependency)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean canIgnorePlugin(Dependency dependency) {
        return containsPlugin(PLUGINS_TO_IGNORE, dependency);
    }

    private boolean canIgnoreExtension(Dependency dependency) {
        return containsPlugin(EXTENSIONS_TO_IGNORE, dependency);
    }

    private boolean canBeIgnoredPlugin(Dependency dependency) {
        return containsPlugin(PLUGINS_THAT_CAN_BE_IGNORED, dependency);
    }

    private boolean askIgnoreDependency(Dependency dependency, String message) {
        if (nonInteractive) {
            return false;
        }
        System.out.println(message);
        System.out.println("  " + dependency);
        System.out.print("[y]/n > ");
        String s = System.console().readLine().trim().toLowerCase();
        if (s.startsWith("n")) {
            return false;
        }
        return true;
    }

    public void setNonInteractive(boolean nonInteractive) {
        this.nonInteractive = nonInteractive;
    }

    private class ToResolve {

        private final File sourcePom;
        private final Collection poms;
        private final boolean buildTime;
        private final boolean mavenExtension;
        private final boolean management;

        private ToResolve(File sourcePom, Collection poms, boolean buildTime, boolean mavenExtension, boolean management) {
            this.sourcePom = sourcePom;
            this.poms = poms;
            this.buildTime = buildTime;
            this.mavenExtension = mavenExtension;
            this.management = management;
        }

        public void resolve() {
            resolveDependencies(sourcePom, poms, buildTime, mavenExtension, management);
        }
    }

    public File getBaseDir() {
        return baseDir;
    }

    public File getListOfPoms() {
        return listOfPoms;
    }

    public void setListOfPoms(File listOfPoms) {
        this.listOfPoms = listOfPoms;
    }

    public void saveListOfPoms() {
        if (listOfPoms != null && !listOfPoms.exists()) {
            try {
                PrintWriter out = new PrintWriter(new FileWriter(listOfPoms));
                out.println("# List of POM files for the package");
                out.println("# Format of this file is:");
                out.println("# <path to pom file> [option]");
                out.println("# where option can be:");
                out.println("#   --ignore: ignore this POM or");
                out.println("#   --no-parent: remove the <parent> tag from the POM");
                for (Iterator i = pomsConfig.iterator(); i.hasNext();) {
                    String config = (String) i.next();
                    out.println(config);
                }
                out.flush();
                out.close();
            } catch (Exception ex) {
                Logger.getLogger(DependenciesSolver.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void saveMavenRules() {
        File mavenRules = new File(outputDirectory, "maven.rules");
        if (!mavenRules.exists()) {
            try {
                PrintWriter out = new PrintWriter(new FileWriter(mavenRules));
                out.println("# Maven rules - transform Maven dependencies and plugins");
                out.println("# Format of this file is:");
                out.println("# [group] [artifact] [type] [version]");
                out.println("# where each element can be either");
                out.println("# - the exact string, for example org.apache for the group, or 3.1");
                out.println("#   for the version. In this case, the element is simply matched");
                out.println("#   and left as it is");
                out.println("# - * (the star character, alone). In this case, anything will");
                out.println("#   match and be left as it is. For example, using * on the");
                out.println("#  position of the artifact field will match any artifact id");
                out.println("# - a regular expression of the form s/match/replace/");
                out.println("#   in this case, elements that match are transformed using");
                out.println("#   the regex rule.");
                out.println("# All elements much match before a rule can be applied");
                out.println("# Example rule: match jar with groupid= junit, artifactid= junit");
                out.println("# and version starting with 3., replacing the version with 3.x");
                out.println("#   junit junit jar s/3\\..*/3.x/");

                for (Iterator i = rules.iterator(); i.hasNext();) {
                    String rule = (String) i.next();
                    out.println(rule);
                }
                out.flush();
                out.close();
            } catch (IOException ex) {
                Logger.getLogger(DependenciesSolver.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void saveMavenPublishedRules() {
        File mavenRules = new File(outputDirectory, "maven.publishedRules");
        if (!mavenRules.exists()) {
            try {
                PrintWriter out = new PrintWriter(new FileWriter(mavenRules));
                out.println("# Maven published rules - additional rules to publish, to help");
                out.println("# the packaging work of Debian maintainers using mh_make");
                out.println("# Format of this file is:");
                out.println("# [group] [artifact] [type] [version]");
                out.println("# where each element can be either");
                out.println("# - the exact string, for example org.apache for the group, or 3.1");
                out.println("#   for the version. In this case, the element is simply matched");
                out.println("#   and left as it is");
                out.println("# - * (the star character, alone). In this case, anything will");
                out.println("#   match and be left as it is. For example, using * on the");
                out.println("#  position of the artifact field will match any artifact id");
                out.println("# - a regular expression of the form s/match/replace/");
                out.println("#   in this case, elements that match are transformed using");
                out.println("#   the regex rule.");
                out.println("# All elements much match before a rule can be applied");
                out.println("# Example rule: match any dependency whose group is ant,");
                out.println("# replacing it with org.apache.ant");
                out.println("#   s/ant/org.apache.ant/ * * s/.*/debian/");

                out.flush();
                out.close();
            } catch (IOException ex) {
                Logger.getLogger(DependenciesSolver.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void saveMavenIgnoreRules() {
        File mavenRules = new File(outputDirectory, "maven.ignoreRules");
        if (!mavenRules.exists()) {
            try {
                PrintWriter out = new PrintWriter(new FileWriter(mavenRules));
                out.println("# Maven ignore rules - ignore some Maven dependencies and plugins");
                out.println("# Format of this file is:");
                out.println("# [group] [artifact] [type] [version]");
                out.println("# where each element can be either");
                out.println("# - the exact string, for example org.apache for the group, or 3.1");
                out.println("#   for the version. In this case, the element is simply matched");
                out.println("#   and left as it is");
                out.println("# - * (the star character, alone). In this case, anything will");
                out.println("#   match and be left as it is. For example, using * on the");
                out.println("#  position of the artifact field will match any artifact id");
                out.println("# All elements much match before a rule can be applied");
                out.println("# Example rule: match jar with groupid= junit, artifactid= junit");
                out.println("# and version starting with 3., this dependency is then removed");
                out.println("# from the POM");
                out.println("#   junit junit jar s/3\\..*/3.x/");

                for (Iterator i = ignoreRules.iterator(); i.hasNext();) {
                    String rule = (String) i.next();
                    out.println(rule);
                }

                out.flush();
                out.close();
            } catch (IOException ex) {
                Logger.getLogger(DependenciesSolver.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void saveMavenCleanIgnoreRules() {
        File mavenRules = new File(outputDirectory, "maven.cleanIgnoreRules");
        if (!mavenRules.exists()) {
            try {
                PrintWriter out = new PrintWriter(new FileWriter(mavenRules));
                out.println("# Maven clean ignore rules - ignore some Maven dependencies and plugins during the clean phase");
                out.println("# Format of this file is:");
                out.println("# [group] [artifact] [type] [version]");
                out.println("# where each element can be either");
                out.println("# - the exact string, for example org.apache for the group, or 3.1");
                out.println("#   for the version. In this case, the element is simply matched");
                out.println("#   and left as it is");
                out.println("# - * (the star character, alone). In this case, anything will");
                out.println("#   match and be left as it is. For example, using * on the");
                out.println("#  position of the artifact field will match any artifact id");
                out.println("# All elements much match before a rule can be applied");
                out.println("# Example rule: match jar with groupid= junit, artifactid= junit");
                out.println("# and version starting with 3., this dependency is then removed");
                out.println("# from the POM");
                out.println("#   junit junit jar s/3\\..*/3.x/");

                for (Iterator i = cleanIgnoreRules.iterator(); i.hasNext();) {
                    String rule = (String) i.next();
                    out.println(rule);
                }

                out.flush();
                out.close();
            } catch (IOException ex) {
                Logger.getLogger(DependenciesSolver.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void saveSubstvars() {
        File dependencies = new File(outputDirectory, packageName + ".substvars");
        Properties depVars = new Properties();
        if (dependencies.exists()) {
            try {
                depVars.load(new FileReader(dependencies));
            } catch (IOException ex) {
                Logger.getLogger(DependenciesSolver.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        depVars.put("maven.CompileDepends", toString(compileDepends));
        depVars.put("maven.TestDepends", toString(testDepends));
        depVars.put("maven.Depends", toString(runtimeDepends));
        depVars.put("maven.OptionalDepends", toString(optionalDepends));
        Set docRuntimeDepends = new TreeSet();
        docRuntimeDepends.add("default-jdk-doc");
        for (Iterator i = runtimeDepends.iterator(); i.hasNext();) {
            docRuntimeDepends.add(i.next() + "-doc");
        }
        Set docOptionalDepends = new TreeSet();
        for (Iterator i = optionalDepends.iterator(); i.hasNext();) {
            docOptionalDepends.add(i.next() + "-doc");
        }
        depVars.put("maven.DocDepends", toString(docRuntimeDepends));
        depVars.put("maven.DocOptionalDepends", toString(docOptionalDepends));
        try {
            depVars.store(new FileWriter(dependencies), "List of dependencies for " + packageName + ", generated for use by debian/control");
        } catch (IOException ex) {
            Logger.getLogger(DependenciesSolver.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void setBaseDir(File baseDir) {
        this.baseDir = baseDir;
    }

    public boolean isExploreProjects() {
        return exploreProjects;
    }

    public void setExploreProjects(boolean exploreProjects) {
        this.exploreProjects = exploreProjects;
    }

    public File getMavenRepo() {
        return mavenRepo;
    }

    public void setMavenRepo(File mavenRepo) {
        this.mavenRepo = mavenRepo;
    }

    public File getOutputDirectory() {
        return outputDirectory;
    }

    public void setOutputDirectory(File outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getPackageType() {
        return packageType;
    }

    public void setPackageType(String packageType) {
        this.packageType = packageType;
    }

    public List getProjects() {
        return projects;
    }

    public void setProjects(List projects) {
        this.projects = projects;
    }

    public List getIssues() {
        return issues;
    }

    public void solveDependencies() {
        File f = outputDirectory;
        if (!f.exists()) {
            f.mkdirs();
        }

        repository = new Repository((mavenRepo));
        repository.scan();

        if (exploreProjects) {
            File pom = new File(baseDir, "pom.xml");
            if (pom.exists()) {
                projects.add(pom);
            } else {
                pom = new File(baseDir, "debian/pom.xml");
                if (pom.exists()) {
                    projects.add(pom);
                } else {
                    System.err.println("Cannot find the POM file");
                    return;
                }
            }
            resolveDependencies(pom);
        } else {
            for (Iterator i = projects.iterator(); i.hasNext();) {
                File pom = (File) i.next();
                resolveDependencies(pom);
            }
        }

        resolveDependenciesNow();

        if (!issues.isEmpty()) {
            System.err.println("WARNING:");
            for (Iterator i = issues.iterator(); i.hasNext();) {
                String issue = (String) i.next();
                System.err.println(issue);
            }
            System.err.println("--------");
        }
    }

    private void resolveDependencies(File projectPom) {
        POMReader reader = new POMReader();
        String pomRelPath = projectPom.getAbsolutePath().substring(baseDir.getAbsolutePath().length() + 1);
        try {
            POMInfo pom = reader.readPom(projectPom);
            pom.setProperties(new HashMap());
            pom.getProperties().put("debian.package", getPackageName());
//            System.out.println("Register POM " + pom.getThisPom().getGroupId() + ":" + pom.getThisPom().getArtifactId()
//                    + ":" + pom.getThisPom().getVersion());
            repository.registerPom(projectPom, pom);

            if (filterModules) {
                System.out.println("Include the module " + pomRelPath + " ?");
                System.out.print("[y]/n > ");
                String s = System.console().readLine().trim().toLowerCase();
                boolean includeModule = !s.startsWith("n");
                if (!includeModule) {
                    pomsConfig.add(pomRelPath + " --ignore");
                    String type = "*";
                    if (pom.getThisPom().getType() != null) {
                        type = pom.getThisPom().getType();
                    }
                    ignoreRules.add(pom.getThisPom().getGroupId() + " " + pom.getThisPom().getArtifactId()
                            + " " + type + " *");
                    return;
                }
            }

            boolean noParent = false;
            if (pom.getParent() != null) {
                POMInfo parentPom = repository.searchMatchingPOM(pom.getParent());
                if (parentPom == null || parentPom.equals(repository.getSuperPOM())) {
                    noParent = true;
                }
                if (!baseDir.equals(projectPom.getParentFile())) {
//                    System.out.println("Checking the parent dependency in the sub project " + projectPom);
                    Set parentDependencies = new TreeSet();
                    parentDependencies.add(pom.getParent());
                    resolveDependenciesLater(projectPom, parentDependencies, false, false, false);
                }
            }

            projectPoms.add(pom.getThisPom());
            if (noParent) {
                pomsConfig.add(pomRelPath + " --no-parent");
            } else {
                pomsConfig.add(pomRelPath);
            }

            resolveDependenciesLater(projectPom, pom.getDependencies(), false, false, false);
            resolveDependenciesLater(projectPom, pom.getDependencyManagement(), false, false, true);
            resolveDependenciesLater(projectPom, pom.getPlugins(), true, true, false);
            resolveDependenciesLater(projectPom, pom.getPluginDependencies(), true, true, false);
            resolveDependenciesLater(projectPom, pom.getPluginManagement(), true, true, true);
            resolveDependenciesLater(projectPom, pom.getExtensions(), true, true, false);

            if (exploreProjects && !pom.getModules().isEmpty()) {
                if (!nonInteractive && !askedToFilterModules) {
                    System.out.println("This project contains modules. Include all modules?");
                    System.out.print("[y]/n > ");
                    String s = System.console().readLine().trim().toLowerCase();
                    filterModules = s.startsWith("n");
                }
                for (Iterator i = pom.getModules().iterator(); i.hasNext();) {
                    String module = (String) i.next();
                    File modulePom = new File(projectPom.getParent(), module + "/pom.xml");
                    resolveDependencies(modulePom);
                }
            }
        } catch (XMLStreamException ex) {
            Logger.getLogger(DependenciesSolver.class.getName()).log(Level.SEVERE, null, ex);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(DependenciesSolver.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void resolveDependenciesNow() {
        for (Iterator i = toResolve.iterator(); i.hasNext();) {
            ToResolve tr = (ToResolve) i.next();
            tr.resolve();
            i.remove();
        }
    }

    private void resolveDependenciesLater(File sourcePom, Collection poms, boolean buildTime, boolean mavenExtension, boolean management) {
        toResolve.add(new ToResolve(sourcePom, poms, buildTime, mavenExtension, management));
    }

    private void resolveDependencies(File sourcePom, Collection poms, boolean buildTime, boolean mavenExtension, boolean management) {
        String sourcePomLoc = sourcePom.getAbsolutePath();
        String baseDirPath = baseDir.getAbsolutePath();
        sourcePomLoc = sourcePomLoc.substring(baseDirPath.length() + 1, sourcePomLoc.length());

        nextDependency:
        for (Iterator i = poms.iterator(); i.hasNext();) {
            Dependency dependency = (Dependency) i.next();
            if (containsDependencyIgnoreVersion(ignoredDependencies, dependency) ||
                    (management && isDefaultMavenPlugin(dependency))) {
                continue;
            }

            boolean ignoreDependency = false;
            if (canIgnorePlugin(dependency)) {
                ignoreDependency = askIgnoreDependency(dependency, "This plugin is not useful for the build or its use is against Debian policies. Ignore this plugin?");
            } else if (canIgnoreExtension(dependency)) {
                ignoreDependency = askIgnoreDependency(dependency, "This extension is not useful for the build or its use is against Debian policies. Ignore this extension?");
            } else if (canBeIgnoredPlugin(dependency)) {
                ignoreDependency = askIgnoreDependency(dependency, "This plugin may be ignored in some cases. Ignore this plugin?");
            } else if (!runTests) {
                if ("test".equals(dependency.getScope())) {
                    ignoreDependency = askIgnoreDependency(dependency, "Tests are turned off. Ignore this test dependency?");
                } else if (isTestPlugin(dependency)) {
                    ignoreDependency = askIgnoreDependency(dependency, "Tests are turned off. Ignore this test plugin?");
                }
            } else if (!generateJavadoc && isJavadocPlugin(dependency)) {
                ignoreDependency = askIgnoreDependency(dependency, "Documentation is turned off. Ignore this documentation plugin?");
            }

            if (ignoreDependency) {
                ignoredDependencies.add(dependency);
                ignoreRules.add(dependency.getGroupId() + " " + dependency.getArtifactId() + " * *");
                continue;
            }

            POMInfo pom = repository.searchMatchingPOM(dependency);
            if (pom == null && "maven-plugin".equals(dependency.getType())) {
                List matchingPoms = repository.searchMatchingPOMsIgnoreVersion(dependency);
                if (matchingPoms.size() > 1) {
                    issues.add(sourcePomLoc + ": More than one version matches the plugin " + dependency.getGroupId() + ":"
                            + dependency.getArtifactId() + ":" + dependency.getVersion());
                }
                if (!matchingPoms.isEmpty()) {
                    pom = (POMInfo) matchingPoms.get(0);
                    // Don't add a rule to force the version of a Maven plugin, it's now done
                    // automatically at build time
                }
            }
            if (pom == null) {
                if (!management) {
                    issues.add(sourcePomLoc + ": Dependency is not packaged in the Maven repository for Debian: " + dependency.getGroupId() + ":"
                            + dependency.getArtifactId() + ":" + dependency.getVersion());
                    ignoreDependency = askIgnoreDependency(dependency, "This dependency cannot be found in the Debian Maven repository. Ignore this dependency?");
                    if (ignoreDependency) {
                        ignoredDependencies.add(dependency);
                        ignoreRules.add(dependency.getGroupId() + " " + dependency.getArtifactId() + " * *");
                        continue;
                    }
                }

                return;
            }

            // Handle the case of Maven plugins built and used in a multi-module build:
            // they need to be added to maven.cleanIgnoreRules to avoid errors during
            // a mvn clean
            if ("maven-plugin".equals(dependency.getType()) && containsDependencyIgnoreVersion(projectPoms, dependency)) {
                cleanIgnoreRules.add(dependency.getGroupId() + " " + dependency.getArtifactId() + " maven-plugin *");
            }

            // Discover the library to import for the dependency
            String library = null;
            if (pom.getProperties() != null) {
                library = (String) pom.getProperties().get("debian.package");
            }
            if (library == null) {
                issues.add(sourcePomLoc + ": Dependency is missing the Debian properties in its POM: " + dependency.getGroupId() + ":"
                        + dependency.getArtifactId() + ":" + dependency.getVersion());
                File pomFile = new File(mavenRepo, dependency.getGroupId().replace(".", "/") + "/" + dependency.getArtifactId() + "/" + dependency.getVersion() + "/" + dependency.getArtifactId() + "-" + dependency.getVersion() + ".pom");
                library = searchPkg(pomFile);
            }
            if (library != null && !library.equals(getPackageName())) {
                if (buildTime) {
                    if ("test".equals(dependency.getScope())) {
                        testDepends.add(library);
                    } else if ("maven-plugin".equals(dependency.getType())) {
                        if (!packageType.equals("ant")) {
                            compileDepends.add(library + " (>= " + pom.getOriginalVersion() + ")");
                        }
                    } else if (mavenExtension) {
                        if (!packageType.equals("ant")) {
                            compileDepends.add(library);
                        }
                    } else {
                        compileDepends.add(library);
                    }
                } else {
                    if (dependency.isOptional()) {
                        optionalDepends.add(library);
                    } else if ("test".equals(dependency.getScope())) {
                        testDepends.add(library);
                    } else {
                        runtimeDepends.add(library);
                    }
                }
            }
            String mavenRules = (String) pom.getProperties().get("debian.mavenRules");
            if (mavenRules != null) {
                StringTokenizer st = new StringTokenizer(mavenRules, ",");
                while (st.hasMoreTokens()) {
                    rules.add(st.nextToken().trim());
                }
            }
        }
    }

    private boolean containsDependencyIgnoreVersion(Collection dependencies, Dependency dependency) {
        for (Iterator j = dependencies.iterator(); j.hasNext();) {
            Dependency ignoredDependency = (Dependency) j.next();
            if (ignoredDependency.equalsIgnoreVersion(dependency)) {
                return true;
            }
        }
        return false;
    }

    private String searchPkg(File pomFile) {
        GetPackageResult packageResult = new GetPackageResult();
        executeProcess(new String[]{"dpkg", "--search", pomFile.getAbsolutePath()}, packageResult);
        if (packageResult.getResult() != null) {
            return packageResult.getResult();
        }

        if (!checkedAptFile) {
            if (!"maven2".equals(searchPkg(new File("/usr/bin/mvn")))) {
                System.err.println("Warning: apt-file doesn't seem to be configured");
                System.err.println("Please run the following command and start again:");
                System.err.println("  sudo apt-file update");
                return null;
            }
            checkedAptFile = true;
        }
        executeProcess(new String[]{"apt-file", "search", pomFile.getAbsolutePath()}, packageResult);
        return packageResult.getResult();
    }

    public static void executeProcess(final String[] cmd, final OutputHandler handler) {
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            System.out.print("> ");
            for (int i = 0; i < cmd.length; i++) {
                String arg = cmd[i];
                System.out.print(arg + " ");
            }
            System.out.println();
            final Process process = pb.start();
            try {
                ThreadFactory threadFactory = new ThreadFactory() {

                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r, "Run command " + cmd[0]);
                        t.setDaemon(true);
                        return t;
                    }
                };

                ExecutorService executor = Executors.newSingleThreadExecutor(threadFactory);
                executor.execute(new Runnable() {

                    public void run() {
                        try {
                            InputStreamReader isr = new InputStreamReader(process.getInputStream());
                            BufferedReader br = new BufferedReader(isr);
                            LineNumberReader aptIn = new LineNumberReader(br);
                            String line;
                            while ((line = aptIn.readLine()) != null) {
                                System.out.println(line);
                                handler.newLine(line);
                            }
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }
                });

                process.waitFor();
                executor.awaitTermination(5, TimeUnit.SECONDS);
                if (process.exitValue() == 0) {
                } else {
                    System.out.println("Cannot execute " + cmd[0]);
                }
                process.destroy();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
                Thread.interrupted();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private String toString(Set s) {
        StringBuffer sb = new StringBuffer();
        for (Iterator i = s.iterator(); i.hasNext();) {
            String st = (String) i.next();
            sb.append(st);
            if (i.hasNext()) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    public static interface OutputHandler {

        void newLine(String line);
    }

    public static class NoOutputHandler implements OutputHandler {

        public void newLine(String line) {
        }
    }

    static class GetPackageResult implements OutputHandler {

        private String result;

        public void newLine(String line) {
            int colon = line.indexOf(':');
            if (colon > 0 && line.indexOf(' ') > colon) {
                result = line.substring(0, colon);
                // Ignore lines such as 'dpkg : xxx'
                if (!result.equals(result.trim()) || result.startsWith("dpkg")) {
                    result = null;
                } else {
                    System.out.println("Found " + result);
                }
            }
        }

        public String getResult() {
            return result;
        }
    };

    public static void main(String[] args) {
        if (args.length == 0 || "-h".equals(args[0]) || "--help".equals(args[0])) {
            System.out.println("Purpose: Solve the dependencies in the POM(s).");
            System.out.println("Usage: [option]");
            System.out.println("");
            System.out.println("Options:");
            System.out.println("  -v, --verbose: be extra verbose");
            System.out.println("  -p<package>, --package=<package>: name of the Debian package containing");
            System.out.println("    this library");
            System.out.println("  -r<rules>, --rules=<rules>: path to the file containing the");
            System.out.println("    extra rules to apply when cleaning the POM");
            System.out.println("  -i<rules>, --published-rules=<rules>: path to the file containing the");
            System.out.println("    extra rules to publish in the property debian.mavenRules in the cleaned POM");
            System.out.println("  --ant: use ant for the packaging");
            System.out.println("  --run-tests: run the unit tests");
            System.out.println("  --generate-javadoc: generate Javadoc");
            System.out.println("  --non-interactive: non interactive session");
            return;
        }
        DependenciesSolver solver = new DependenciesSolver();

        solver.setBaseDir(new File("."));
        solver.setExploreProjects(true);
        solver.setOutputDirectory(new File("debian"));

        int i = inc(-1, args);
        boolean verbose = false;
        String debianPackage = "";
        String packageType = "maven";
        while (i < args.length && (args[i].trim().startsWith("-") || args[i].trim().isEmpty())) {
            String arg = args[i].trim();
            if ("--verbose".equals(arg) || "-v".equals(arg)) {
                verbose = true;
            } else if (arg.startsWith("-p")) {
                debianPackage = arg.substring(2);
            } else if (arg.startsWith("--package=")) {
                debianPackage = arg.substring("--package=".length());
            } else if (arg.equals("--ant")) {
                packageType = "ant";
            } else if (arg.equals("--run-tests")) {
                solver.setRunTests(true);
            } else if (arg.equals("--generate-javadoc")) {
                solver.setGenerateJavadoc(true);
            } else if (arg.equals("--non-interactive")) {
                solver.setNonInteractive(true);
            }
            i = inc(i, args);
        }
        File poms = new File(solver.getOutputDirectory(), debianPackage + ".poms");

        solver.setPackageName(debianPackage);
        solver.setPackageType(packageType);
        solver.setExploreProjects(true);
        solver.setListOfPoms(poms);

        if (verbose) {
            System.out.println("Solving dependencies for package " + debianPackage);
        }

        solver.solveDependencies();

        solver.saveListOfPoms();
        solver.saveMavenRules();
        solver.saveMavenIgnoreRules();
        solver.saveMavenCleanIgnoreRules();
        solver.saveMavenPublishedRules();
        solver.saveSubstvars();
    }

    private static int inc(int i, String[] args) {
        do {
            i++;
        } while (i < args.length && args[i].isEmpty());
        return i;
    }
}
