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
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamException;

import org.debian.maven.repo.Dependency;
import org.debian.maven.repo.DependencyNotFoundException;
import org.debian.maven.repo.DependencyRule;
import org.debian.maven.repo.DependencyRuleSet;
import org.debian.maven.repo.ListOfPOMs;
import org.debian.maven.repo.POMHandler;
import org.debian.maven.repo.POMInfo;
import org.debian.maven.repo.POMTransformer;
import org.debian.maven.repo.Repository;
import org.debian.maven.repo.Rule;

/**
 * Analyze the Maven dependencies and extract the Maven rules to use
 * as well as the list of dependent packages.
 *
 * @author Ludovic Claude
 */
public class DependenciesSolver {

    private static final Logger log = Logger.getLogger(DependenciesSolver.class.getName());

    // Plugins not useful for the build or whose use is against the
    // Debian policy
    private static final String[][] PLUGINS_TO_IGNORE = {
        {"org.apache.maven.plugins", "maven-archetype-plugin"},
        {"org.apache.maven.plugins", "changelog-maven-plugin"},
        {"org.apache.maven.plugins", "maven-deploy-plugin"},
        {"org.apache.maven.plugins", "maven-release-plugin"},
        {"org.apache.maven.plugins", "maven-repository-plugin"},
        {"org.apache.maven.plugins", "maven-scm-plugin"},
        {"org.apache.maven.plugins", "maven-stage-plugin"},
        {"org.apache.maven.plugins", "maven-eclipse-plugin"},
        {"org.apache.maven.plugins", "maven-idea-plugin"},
        {"org.apache.maven.plugins", "maven-source-plugin"},
        {"org.codehaus.mojo", "changelog-maven-plugin"},
        {"org.codehaus.mojo", "netbeans-freeform-maven-plugin"},
        {"org.codehaus.mojo", "nbm-maven-plugin"},
        {"org.codehaus.mojo", "ideauidesigner-maven-plugin"},
        {"org.codehaus.mojo", "scmchangelog-maven-plugin"},};
    private static final String[][] PLUGINS_THAT_CAN_BE_IGNORED = {
        {"org.apache.maven.plugins", "maven-ant-plugin"},
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
        {"org.apache.maven.plugins", "maven-clover-plugin"},
        {"org.apache.maven.plugins", "maven-docck-plugin"},
        {"org.apache.maven.plugins", "maven-javadoc-plugin"},
        {"org.apache.maven.plugins", "maven-jxr-plugin"},
        {"org.apache.maven.plugins", "maven-pmd-plugin"},
        {"org.apache.maven.plugins", "maven-project-info-reports-plugin"},
        {"org.apache.maven.plugins", "maven-surefire-report-plugin"},
        {"org.apache.maven.plugins", "maven-pdf-plugin"},
        {"org.apache.maven.plugins", "maven-site-plugin"},
        {"org.codehaus.mojo", "changes-maven-plugin"},
        {"org.codehaus.mojo", "clirr-maven-plugin"},
        {"org.codehaus.mojo", "cobertura-maven-plugin"},
        {"org.codehaus.mojo", "taglist-maven-plugin"},
        {"org.codehaus.mojo", "dita-maven-plugin"},
        {"org.codehaus.mojo", "docbook-maven-plugin"},
        {"org.codehaus.mojo", "javancss-maven-plugin"},
        {"org.codehaus.mojo", "jdepend-maven-plugin"},
        {"org.codehaus.mojo", "jxr-maven-plugin"},
        {"org.codehaus.mojo", "dashboard-maven-plugin"},
        {"org.codehaus.mojo", "emma-maven-plugin"},
        {"org.codehaus.mojo", "sonar-maven-plugin"},
        {"org.codehaus.mojo", "surefire-report-maven-plugin"},
        {"org.jboss.maven.plugins", "maven-jdocbook-plugin"},
    };
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
        {"org.apache.maven.wagon", "wagon-scm"},
        {"org.apache.maven.wagon", "wagon-webdav"},
        {"org.apache.maven.wagon", "wagon-webdav-jackrabbit"},
        {"org.jvnet.wagon-svn", "wagon-svn"},
    };

    protected File baseDir;
    protected POMTransformer pomTransformer = new POMTransformer();
    protected File outputDirectory;
    protected String packageName;
    protected String packageType;
    private String packageVersion;
    protected File mavenRepo = new File("/usr/share/maven-repo");
    protected boolean exploreProjects;
    private Repository repository;
    private List issues = new ArrayList();
    private List projectPoms = new ArrayList();
    private List toResolve = new ArrayList();
    private Set knownProjectDependencies = new TreeSet();
    private Set ignoredDependencies = new TreeSet();
    private Set notIgnoredDependencies = new TreeSet();
    private Set compileDepends = new TreeSet();
    private Set testDepends = new TreeSet();
    private Set runtimeDepends = new TreeSet();
    private Set optionalDepends = new TreeSet();
    private DependencyRuleSet cleanIgnoreRules = new DependencyRuleSet("Ignore rules to be applied during the Maven clean phase",
            new File("debian/maven.cleanIgnoreRules"));
    private boolean offline;
    private boolean runTests;
    private boolean generateJavadoc;
    private boolean interactive = true;
    private boolean askedToFilterModules = false;
    private boolean filterModules = false;
    private boolean verbose = false;
    private Map pomInfoCache = new HashMap();
    // Keep the previous selected rule for a given version 
    private Map versionToRules = new HashMap();
    // Keep the list of known files and their package
    private Map filesInPackages = new HashMap();
    // Keep the list of packages and dependencies
    private Map versionedPackagesAndDependencies = new HashMap();
    private List defaultRules = new ArrayList();

    public DependenciesSolver() {
        pomTransformer.setVerbose(true);
        pomTransformer.getRules().setWarnRulesFileNotFound(false);
        pomTransformer.getRules().setDescription(readResource("maven.rules.description"));
        pomTransformer.getIgnoreRules().setDescription(readResource("maven.ignoreRules.description"));
        pomTransformer.getIgnoreRules().setWarnRulesFileNotFound(false);
        pomTransformer.getPublishedRules().setDescription(readResource("maven.publishedRules.description"));
        pomTransformer.getPublishedRules().setWarnRulesFileNotFound(false);
        cleanIgnoreRules.setDescription(readResource("maven.cleanIgnoreRules.description"));
        cleanIgnoreRules.setWarnRulesFileNotFound(false);
        cleanIgnoreRules.setVerbose(true);
        cleanIgnoreRules.setDontDuplicate(pomTransformer.getIgnoreRules());        

        Rule toDebianRule = new Rule("s/.*/debian/");
        toDebianRule.setDescription("Change the version to the symbolic 'debian' version");
        Rule keepVersionRule = new Rule("*");
        keepVersionRule.setDescription("Keep the version");
        Rule customRule = new Rule("CUSTOM");
        customRule.setDescription("Custom rule");
        defaultRules.add(toDebianRule);
        defaultRules.add(keepVersionRule);
        defaultRules.add(customRule);
    }

    private static String readResource(String resource) {
        StringBuffer sb = new StringBuffer();
        try {
            InputStream is = DependenciesSolver.class.getResourceAsStream("/" + resource);
            LineNumberReader r = new LineNumberReader(new InputStreamReader(is));
            String line;
            while ((line = r.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
            r.close();
        } catch (IOException e) {
            log.log(Level.SEVERE, "Cannot read resource " + resource, e);
        }
        return sb.toString();
    }

    public boolean isRunTests() {
        return runTests;
    }

    public void setRunTests(boolean b) {
        this.runTests = b;
    }

    public void setOffline(boolean offline) {
        this.offline = offline;
    }

    public boolean isGenerateJavadoc() {
        return generateJavadoc;
    }

    public void setGenerateJavadoc(boolean b) {
        this.generateJavadoc = b;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
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

    private boolean isDocumentationOrReportPlugin(Dependency dependency) {
        return containsPlugin(DOC_PLUGINS, dependency);
    }

    private boolean isTestPlugin(Dependency dependency) {
        return containsPlugin(TEST_PLUGINS, dependency);
    }

    private boolean isDefaultMavenPlugin(Dependency dependency) {
        if (getRepository() != null && getRepository().getSuperPOM() != null) {
            for (Iterator i = getRepository().getSuperPOM().getPluginManagement().iterator(); i.hasNext();) {
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

    private boolean askIgnoreDependency(String sourcePomLoc, Dependency dependency, String message) {
        return askIgnoreDependency(sourcePomLoc, dependency, message, true);
    }

    private boolean askIgnoreDependency(String sourcePomLoc, Dependency dependency, String message, boolean defaultToIgnore) {
        if (!interactive || notIgnoredDependencies.contains(dependency)) {
            return false;
        }
        System.out.println();
        System.out.println("In " + sourcePomLoc + ":");
        System.out.println(message);
        System.out.println("  " + dependency);
        if (defaultToIgnore) {
            System.out.print("[y]/n > ");
        } else {
            System.out.print("y/[n] > ");
        }
        String s = readLine().toLowerCase();
        boolean ignore = defaultToIgnore ? !s.startsWith("n") : s.startsWith("y");
        if (!ignore) {
            notIgnoredDependencies.add(dependency);
        }
        return ignore;
    }

    public boolean isInteractive() {
        return interactive;
    }

    public void setInteractive(boolean interactive) {
        this.interactive = interactive;
    }

    POMTransformer getPomTransformer() {
        return pomTransformer;
    }

    public ListOfPOMs getListOfPOMs() {
        return pomTransformer.getListOfPOMs();
    }
    
    private class ToResolve {

        private final File sourcePom;
        private final String listType;
        private final boolean buildTime;
        private final boolean mavenExtension;
        private final boolean management;

        private ToResolve(File sourcePom, String listType, boolean buildTime, boolean mavenExtension, boolean management) {
            this.sourcePom = sourcePom;
            this.listType = listType;
            this.buildTime = buildTime;
            this.mavenExtension = mavenExtension;
            this.management = management;
        }

        public void resolve() {
            try {
                resolveDependencies(sourcePom, listType, buildTime, mavenExtension, management);
            } catch (Exception e) {
                log.log(Level.SEVERE, "Cannot resolve dependencies in " + sourcePom + ": " + e.getMessage());
            }
        }
    }

    public File getBaseDir() {
        return baseDir;
    }

    public void saveListOfPoms() {
        pomTransformer.getListOfPOMs().save();
    }

    public void saveMavenRules() {
        // Don't save implicit rules
        pomTransformer.getRules().remove(DependencyRule.TO_DEBIAN_VERSION_RULE);
        pomTransformer.getRules().remove(DependencyRule.MAVEN_PLUGINS_KEEP_VERSION_RULE);
        pomTransformer.getRules().save();
    }

    public void saveMavenPublishedRules() {
        // Don't save implicit rules
        pomTransformer.getPublishedRules().remove(DependencyRule.TO_DEBIAN_VERSION_RULE);
        pomTransformer.getPublishedRules().remove(DependencyRule.MAVEN_PLUGINS_KEEP_VERSION_RULE);
        pomTransformer.getPublishedRules().save();
    }

    public void saveMavenIgnoreRules() {
        pomTransformer.getIgnoreRules().save();
    }

    public void saveMavenCleanIgnoreRules() {
        cleanIgnoreRules.save();
    }

    public void saveSubstvars() {
        File dependencies = new File(outputDirectory, packageName + ".substvars");
        Properties depVars = new Properties();
        if (dependencies.exists()) {
            try {
                depVars.load(new FileReader(dependencies));
            } catch (IOException ex) {
                log.log(Level.SEVERE, "Error while reading file " + dependencies, ex);
            }
        }
        depVars.put("maven.CompileDepends", toString(compileDepends));
        depVars.put("maven.TestDepends", toString(testDepends));
        depVars.put("maven.Depends", toString(runtimeDepends));
        depVars.put("maven.OptionalDepends", toString(optionalDepends));
        if (generateJavadoc) {
            System.out.println("Checking dependencies for documentation packages...");
            Set docRuntimeDepends = new TreeSet();
            docRuntimeDepends.add("default-jdk-doc");
            for (Iterator i = runtimeDepends.iterator(); i.hasNext();) {
                String dependency = (String) i.next();
                Dependency runtimeDependency = (Dependency) versionedPackagesAndDependencies.get(dependency);
                if (dependency.indexOf(' ') > 0) {
                    dependency = dependency.substring(0, dependency.indexOf(' '));
                }
                if (runtimeDependency != null && "pom".equals(runtimeDependency.getType())) {
                    continue;
                }
                String docPkg = searchPkg(new File("/usr/share/doc/" + dependency + "/api/index.html"));
                if (docPkg != null) {
                    docRuntimeDepends.add(docPkg);
                }
            }
            Set docOptionalDepends = new TreeSet();
            for (Iterator i = optionalDepends.iterator(); i.hasNext();) {
                String dependency = (String) i.next();
                Dependency optionalDependency = (Dependency) versionedPackagesAndDependencies.get(dependency);
                if (dependency.indexOf(' ') > 0) {
                    dependency = dependency.substring(0, dependency.indexOf(' '));
                }
                if (optionalDependency != null && "pom".equals(optionalDependency.getType())) {
                    continue;
                }
                String docPkg = searchPkg(new File("/usr/share/doc/" + dependency + "/api/index.html"));
                if (docPkg != null) {
                    docOptionalDepends.add(docPkg);
                }
            }
            depVars.put("maven.DocDepends", toString(docRuntimeDepends));
            depVars.put("maven.DocOptionalDepends", toString(docOptionalDepends));
        }
        if (packageVersion != null) {
            depVars.put("maven.UpstreamPackageVersion", packageVersion);
        }
        try {
            depVars.store(new FileOutputStream(dependencies), "List of dependencies for " + packageName + ", generated for use by debian/control");
        } catch (IOException ex) {
            log.log(Level.SEVERE, "Error while saving file " + dependencies, ex);
        }
    }

    public void setBaseDir(File baseDir) {
        this.baseDir = baseDir;
        if (pomTransformer.getListOfPOMs() != null) {
            pomTransformer.getListOfPOMs().setBaseDir(baseDir);
        }
    }

    public void setListOfPoms(File listOfPoms) {
        if (pomTransformer.getListOfPOMs() == null) {
            pomTransformer.setListOfPOMs(new ListOfPOMs(listOfPoms));
        } else {
            pomTransformer.getListOfPOMs().setListOfPOMsFile(listOfPoms);
        }
        pomTransformer.getListOfPOMs().setBaseDir(baseDir);
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
        pomTransformer.getRules().setRulesFile(new File(outputDirectory, "maven.rules"));
        pomTransformer.getIgnoreRules().setRulesFile(new File(outputDirectory, "maven.ignoreRules"));
        pomTransformer.getPublishedRules().setRulesFile(new File(outputDirectory, "maven.publishedRules"));
        cleanIgnoreRules.setRulesFile(new File(outputDirectory, "maven.cleanIgnoreRules"));
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

    public List getIssues() {
        return issues;
    }

    private Repository getRepository() {
        if (repository == null && mavenRepo != null) {
            repository = new Repository(mavenRepo);
            repository.scan();
        }
        return repository;
    }

    public void solveDependencies() {
        pomTransformer.setRepository(getRepository());
        pomTransformer.usePluginVersionsFromRepository();

        File f = outputDirectory;
        if (!f.exists()) {
            f.mkdirs();
        }

        if (exploreProjects) {
            File pom;
            if (pomTransformer.getListOfPOMs().getPomOptions().isEmpty()) {
                pom = new File(baseDir, "pom.xml");
                if (pom.exists()) {
                    pomTransformer.getListOfPOMs().addPOM("pom.xml");
                } else {
                    pom = new File(baseDir, "debian/pom.xml");
                    if (pom.exists()) {
                        pomTransformer.getListOfPOMs().addPOM("debian/pom.xml");
                    } else {
                        System.err.println("Cannot find the POM file");
                        return;
                    }
                }
            } else {
                pom = new File(baseDir, pomTransformer.getListOfPOMs().getFirstPOM());
            }
            resolveDependencies(pom);
        } else {
            pomTransformer.getListOfPOMs().foreachPoms(new POMHandler() {

                public void handlePOM(File pomFile, boolean noParent, boolean hasPackageVersion) throws Exception {
                    resolveDependencies(pomFile);
                }

                public void ignorePOM(File pomFile) throws Exception {
                }
            });
        }

        resolveDependenciesNow();

        if (!issues.isEmpty()) {
            System.err.println("ERROR:");
            for (Iterator i = issues.iterator(); i.hasNext();) {
                String issue = (String) i.next();
                System.err.println(issue);
            }
            System.err.println("--------");
        }
    }

    private void resolveDependencies(File projectPom) {

        if (getPOMOptions(projectPom) != null && getPOMOptions(projectPom).isIgnore()) {
            return;
        }

        String pomRelPath = projectPom.getAbsolutePath().substring(baseDir.getAbsolutePath().length() + 1);

        try {
            POMInfo pom = getPOM(projectPom);
            pom.setProperties(new HashMap());
            pom.getProperties().put("debian.package", getPackageName());

            if (getPOMOptions(projectPom).isNoParent()) {
                pom.setParent(null);
            } else if (pom.getParent() != null) {
                boolean oldNoParent = getPOMOptions(projectPom).isNoParent();
                // Don't mark the parent dependency as 'build time' dependency because once installed,
                // the POM for this project will always need the parent POM to be available
                pom.setParent(resolveDependency(pom.getParent(), projectPom, false, false, false, true));
                if (getPOMOptions(projectPom).isNoParent() != oldNoParent) {
                    resetPOM(projectPom);
                    pom = getPOM(projectPom);
                }
            }

            getRepository().registerPom(projectPom, pom);
            // Also register automatically the test jar which may accompany the current jar and be
            // used in another module of the same project
            if ("jar".equals(pom.getThisPom().getType())) {
                POMInfo testPom = (POMInfo) pom.clone();
                testPom.getThisPom().setType("test-jar");
                getRepository().registerPom(projectPom, testPom);
            }

            knownProjectDependencies.add(pom.getThisPom());

            if (interactive && packageVersion == null) {
                System.out.println("Enter the upstream version for the package. If you press <Enter> it will default to " + pom.getOriginalVersion());
                System.out.print("> ");
                String v = readLine();
                if (v.isEmpty()) {
                    v = pom.getOriginalVersion();
                }
                packageVersion = v;
            }

            if (pom.getOriginalVersion().equals(packageVersion)) {
                pom.getProperties().put("debian.hasPackageVersion", "true");
                getPOMOptions(projectPom).setHasPackageVersion(true);
            }

            if (filterModules) {
                System.out.println("Include the module " + pomRelPath + " ?");
                System.out.print("[y]/n > ");
                String s = readLine().toLowerCase();
                boolean includeModule = !s.startsWith("n");
                if (!includeModule) {
                    getPOMOptions(projectPom).setIgnore(true);
                    String type = "*";
                    if (pom.getThisPom().getType() != null) {
                        type = pom.getThisPom().getType();
                    }
                    String rule = pom.getThisPom().getGroupId() + " " + pom.getThisPom().getArtifactId()
                            + " " + type + " *";
                    pomTransformer.getIgnoreRules().add(new DependencyRule(rule));
                    return;
                }
            }

            // Previous rule from another run
            boolean explicitlyMentionedInRules = false;
            for (Iterator i = pomTransformer.getRules().findMatchingRules(pom.getThisPom()).iterator();
                    i.hasNext(); ) {
                DependencyRule previousRule = (DependencyRule) i.next();
                if (!previousRule.equals(DependencyRule.TO_DEBIAN_VERSION_RULE) &&
                        !previousRule.equals(DependencyRule.MAVEN_PLUGINS_KEEP_VERSION_RULE) &&
                        previousRule.matches(pom.getThisPom())) {
                    explicitlyMentionedInRules = true;
                    break;
                }
            }

            if (interactive && !explicitlyMentionedInRules && !"maven-plugin".equals(pom.getThisPom().getType())) {
                String version = pom.getThisPom().getVersion();
                System.out.println("Version of " + pom.getThisPom().getGroupId() + ":"
                    + pom.getThisPom().getArtifactId() + " is " + version);
                System.out.println("Choose how it will be transformed:");
                List choices = new ArrayList();

                if (versionToRules.containsKey(version)) {
                    choices.add(versionToRules.get(version));
                }

                Pattern p = Pattern.compile("(\\d+)(\\..*)");
                Matcher matcher = p.matcher(version);
                if (matcher.matches()) {
                    String mainVersion = matcher.group(1);
                    Rule mainVersionRule = new Rule("s/" + mainVersion + "\\..*/" +
                        mainVersion + ".x/");
                    mainVersionRule.setDescription("Replace all versions starting by "
                        + mainVersion + ". with " + mainVersion + ".x");
                    if (!choices.contains(mainVersionRule)) {
                        choices.add(mainVersionRule);
                    }
                }
                for (Iterator i = defaultRules.iterator(); i.hasNext(); ) {
                    Rule rule = (Rule) i.next();
                    if (!choices.contains(rule)) {
                        choices.add(rule);
                    }
                }

                int count = 1;
                for (Iterator i = choices.iterator(); i.hasNext(); count++) {
                    Rule rule = (Rule) i.next();
                    if (count == 1) {
                        System.out.print("[1]");
                    } else {
                        System.out.print(" " + count + " ");
                    }
                    System.out.println(" - " + rule.getDescription());
                }
                System.out.print("> ");
                String s = readLine().toLowerCase();
                int choice = 1;
                try {
                    choice = Integer.parseInt(s);
                } catch (Exception ignore) {
                }

                Rule selectedRule = (Rule) choices.get(choice - 1);
                versionToRules.put(version, selectedRule);
                if (selectedRule.getPattern().equals("CUSTOM")) {
                    System.out.println("Enter the pattern for your custom rule (in the form s/regex/replace/)");
                    System.out.print("> ");
                    s = readLine().toLowerCase();
                    selectedRule = new Rule(s);
                    selectedRule.setDescription("My custom rule " + s);
                    defaultRules.add(selectedRule);
                }

                String dependencyRule = pom.getThisPom().getGroupId() + " " + pom.getThisPom().getArtifactId()
                        + " " + pom.getThisPom().getType() + " " + selectedRule.toString();
                pomTransformer.getRules().add(new DependencyRule(dependencyRule));
            }

            if (pom.getParent() != null) {
                POMInfo parentPom = getRepository().searchMatchingPOM(pom.getParent());
                if (parentPom == null || parentPom.equals(getRepository().getSuperPOM())) {
                    getPOMOptions(projectPom).setNoParent(true);
                }
                if (!baseDir.equals(projectPom.getParentFile())) {
//                    System.out.println("Checking the parent dependency in the sub project " + projectPom);
                    resolveDependenciesLater(projectPom, POMInfo.PARENT, false, false, false);
                }
            }

            projectPoms.add(pom.getThisPom());

            resolveDependenciesLater(projectPom, POMInfo.DEPENDENCIES, false, false, false);
            resolveDependenciesLater(projectPom, POMInfo.DEPENDENCY_MANAGEMENT_LIST, false, false, true);
            resolveDependenciesLater(projectPom, POMInfo.PLUGINS, true, true, false);
            resolveDependenciesLater(projectPom, POMInfo.PLUGIN_DEPENDENCIES, true, true, false);
            resolveDependenciesLater(projectPom, POMInfo.PLUGIN_MANAGEMENT, true, true, true);
            resolveDependenciesLater(projectPom, POMInfo.REPORTING_PLUGINS, true, true, false);
            resolveDependenciesLater(projectPom, POMInfo.EXTENSIONS, true, true, false);

            if (exploreProjects && !pom.getModules().isEmpty()) {
                if (interactive && !askedToFilterModules) {
                    System.out.println("This project contains modules. Include all modules?");
                    System.out.print("[y]/n > ");
                    String s = readLine().toLowerCase();
                    filterModules = s.startsWith("n");
                    askedToFilterModules = true;
                }
                for (Iterator i = pom.getModules().iterator(); i.hasNext();) {
                    String module = (String) i.next();
                    File modulePom = new File(projectPom.getParent(), module + "/pom.xml");
                    resolveDependencies(modulePom);
                }
            }
        } catch (Exception ex) {
            log.log(Level.SEVERE, "Error while resolving " + projectPom + ": " + ex.getMessage());
            System.exit(1);
        }
    }

    private POMInfo getPOM(File projectPom) throws XMLStreamException, IOException {
        POMInfo info = (POMInfo) pomInfoCache.get(projectPom.getAbsolutePath());
        if (info != null) {
            return info;
        }
        File tmpDest = File.createTempFile("pom", ".tmp", baseDir);
        tmpDest.deleteOnExit();
        ListOfPOMs.POMOptions options = getPOMOptions(projectPom);
        boolean noParent = false;
        boolean hasPackageVersion = false;
        if (options != null) {
            noParent = options.isNoParent();
            hasPackageVersion = options.getHasPackageVersion();
        }

        info = pomTransformer.transformPom(projectPom, tmpDest, noParent, hasPackageVersion, false, false, null, null, true);
        pomInfoCache.put(projectPom.getAbsolutePath(), info);
        return info;
    }

    private ListOfPOMs.POMOptions getPOMOptions(File pom) {
        return pomTransformer.getListOfPOMs().getOrCreatePOMOptions(pom);
    }

    private void resetPOM(File projectPom) {
         pomInfoCache.remove(projectPom.getAbsolutePath());
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

    private void resolveDependenciesNow() {
        for (Iterator i = toResolve.iterator(); i.hasNext();) {
            ToResolve tr = (ToResolve) i.next();
            tr.resolve();
            i.remove();
        }
    }

    private void resolveDependenciesLater(File sourcePom, String listType, boolean buildTime, boolean mavenExtension, boolean management) {
        toResolve.add(new ToResolve(sourcePom, listType, buildTime, mavenExtension, management));
    }

    private void resolveDependencies(File sourcePom, String listType, boolean buildTime, boolean mavenExtension, boolean management) throws Exception {
        List poms = getPOM(sourcePom).getAllDependencies(listType);

        for (Iterator i = poms.iterator(); i.hasNext();) {
            Dependency dependency = (Dependency) i.next();
            resolveDependency(dependency, sourcePom, buildTime, mavenExtension, management);
        }
    }

    private Dependency resolveParentDependency(Dependency dependency, File sourcePom, boolean buildTime) throws DependencyNotFoundException {
        return resolveDependency(dependency, sourcePom, buildTime, false, false);
    }

    public Dependency resolveDependency(Dependency dependency, File sourcePom, boolean buildTime, boolean mavenExtension, boolean management) throws DependencyNotFoundException {
        return resolveDependency(dependency, sourcePom, buildTime, mavenExtension, management, false);
    }

    private Dependency resolveDependency(Dependency dependency, File sourcePom, boolean buildTime, boolean mavenExtension, boolean management, boolean resolvingParent) throws DependencyNotFoundException {

        if (containsDependencyIgnoreVersion(ignoredDependencies, dependency) ||
            containsDependencyIgnoreVersion(knownProjectDependencies, dependency) ||
                (management && isDefaultMavenPlugin(dependency))) {
            return null;
        }

        String sourcePomLoc = sourcePom.getAbsolutePath();
        String baseDirPath = baseDir.getAbsolutePath();
        sourcePomLoc = sourcePomLoc.substring(baseDirPath.length() + 1, sourcePomLoc.length());
        if (verbose) {
            String msg = "Resolving " + dependency;
            if (dependency.getScope() != null) {
                msg += " of scope " + dependency.getScope();
            }
            System.out.println(msg + "...");
        }

        // First let the packager mark as ignored those dependencies which should be ignored
        boolean ignoreDependency = false;
        if (!ignoreDependency && canIgnorePlugin(dependency)) {
            ignoreDependency = askIgnoreDependency(sourcePomLoc, dependency, "This plugin is not useful for the build or its use is against Debian policies. Ignore this plugin?");
        }
        if (!ignoreDependency && canIgnoreExtension(dependency)) {
            ignoreDependency = askIgnoreDependency(sourcePomLoc, dependency, "This extension is not useful for the build or its use is against Debian policies. Ignore this extension?");
        }
        if (!ignoreDependency && canBeIgnoredPlugin(dependency)) {
            ignoreDependency = askIgnoreDependency(sourcePomLoc, dependency, "This plugin may be ignored in some cases. Ignore this plugin?");
        }
        if (!ignoreDependency && !runTests) {
            if ("test".equals(dependency.getScope())) {
                ignoreDependency = askIgnoreDependency(sourcePomLoc, dependency, "Tests are turned off. Ignore this test dependency?");
            } else if (isTestPlugin(dependency)) {
                ignoreDependency = askIgnoreDependency(sourcePomLoc, dependency, "Tests are turned off. Ignore this test plugin?");
            }
        }
        if (!ignoreDependency && !generateJavadoc && isDocumentationOrReportPlugin(dependency)) {
            ignoreDependency = askIgnoreDependency(sourcePomLoc, dependency, "Documentation is turned off. Ignore this documentation plugin?");
        }

        if (ignoreDependency) {
            ignoredDependencies.add(dependency);
            String ruleDef = dependency.getGroupId() + " " + dependency.getArtifactId() + " * *";
            pomTransformer.getIgnoreRules().add(new DependencyRule(ruleDef));
            if (verbose) {
                System.out.println("[ignored]");
            }
            return null;
        }

        // Automatically skip some dependencies when ant packaging is used
        boolean skipDependency = false;
        String skipReason = "";
        if (packageType.equals("ant")) {
            if ("maven-plugin".equals(dependency.getType())) {
                try {
                    if (!getPOM(sourcePom).getThisPom().getType().equals("pom")) {
                        skipDependency = true;
                        skipReason = "Maven plugins are not used during a build with Ant";
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (!runTests && "test".equals(dependency.getScope())) {
                skipDependency = true;
                skipReason = "Tests are not executed during the build";
            }
        }
        if (skipDependency) {
            // Even if we skip the dependency, try to locate its associated maven rules,
            // as this may be useful later - but never fail if the dependency is not found.
            POMInfo pom = getRepository().searchMatchingPOM(dependency);
            if (pom != null) {
                String mavenRules = (String) pom.getProperties().get("debian.mavenRules");
                if (mavenRules != null) {
                    StringTokenizer st = new StringTokenizer(mavenRules, ",");
                    while (st.hasMoreTokens()) {
                        String ruleDef = st.nextToken().trim();
                        pomTransformer.getRules().add(new DependencyRule(ruleDef));
                    }
                }
            }
            if (verbose) {
                if (!skipReason.isEmpty()) {
                    System.out.println("[skipped - " + skipReason + "]");
                } else {
                    System.out.println("[skipped]");
                }
            }
            return null;
        }

        POMInfo pom = getRepository().searchMatchingPOM(dependency);
        if (pom == null && dependency.getVersion() == null) {
            // Set a dummy version and try again
            for (int version = 0; version < 10; version++) {
                dependency.setVersion(version + ".0");
                pom = getRepository().searchMatchingPOM(dependency);
                if (pom != null) {
                    break;
                }
                dependency.setVersion(null);
            }
        }

        if (pom == null && "maven-plugin".equals(dependency.getType())) {
            List matchingPoms = getRepository().searchMatchingPOMsIgnoreVersion(dependency);
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
        
        // In case, we didn't found anything for "jar" packaging type, just check for a "bundle" type inside repository.
        // drazzib: I'm not sure this is really the right way to fix that (ie. maybe we should install "bundle" artifacts
        // directly with "jar" type inside Debian ?).
        if (pom == null && "jar".equals(dependency.getType())) {
            if (verbose) {
                System.out.println("[check with bundle dependency type]");
            }
            dependency.setType("bundle");
            pom = getRepository().searchMatchingPOM(dependency);
        }

        if (pom == null) {
            if (management) {
                if (verbose) {
                    System.out.println("[skipped dependency or plugin management]");
                }
                return null;
            } else if ("maven-plugin".equals(dependency.getType()) && packageType.equals("ant")) {
                    if (verbose) {
                        System.out.println("[skipped - not used in Ant build]");
                    }
                    return null;
            } else {
                if (!ignoreDependency) {
                    if (resolvingParent) {
                        boolean noParent = askIgnoreDependency(sourcePomLoc, dependency,
                                "The parent POM cannot be found in the Maven repository for Debian. Ignore it?");
                        getPOMOptions(sourcePom).setNoParent(noParent);
                        if (noParent) {
                            if (verbose) {
                                System.out.println("[no-parent]");
                            }
                            return null;
                        }
                    } else if (isDocumentationOrReportPlugin(dependency)) {
                        ignoreDependency = askIgnoreDependency(sourcePomLoc, dependency,
                                "This documentation or report plugin cannot be found in the Maven repository for Debian. Ignore this plugin?");
                    } else if ("maven-plugin".equals(dependency.getType())) {
                        ignoreDependency = askIgnoreDependency(sourcePomLoc, dependency, "This plugin cannot be found in the Debian Maven repository. Ignore this plugin?", false);
                        if (!ignoreDependency) {
                            issues.add(sourcePomLoc + ": Plugin is not packaged in the Maven repository for Debian: " + dependency.getGroupId() + ":"
                                    + dependency.getArtifactId() + ":" + dependency.getVersion());
                        }
                    } else {
                        ignoreDependency = askIgnoreDependency(sourcePomLoc, dependency, "This dependency cannot be found in the Debian Maven repository. Ignore this dependency?", false);
                        if (!ignoreDependency) {
                            issues.add(sourcePomLoc + ": Dependency is not packaged in the Maven repository for Debian: " + dependency.getGroupId() + ":"
                                    + dependency.getArtifactId() + ":" + dependency.getVersion());
                        }
                    }
                }
                if (ignoreDependency) {
                    ignoredDependencies.add(dependency);
                    String ruleDef = dependency.getGroupId() + " " + dependency.getArtifactId() + " * *";
                    pomTransformer.getIgnoreRules().add(new DependencyRule(ruleDef));
                    if (verbose) {
                        System.out.println("[ignored]");
                    }
                    return null;
                } else {
                    String pkg = searchPkg(new File("/usr/share/maven-repo/"
                        + dependency.getGroupId().replace('.', '/')
                        + "/" + dependency.getArtifactId()));
                    if (pkg != null) {
                        System.out.println("Please install the missing dependency using");
                        System.out.println("  sudo apt-get install " + pkg);
                    }
                    if (interactive) {
                        System.out.println("Try again to resolve the dependency?");
                        System.out.print("[y]/n > ");
                        String s = readLine().trim().toLowerCase();
                        if (!s.startsWith("n")) {
                            return resolveDependency(dependency, sourcePom, buildTime, mavenExtension, management);
                        } 
                    }
                    if (verbose) {
                        System.out.println("[error]");
                    }
                    throw new DependencyNotFoundException(dependency);
                }
            }

        }

        // Handle the case of Maven plugins built and used in a multi-module build:
        // they need to be added to maven.cleanIgnoreRules to avoid errors during
        // a mvn clean
        if ("maven-plugin".equals(dependency.getType()) && containsDependencyIgnoreVersion(projectPoms, dependency)) {
            String ruleDef = dependency.getGroupId() + " " + dependency.getArtifactId() + " maven-plugin *";
            cleanIgnoreRules.add(new DependencyRule(ruleDef));
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
            String libraryWithVersionConstraint = library;
            String version = dependency.getVersion();
            if (version == null || (pom.getOriginalVersion() != null && version.compareTo(pom.getOriginalVersion()) > 0)) {
                version = pom.getOriginalVersion();
            }
            if (pom.getOriginalVersion() != null && (pom.getProperties().containsKey("debian.hasPackageVersion"))) {
                libraryWithVersionConstraint += " (>= " + version + ")";
            }
            if (buildTime) {
                if ("test".equals(dependency.getScope())) {
                    testDepends.add(libraryWithVersionConstraint);
                } else if ("maven-plugin".equals(dependency.getType())) {
                    if (!packageType.equals("ant")) {
                        compileDepends.add(libraryWithVersionConstraint);
                    }
                } else if (mavenExtension) {
                    if (!packageType.equals("ant")) {
                        compileDepends.add(libraryWithVersionConstraint);
                    }
                } else {
                    compileDepends.add(libraryWithVersionConstraint);
                }
            } else {
                if (dependency.isOptional()) {
                    optionalDepends.add(libraryWithVersionConstraint);
                } else if ("test".equals(dependency.getScope())) {
                    testDepends.add(libraryWithVersionConstraint);
                } else {
                    runtimeDepends.add(libraryWithVersionConstraint);
                }
            }
            versionedPackagesAndDependencies.put(libraryWithVersionConstraint, dependency);
        }

        String mavenRules = (String) pom.getProperties().get("debian.mavenRules");
        if (mavenRules != null) {
            StringTokenizer st = new StringTokenizer(mavenRules, ",");
            while (st.hasMoreTokens()) {
                String ruleDef = st.nextToken().trim();
                pomTransformer.getRules().add(new DependencyRule(ruleDef));
            }
        }
        if (verbose) {
            System.out.println("[ok]");
        }
        return pom.getThisPom();
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

    private String searchPkg(File file) {
        if (filesInPackages.containsKey(file)) {
            return (String) filesInPackages.get(file);
        }

        GetPackageResult packageResult = new GetPackageResult();
        executeProcess(new String[]{"dpkg", "--search", file.getAbsolutePath()}, packageResult);
        if (packageResult.getResult() != null) {
            String pkg = packageResult.getResult();
            if (pkg != null) {
                filesInPackages.put(file, pkg);
            }
            return pkg;
        }

        // Debian policy prevents the use of apt-file during a build
        if (offline) {
            return null;
        }

        if (!new File("/usr/bin/apt-file").exists()) {
            return null;
        }
        executeProcess(new String[]{"apt-file", "search", file.getAbsolutePath()}, packageResult);
        String pkg = packageResult.getResult();
        if (pkg != null) {
            filesInPackages.put(file, pkg);
        }
        return pkg;
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
                    System.out.println(cmd[0] + " failed to execute successfully");
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

        public void setResult(String result) {
            this.result = result;
        }
    }

    public static void main(String[] args) {
        if (args.length == 0 || "-h".equals(args[0]) || "--help".equals(args[0])) {
            System.out.println("Purpose: Solve the dependencies in the POM(s).");
            System.out.println("Usage: [option]");
            System.out.println("");
            System.out.println("Options:");
            System.out.println("  -v, --verbose: be extra verbose");
            System.out.println("  -p<package>, --package=<package>: name of the Debian package containing");
            System.out.println("    this library");
//            System.out.println("  -r<rules>, --rules=<rules>: path to the file containing the");
//            System.out.println("    extra rules to apply when cleaning the POM");
//            System.out.println("  -i<rules>, --published-rules=<rules>: path to the file containing the");
//            System.out.println("    extra rules to publish in the property debian.mavenRules in the cleaned POM");
            System.out.println("  --ant: use ant for the packaging");
            System.out.println("  --run-tests: run the unit tests");
            System.out.println("  --generate-javadoc: generate Javadoc");
            System.out.println("  --non-interactive: non interactive session");
            System.out.println("  --offline: offline mode for Debian build compatibility");
            System.out.println("  -m<repo root>--maven-repo=<repo root>: location of the Maven repository,");
            System.out.println("    used to force the versions of the Maven plugins used in the current");
            System.out.println("    POM file with the versions found in the repository");
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
        File mavenRepo = null;
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
                solver.setInteractive(false);
            } else if (arg.equals("--offline")) {
                solver.setOffline(true);
            } else if (arg.startsWith("-m")) {
                mavenRepo = new File(arg.substring(2));
            } else if (arg.startsWith("--maven-repo=")) {
                mavenRepo = new File(arg.substring("--maven-repo=".length()));
            }
            i = inc(i, args);
        }
        File poms = new File(solver.getOutputDirectory(), debianPackage + ".poms");

        solver.setPackageName(debianPackage);
        solver.setPackageType(packageType);
        solver.setExploreProjects(true);
        solver.setListOfPoms(poms);

        if (mavenRepo != null) {
            Repository repository = new Repository(mavenRepo);
            solver.getPomTransformer().setRepository(repository);
            solver.getPomTransformer().usePluginVersionsFromRepository();
        }

        if (verbose) {
            String msg = "Solving dependencies for package " + debianPackage;
            if (solver.isRunTests()) {
                msg += " (tests are included)";
            }
            if (solver.isGenerateJavadoc()) {
                msg += " (documentation is included)";
            }
            System.out.println(msg);
            solver.setVerbose(true);
        }

        solver.solveDependencies();

        solver.saveListOfPoms();
        solver.saveMavenRules();
        solver.saveMavenIgnoreRules();
        solver.saveMavenCleanIgnoreRules();
        solver.saveMavenPublishedRules();
        solver.saveSubstvars();

        if (!solver.getIssues().isEmpty()) {
            System.exit(1);
        }
    }

    private static int inc(int i, String[] args) {
        do {
            i++;
        } while (i < args.length && args[i].isEmpty());
        return i;
    }
}
