package org.debian.maven.packager;

/*
 * Copyright 2009 Ludovic Claude.
 * Copyright 2011 Damien Raude-Morvan.
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamException;

import org.debian.maven.packager.util.*;
import org.debian.maven.repo.Dependency;
import org.debian.maven.repo.DependencyNotFoundException;
import org.debian.maven.repo.DependencyRule;
import org.debian.maven.repo.DependencyRuleSet;
import org.debian.maven.repo.ListOfPOMs;
import org.debian.maven.repo.POMHandler;
import org.debian.maven.repo.POMInfo;
import org.debian.maven.repo.POMInfo.DependencyType;
import org.debian.maven.repo.POMTransformer;
import org.debian.maven.repo.Repository;
import org.debian.maven.repo.Rule;
import org.debian.maven.util.Strings;

/**
 * Analyze the Maven dependencies and extract the Maven rules to use
 * as well as the list of dependent packages.
 *
 * @author Ludovic Claude
 */
public class DependenciesSolver {

    private static final Logger log = Logger.getLogger(DependenciesSolver.class.getName());
    private static final UserInteraction userInteraction = new UserInteraction();
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
        {"org.apache.maven.plugins", "maven-enforcer-plugin"},
        {"org.apache.maven.plugins", "maven-gpg-plugin"},
        {"org.apache.rat", "apache-rat-plugin"},
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

    private File baseDir;
    final POMTransformer pomTransformer = new POMTransformer();
    private File outputDirectory;
    String packageName;
    String packageType;
    private String packageVersion;
    File mavenRepo = new File("/usr/share/maven-repo");
    // explore (search) for additional pom files or look only for those defined in debian/*.poms?
    boolean exploreProjects;
    private Repository repository;
    List<String> issues = new ArrayList<String>();
    private List<Dependency> projectPoms = new ArrayList<Dependency>();
    private List<ToResolve> toResolve = new ArrayList<ToResolve>();
    private Set<Dependency> knownProjectDependencies = new TreeSet<Dependency>();
    private Set<Dependency> ignoredDependencies = new TreeSet<Dependency>();
    private Set<Dependency> notIgnoredDependencies = new TreeSet<Dependency>();
    private Set<String> compileDepends = new TreeSet<String>();
    private Set<String> testDepends = new TreeSet<String>();
    private Set<String> runtimeDepends = new TreeSet<String>();
    private Set<String> optionalDepends = new TreeSet<String>();
    private DependencyRuleSet cleanIgnoreRules = new DependencyRuleSet("Ignore rules to be applied during the Maven clean phase",
            new File("debian/maven.cleanIgnoreRules"));
    private boolean offline;
    boolean runTests;
    boolean generateJavadoc;
    boolean interactive = true;
    private boolean askedToFilterModules = false;
    private boolean filterModules = false;
    boolean verbose = false;
    private Map<String, POMInfo> pomInfoCache = new HashMap<String, POMInfo>();
    // Keep the original POMs for reference
    private Map<String, POMInfo> originalPomInfoCache = new HashMap<String, POMInfo>();
    // Keep the previous selected rule for a given version
    private Map<String, Rule> versionToRules = new HashMap<String, Rule>();
    // Keep the list of packages and dependencies
    private Map<String, Dependency> versionedPackagesAndDependencies = new HashMap<String, Dependency>();
    private List<Rule> defaultRules = new ArrayList<Rule>();
    private PackageScanner scanner = new PackageScanner();

    public DependenciesSolver() {
        pomTransformer.setVerbose(true);
        pomTransformer.setFixVersions(false);
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

        Rule toDebianRule = new Rule("s/.*/debian/", "Change the version to the symbolic 'debian' version");
        Rule keepVersionRule = new Rule("*", "Keep the version");
        Rule customRule = new Rule("CUSTOM", "Custom rule");
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

    public void setOffline(boolean offline) {
        this.offline = offline;
        scanner.setOffline(offline);
    }

    private boolean containsPlugin(String[][] pluginDefinitions, Dependency plugin) {
        for (String[] pluginDefinition : pluginDefinitions) {
            if (!plugin.getGroupId().equals(pluginDefinition[0])) {
                continue;
            }
            if (plugin.getArtifactId().equals(pluginDefinition[1])) {
                return true;
            }
        }
        return false;
    }

    private boolean isDefaultMavenPlugin(Dependency dependency) {
        if (getRepository() != null && getRepository().getSuperPOM() != null) {
            for (Dependency defaultPlugin : getRepository().getSuperPOM().getDependencies().get(DependencyType.PLUGIN_MANAGEMENT)) {
                if (defaultPlugin.equalsIgnoreVersion(dependency)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean askIgnoreDependency(String sourcePomLoc, Dependency dependency, String message) {
        return askIgnoreDependency(sourcePomLoc, dependency, message, true);
    }

    private boolean askIgnoreDependency(String sourcePomLoc, Dependency dependency, String message, boolean defaultToIgnore) {
        if (!interactive || notIgnoredDependencies.contains(dependency)) {
            return false;
        }
        String q = "\n" + "In " + sourcePomLoc + ":"
                 + message
                 + "  " + dependency;
        boolean ignore = userInteraction.askYesNo(q, defaultToIgnore);
        if (!ignore) {
            notIgnoredDependencies.add(dependency);
        }
        return ignore;
    }

    private class ToResolve {

        private final File sourcePom;
        private final DependencyType listType;
        private final boolean buildTime;
        private final boolean mavenExtension;
        private final boolean management;

        private ToResolve(File sourcePom, DependencyType listType, boolean buildTime, boolean mavenExtension, boolean management) {
            this.sourcePom = sourcePom;
            this.listType = listType;
            this.buildTime = buildTime;
            this.mavenExtension = mavenExtension;
            this.management = management;
        }

        public void resolve() {
            try {
                POMInfo pom = getPOM(sourcePom);
                List<Dependency> dependenciesByType = pom.getDependencies().get(listType);

                for (Dependency dependency : dependenciesByType) {
                    resolveDependency(dependency, sourcePom, buildTime, mavenExtension, management, false);
                }
            } catch (DependencyNotFoundException e) {
                log.log(Level.SEVERE, "Cannot resolve dependencies in " + sourcePom + ": " + e.getMessage());
            } catch (Exception e) {
                log.log(Level.SEVERE, "Cannot resolve dependencies in " + sourcePom + ": " + e.getMessage(), e);
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
                log.log(Level.SEVERE, "Error while reading file " + dependencies, ex);
            }
        }
        depVars.put("maven.CompileDepends", Strings.join(compileDepends, ", "));
        depVars.put("maven.TestDepends", Strings.join(testDepends, ", "));
        depVars.put("maven.Depends", Strings.join(runtimeDepends, ", "));
        depVars.put("maven.OptionalDepends", Strings.join(optionalDepends, ", "));
        if (generateJavadoc) {
            System.out.println("Checking dependencies for documentation packages...");
            Set<String> docRuntimeDepends = new TreeSet<String>();
            docRuntimeDepends.add("default-jdk-doc");
            for (String dependency : runtimeDepends) {
                Dependency runtimeDependency = versionedPackagesAndDependencies.get(dependency);
                if (dependency.indexOf(' ') > 0) {
                    dependency = dependency.substring(0, dependency.indexOf(' '));
                }
                if (runtimeDependency != null && "pom".equals(runtimeDependency.getType())) {
                    continue;
                }
                String docPkg = scanner.searchPkg(new File("/usr/share/doc/" + dependency + "/api/index.html"));
                if (docPkg != null) {
                    docRuntimeDepends.add(docPkg);
                }
            }
            Set<String> docOptionalDepends = new TreeSet<String>();
            for (String dependency : optionalDepends) {
                Dependency optionalDependency = versionedPackagesAndDependencies.get(dependency);
                if (dependency.indexOf(' ') > 0) {
                    dependency = dependency.substring(0, dependency.indexOf(' '));
                }
                if (optionalDependency != null && "pom".equals(optionalDependency.getType())) {
                    continue;
                }
                String docPkg = scanner.searchPkg(new File("/usr/share/doc/" + dependency + "/api/index.html"));
                if (docPkg != null) {
                    docOptionalDepends.add(docPkg);
                }
            }
            depVars.put("maven.DocDepends", Strings.join(docRuntimeDepends, ", "));
            depVars.put("maven.DocOptionalDepends", Strings.join(docOptionalDepends, ", "));
        }
        if (packageVersion != null) {
            depVars.put("maven.UpstreamPackageVersion", packageVersion);
        }
        // Write everything to debian/substvars
        try {
            FileWriter fstream = new FileWriter(dependencies);
            BufferedWriter out = new BufferedWriter(fstream);
            out.write("#List of dependencies for " + packageName + ", generated for use by debian/control");
            out.write("\n");
            Set<String> propertiesNames = depVars.stringPropertyNames();
            if (propertiesNames != null) {
                for (String propName : propertiesNames) {
                    out.write(Strings.propertyLine(propName, depVars.get(propName).toString()));
                }
            }
            out.close();
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

    public void setOutputDirectory(File outputDirectory) {
        this.outputDirectory = outputDirectory;
        File rulesFile = new File(outputDirectory, "maven.rules");
        if (rulesFile.exists() && verbose) {
            System.out.println("Use existing rules:");
        }
        pomTransformer.getRules().setRulesFile(rulesFile);

        File ignoreRulesFile = new File(outputDirectory, "maven.ignoreRules");
        if (ignoreRulesFile.exists() && verbose) {
            System.out.println();
            System.out.println("Use existing ignore rules:");
        }
        pomTransformer.getIgnoreRules().setRulesFile(ignoreRulesFile);

        File publishedRulesFile = new File(outputDirectory, "maven.publishedRules");
        if (publishedRulesFile.exists() && verbose) {
            System.out.println();
            System.out.println("Use existing published rules:");
        }
        pomTransformer.getPublishedRules().setRulesFile(publishedRulesFile);

        File cleanIgnoreRules = new File(outputDirectory, "maven.cleanIgnoreRules");
        if (cleanIgnoreRules.exists() && verbose) {
            System.out.println();
            System.out.println("Use existing clean ignore rules:");
        }
        this.cleanIgnoreRules.setRulesFile(cleanIgnoreRules);

        if (verbose) {
            System.out.println();
        }
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

        for (ToResolve tr : toResolve) {
            tr.resolve();
        }

        if (!issues.isEmpty()) {
            System.err.println("ERROR:");
            for (String issue : issues) {
                System.err.println(issue);
            }
            System.err.println("--------");
        }
    }

    private void resolveDependencies(File projectPom) {

        if (pomTransformer.getListOfPOMs().getOrCreatePOMOptions(projectPom) != null && pomTransformer.getListOfPOMs().getOrCreatePOMOptions(projectPom).isIgnore()) {
            return;
        }

        String pomRelPath = projectPom.getAbsolutePath().substring(baseDir.getAbsolutePath().length() + 1);
        System.out.println("Analysing " + pomRelPath + "...");

        try {
            POMInfo pom = getPOM(projectPom);
            pom.setProperties(new HashMap<String, String>());
            pom.getProperties().put("debian.package", packageName);

            if (pomTransformer.getListOfPOMs().getOrCreatePOMOptions(projectPom).isNoParent()) {
                pom.setParent(null);
            } else if (pom.getParent() != null && !pom.getParent().isSuperPom()) {
                boolean oldNoParent = pomTransformer.getListOfPOMs().getOrCreatePOMOptions(projectPom).isNoParent();
                // Don't mark the parent dependency as 'build time' dependency because once installed,
                // the POM for this project will always need the parent POM to be available
                Dependency parent = resolveDependency(pom.getParent(), projectPom, false, false, false, true);
                // The user may have set or unset the --no-parent option, if so we update the POM to include or not the
                // parent according to the user's choice
                if (pomTransformer.getListOfPOMs().getOrCreatePOMOptions(projectPom).isNoParent() != oldNoParent) {
                    pomInfoCache.remove(projectPom.getAbsolutePath());
                    pom = getPOM(projectPom);
                }
                pom.setParent(parent);
                // If the parent is found, search the parent POM and update current POM 
                if (parent != null) {
                    POMInfo parentPOM = getRepository().searchMatchingPOM(parent);
                    pom.setParentPOM(parentPOM);
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
                String q = "Enter the upstream version for the package. If you press <Enter> it will default to " + pom.getOriginalVersion();
                String v = userInteraction.ask(q);
                if (v.isEmpty()) {
                    v = pom.getOriginalVersion();
                }
                packageVersion = v;
            }

            if (pom.getOriginalVersion().equals(packageVersion)) {
                pom.getProperties().put("debian.hasPackageVersion", "true");
                pomTransformer.getListOfPOMs().getOrCreatePOMOptions(projectPom).setHasPackageVersion(true);
            }

            if (filterModules) {
                boolean includeModule = userInteraction.askYesNo("Include the module " + pomRelPath + " ?", true);
                if (!includeModule) {
                    pomTransformer.getListOfPOMs().getOrCreatePOMOptions(projectPom).setIgnore(true);
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

            projectPoms.add(pom.getThisPom());
            
            // Previous rule from another run
            boolean explicitlyMentionedInRules = false;
            for (DependencyRule previousRule : pomTransformer.getRules().findMatchingRules(pom.getThisPom())) {
                if (!previousRule.explicitlyMentions(pom.getThisPom())) {
                    explicitlyMentionedInRules = true;
                    break;
                }
            }

            if (interactive && !explicitlyMentionedInRules && !"maven-plugin".equals(pom.getThisPom().getType())) {
                String version = pom.getThisPom().getVersion();
                String question = "\n"
                    + "Version of " + pom.getThisPom().getGroupId() + ":"
                    + pom.getThisPom().getArtifactId() + " is " + version
                    + "Choose how it will be transformed:";

                List<Rule> choices = new ArrayList<Rule>();

                if (versionToRules.containsKey(version)) {
                    choices.add(versionToRules.get(version));
                }

                Pattern p = Pattern.compile("(\\d+)(\\..*)");
                Matcher matcher = p.matcher(version);
                if (matcher.matches()) {
                    String mainVersion = matcher.group(1);
                    Rule mainVersionRule = new Rule("s/" + mainVersion + "\\..*/" + mainVersion + ".x/",
                        "Replace all versions starting by " + mainVersion + ". with " + mainVersion + ".x");
                    if (!choices.contains(mainVersionRule)) {
                        choices.add(mainVersionRule);
                    }
                }
                for (Rule rule : defaultRules) {
                    if (!choices.contains(rule)) {
                        choices.add(rule);
                    }
                }

                List<String> choicesDescriptions = new ArrayList<String>();
                for(Rule choice : choices) {
                    choicesDescriptions.add(choice.getDescription());
                }
                int choice = userInteraction.askChoices(question, 0, choicesDescriptions);
                Rule selectedRule = choices.get(choice - 1);
                versionToRules.put(version, selectedRule);
                if (selectedRule.getPattern().equals("CUSTOM")) {
                    String s = userInteraction.ask("Enter the pattern for your custom rule (in the form s/regex/replace/)")
                               .toLowerCase();
                    selectedRule = new Rule(s, "My custom rule " + s);
                    defaultRules.add(selectedRule);
                }

                String dependencyRule = pom.getThisPom().getGroupId() + " " + pom.getThisPom().getArtifactId()
                        + " " + pom.getThisPom().getType() + " " + selectedRule.toString();
                pomTransformer.getRules().add(new DependencyRule(dependencyRule));
                POMInfo transformedPom = pom.newPOMFromRules(pomTransformer.getRules().getRules(), getRepository());
                getRepository().registerPom(projectPom, transformedPom);
                projectPoms.add(transformedPom.getThisPom());

                if ("bundle".equals(pom.getThisPom().getType())) {
                    String question2 = pom.getThisPom().getGroupId() + ":" + pom.getThisPom().getArtifactId() +
                            " is a bundle.\n"
                            + "Inform mh_make that dependencies of type jar which may match this library should be transformed into bundles automatically?";

                    boolean transformJarsIntoBundle = userInteraction.askYesNo(question2, true);

                    if (transformJarsIntoBundle) {
                        String transformBundleRule = pom.getThisPom().getGroupId() + " " + pom.getThisPom().getArtifactId()
                                + " s/jar/bundle/ " + selectedRule.toString();
                        pomTransformer.getPublishedRules().add(new DependencyRule(transformBundleRule));
                    }
                }
            }

            if (pom.getParent() != null && !pom.getParent().isSuperPom()) {
                POMInfo parentPom = getRepository().searchMatchingPOM(pom.getParent());
                if (parentPom == null || parentPom.equals(getRepository().getSuperPOM())) {
                    pomTransformer.getListOfPOMs().getOrCreatePOMOptions(projectPom).setNoParent(true);
                }
                if (!baseDir.equals(projectPom.getParentFile())) {
                    System.out.println("Checking the parent dependency in the sub project " + pomRelPath);
                    resolveDependency(pom.getParent(), projectPom, false, false, false, true);
                }
            }

            toResolve.add(new ToResolve(projectPom, DependencyType.DEPENDENCIES, false, false, false));
            toResolve.add(new ToResolve(projectPom, DependencyType.DEPENDENCY_MANAGEMENT_LIST, false, false, true));
            toResolve.add(new ToResolve(projectPom, DependencyType.PLUGINS, true, true, false));
            toResolve.add(new ToResolve(projectPom, DependencyType.PLUGIN_DEPENDENCIES, true, true, false));
            toResolve.add(new ToResolve(projectPom, DependencyType.PLUGIN_MANAGEMENT, true, true, true));
            toResolve.add(new ToResolve(projectPom, DependencyType.REPORTING_PLUGINS, true, true, false));
            toResolve.add(new ToResolve(projectPom, DependencyType.EXTENSIONS, true, true, false));

            if (exploreProjects && !pom.getModules().isEmpty()) {
                if (interactive && !askedToFilterModules) {
                    filterModules = !userInteraction.askYesNo("This project contains modules. Include all modules?", true);
                    askedToFilterModules = true;
                }
                for (String module : pom.getModules()) {
                    File modulePom = new File(projectPom.getParent(), module + "/pom.xml");
                    resolveDependencies(modulePom);
                }
            }
        } catch (Exception ex) {
            log.log(Level.SEVERE, "Error while resolving " + projectPom + ": " + ex.getMessage());
            log.log(Level.SEVERE, "", ex);
            System.exit(1);
        }
    }

    private POMInfo getPOM(File projectPom) throws XMLStreamException, IOException {
        POMInfo info = pomInfoCache.get(projectPom.getAbsolutePath());
        if (info != null) {
            return info;
        }
        File tmpDest = File.createTempFile("pom", ".tmp", baseDir);
        tmpDest.deleteOnExit();
        ListOfPOMs.POMOptions options = pomTransformer.getListOfPOMs().getOrCreatePOMOptions(projectPom);
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

    private POMInfo getOriginalPOM(File projectPom) throws XMLStreamException, IOException {
        POMInfo info = originalPomInfoCache.get(projectPom.getAbsolutePath());
        if (info != null) {
            return info;
        }

        info = pomTransformer.readPom(projectPom);
        originalPomInfoCache.put(projectPom.getAbsolutePath(), info);
        return info;
    }

    private Dependency resolveDependency(Dependency dependency, File sourcePom, boolean buildTime, boolean mavenExtension, boolean management, boolean resolvingParent) throws DependencyNotFoundException {

        if (containsDependencyIgnoreVersion(knownProjectDependencies, dependency)) {
            return dependency;                 
        }

        if (containsDependencyIgnoreVersion(ignoredDependencies, dependency) ||
                (management && isDefaultMavenPlugin(dependency))) {
            return null;
        }

        if (resolvingParent && dependency.isSuperPom()) {
            return dependency;
        }

        String sourcePomLoc = sourcePom.getName();
        if (verbose) {
            String msg = "Resolving " + dependency;
            if (dependency.getScope() != null) {
                msg += " of scope " + dependency.getScope();
            }
            System.out.println(msg + "...");
        }

        // First let the packager mark as ignored those dependencies which should be ignored
        boolean ignoreDependency = false;
        if (!ignoreDependency && containsPlugin(PLUGINS_TO_IGNORE, dependency)) {
            ignoreDependency = askIgnoreDependency(sourcePomLoc, dependency, "This plugin is not useful for the build or its use is against Debian policies. Ignore this plugin?");
        }
        if (!ignoreDependency && containsPlugin(EXTENSIONS_TO_IGNORE, dependency)) {
            ignoreDependency = askIgnoreDependency(sourcePomLoc, dependency, "This extension is not useful for the build or its use is against Debian policies. Ignore this extension?");
        }
        if (!ignoreDependency && containsPlugin(PLUGINS_THAT_CAN_BE_IGNORED, dependency)) {
            ignoreDependency = askIgnoreDependency(sourcePomLoc, dependency, "This plugin may be ignored in some cases. Ignore this plugin?");
        }
        if (!ignoreDependency && !runTests) {
            if ("test".equals(dependency.getScope())) {
                ignoreDependency = askIgnoreDependency(sourcePomLoc, dependency, "Tests are turned off. Ignore this test dependency?");
            } else if (containsPlugin(TEST_PLUGINS, dependency)) {
                ignoreDependency = askIgnoreDependency(sourcePomLoc, dependency, "Tests are turned off. Ignore this test plugin?");
            }
        }
        // is Documentation Or Report Plugin?
        if (!ignoreDependency && !generateJavadoc && containsPlugin(DOC_PLUGINS, dependency)) {
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
        String skipReason = "";
        if (packageType.equals("ant")) {
            if ("maven-plugin".equals(dependency.getType())) {
                try {
                    if (!getPOM(sourcePom).getThisPom().getType().equals("pom")) {
                        skipReason = "Maven plugins are not used during a build with Ant";
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (!runTests && "test".equals(dependency.getScope())) {
                skipReason = "Tests are not executed during the build";
            }
        }
        if (!skipReason.isEmpty()) {
            // Even if we skip the dependency, try to locate its associated maven rules,
            // as this may be useful later - but never fail if the dependency is not found.
            POMInfo pom = getRepository().searchMatchingPOM(dependency);
            if (pom != null) {
                String mavenRules = pom.getProperties().get("debian.mavenRules");
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
        try {
            if (pom == null && dependency.getVersion() == null) {
                POMInfo containerPom = getPOM(sourcePom);
                String version = containerPom.getVersionFromManagementDependency(dependency);
                dependency.setVersion(version);
                if (version != null) {
                    pom = getRepository().searchMatchingPOM(dependency);
                } else {
                    System.out.println("In " + sourcePomLoc + ", cannot find the version for dependency " + dependency + " from this POM or its parent POMs");
                    if (pomTransformer.getListOfPOMs().getOrCreatePOMOptions(sourcePom).isNoParent()) {
                        Dependency originalParent = getOriginalPOM(sourcePom).getParent();
                        System.out.println("[warning] Option --no-parent has been set for POM file " + sourcePomLoc + ", maybe it was not a good idea and you should first package the parent POM " + originalParent);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (pom == null && dependency.getVersion() != null) {
            List<POMInfo> poms = getRepository().searchMatchingPOMsIgnoreVersion(dependency);
            for (POMInfo potentialPom : poms) {
                String mavenRules = potentialPom.getProperties().get("debian.mavenRules");
                if (mavenRules != null) {
                    StringTokenizer st = new StringTokenizer(mavenRules, ",");
                    while (st.hasMoreTokens()) {
                        String ruleDef = st.nextToken().trim();
                        DependencyRule rule = new DependencyRule(ruleDef);
                        if (rule.matches(dependency) && rule.apply(dependency).equals(potentialPom.getThisPom())) {
                            pom = potentialPom;
                            pomTransformer.getRules().add(rule);
                        }
                    }
                }
            }
        }
        if (pom == null && dependency.getVersion() == null) {
            // Set a dummy version and try again
            for (int version = 0; version < 10; version++) {
                dependency.setVersion(version + ".0");
                pom = getRepository().searchMatchingPOM(dependency);
                if (pom != null) {
                    System.out.println("Use best guess version: " + dependency.getVersion() + " for "
                      + dependency.getGroupId() + ":" + dependency.getArtifactId());
                    break;
                }
                dependency.setVersion(null);
            }
        }

        if (pom == null && "maven-plugin".equals(dependency.getType())) {
            List<POMInfo> matchingPoms = getRepository().searchMatchingPOMsIgnoreVersion(dependency);
            if (matchingPoms.size() > 1) {
                issues.add(sourcePomLoc + ": More than one version matches the plugin " + dependency.getGroupId() + ":"
                        + dependency.getArtifactId() + ":" + dependency.getVersion());
            }
            if (!matchingPoms.isEmpty()) {
                pom = matchingPoms.get(0);
                // Don't add a rule to force the version of a Maven plugin, it's now done
                // automatically at build time
            }
        }

        // Ignore fast cases
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
            }
        }
        
        // In case we didn't find anything for "jar" packaging type, just check for a "bundle" type inside repository.
        // Some jars have been upgraded to OSGi bundles as OSGi metadata has been added to them.
        //
        // drazzib: I'm not sure this is really the right way to fix that (ie. maybe we should install "bundle" artifacts
        // directly with "jar" type inside Debian ?).
        //
        // ludovicc: a complex issue, I believe that libraries which evolve from a jar type to a bundle type should
        // inform packagers with a rule of the form
        // '<groupId> <artifactId> s/jar/bundle/ <versionRule>'
        // in other words, the packager of the library should add a published rule which will transform matching
        // libraries from jar type into bundle types, and apply as well the version substitution (for example to 2.x)
        // for Debian.
        //
        if (pom == null && "jar".equals(dependency.getType())) {
            if (verbose) {
                System.out.println("[check dependency with bundle type]");
            }
            Dependency bundleDependency = dependency.builder().setType("bundle").build();
            pom = getRepository().searchMatchingPOM(bundleDependency);
            if (pom != null) {
                dependency = bundleDependency;
                for (DependencyRule rule: pom.getPublishedRules()) {
                    if (rule.matches(dependency)) {
                        Dependency transformed = rule.apply(dependency);
                        if (transformed.getGroupId().equals(dependency.getGroupId())
                                && transformed.getArtifactId().equals(dependency.getArtifactId())
                                && transformed.getType().equals(dependency.getType())) {
                            String newRule = pom.getThisPom().getGroupId() + " " + pom.getThisPom().getArtifactId()
                                    + " s/jar/bundle/ " + rule.getVersionRule().toString();
                            pomTransformer.getRules().add(new DependencyRule(newRule));
                        }
                    }
                }
            }
        }

        if (pom == null) {
            if (!ignoreDependency) {
                if (resolvingParent) {
                    boolean noParent = askIgnoreDependency(sourcePomLoc, dependency,
                            "The parent POM cannot be found in the Maven repository for Debian. Ignore it?");
                    pomTransformer.getListOfPOMs().getOrCreatePOMOptions(sourcePom).setNoParent(noParent);
                    if (noParent) {
                        if (verbose) {
                            System.out.println("[no-parent]");
                        }
                        return null;
                    }
                } else if (containsPlugin(DOC_PLUGINS, dependency)) {
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
                String pkg = scanner.searchPkg(new File("/usr/share/maven-repo/"
                        + dependency.getGroupId().replace('.', '/')
                        + "/" + dependency.getArtifactId()), ".pom");
                if (pkg != null) {
                    String installedVersion = scanner.getPackageVersion(pkg, true);
                    if (installedVersion != null) {
                        System.out.println("[error] Package " + pkg + " (" + installedVersion + ") is already installed and contains a possible match," );
                        System.out.println("but I cannot resolve library " + dependency + " in it.");
                        System.out.println("[error] Please check manually that the library is up to date, otherwise it may be necessary to package version "
                                + dependency.getVersion() + " in Debian.");
                    } else {
                        System.out.println("[warning] Please install the missing dependency. Run the following command in another terminal:");
                        System.out.println("  sudo apt-get install " + pkg);
                    }
                }
                if (interactive && pkg == null) {
                    pkg = scanner.searchPkg(new File("/usr/share/java/" + dependency.getArtifactId() + ".jar"));
                    if (pkg != null) {
                        String q = "[error] Package " + pkg + " does not contain Maven dependency " + dependency + " but there seem to be a match\n"
                         + "If the package contains already Maven artifacts but the names don't match, try to enter a substitution rule\n"
                         + "of the form s/groupId/newGroupId/ s/artifactId/newArtifactId/ jar s/version/newVersion/ here:";
                        String newRule = userInteraction.ask(q);
                        if (!newRule.isEmpty()) {
                            DependencyRule userRule = new DependencyRule(newRule);
                            pomTransformer.getRules().add(userRule);
                            System.out.println("Please suggest the maintainer of package " + pkg + " to add this rule to debian/maven.publishedRules");
                            return resolveDependency(dependency.applyRules(Arrays.asList(userRule)), sourcePom, buildTime, mavenExtension, management, false);
                        }
                    } else {
                        String newRule = userInteraction.ask(
                                "[error] Cannot resolve Maven dependency " + dependency + ". If you know a package that contains a compatible dependency,\n"
                              + "try to enter a substitution rule of the form s/groupId/newGroupId/ s/artifactId/newArtifactId/ jar s/version/newVersion/ here:\n");
                        while (!newRule.isEmpty()) {
                            DependencyRule userRule = new DependencyRule(newRule);
                            Dependency newDependency = dependency.applyRules(Arrays.asList(userRule));
                            if (newDependency.equals(dependency)) {
                                newRule = userInteraction.ask("Your rule doesn't seem to apply on " + dependency
                                 + "Please enter a substitution rule of the form s/groupId/newGroupId/ s/artifactId/newArtifactId/ jar s/version/newVersion/ here,"
                                 + "or press <Enter> to give up");
                            } else {
                                pomTransformer.getRules().add(userRule);
                                System.out.println("Rescanning /usr/share/maven-repo...");
                                pomTransformer.getRepository().scan();
                                return resolveDependency(dependency.applyRules(Arrays.asList(userRule)), sourcePom, buildTime, mavenExtension, management, false);
                            }
                        }
                    }
                }
                if (interactive) {
                    boolean tryAgain = userInteraction.askYesNo("Try again to resolve the dependency?", true);
                    if (tryAgain) {
                        System.out.println("Rescanning /usr/share/maven-repo...");
                        pomTransformer.getRepository().scan();
                        // Clear caches
                        scanner = new PackageScanner();
                        scanner.setOffline(offline);
                        
                        return resolveDependency(dependency, sourcePom, buildTime, mavenExtension, management, false);
                    }
                }
                if (verbose) {
                    System.out.println("[error]");
                }
                throw new DependencyNotFoundException(dependency);
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
        String pkg = getPackage(pom, sourcePomLoc);

        if (pkg != null && !pkg.equals(packageName)) {
            String libraryWithVersionConstraint = pkg;
            String version = dependency.getVersion();
            if (version == null || (pom.getOriginalVersion() != null && version.compareTo(pom.getOriginalVersion()) > 0)) {
                version = pom.getOriginalVersion();
            }
            if (pom.getOriginalVersion() != null && (pom.getProperties().containsKey("debian.hasPackageVersion"))) {
                libraryWithVersionConstraint += " (>= " + version + ")";
            }
            if (!management) {
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
            }
            versionedPackagesAndDependencies.put(libraryWithVersionConstraint, dependency);
        }

        String mavenRules = pom.getProperties().get("debian.mavenRules");
        if (mavenRules != null) {
            StringTokenizer st = new StringTokenizer(mavenRules, ",");
            while (st.hasMoreTokens()) {
                String ruleDef = st.nextToken().trim();
                pomTransformer.getRules().add(new DependencyRule(ruleDef));
            }
        }
        if (verbose) {
            System.out.println("Dependency " + dependency + " found in package " + pkg);
            System.out.println("[ok]");
            System.out.println();
        }

        if (resolvingParent) {
            try {
                POMInfo containerPom = getPOM(sourcePom);
                containerPom.setParentPOM(pom);
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return pom.getThisPom();
    }

    private String getPackage(POMInfo pom, String sourcePomLoc) {
        String pkg = null;
        if (pom.getProperties() != null) {
            pkg = pom.getProperties().get("debian.package");
        }
        if (pkg == null) {
            Dependency dependency = pom.getThisPom();
            issues.add(sourcePomLoc + ": Dependency is missing the Debian properties in its POM: " + dependency.getGroupId() + ":"
                    + dependency.getArtifactId() + ":" + dependency.getVersion());
            File pomFile = new File(mavenRepo, dependency.getGroupId().replace(".", "/") + "/" + dependency.getArtifactId() + "/" + dependency.getVersion() + "/" + dependency.getArtifactId() + "-" + dependency.getVersion() + ".pom");
            pkg = scanner.searchPkg(pomFile);
        }
        return pkg;
    }

    private boolean containsDependencyIgnoreVersion(Collection<Dependency> dependencies, Dependency dependency) {
        for (Dependency ignoredDependency : dependencies) {
            if (ignoredDependency.equalsIgnoreVersion(dependency)) {
                return true;
            }
        }
        return false;
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
            System.out.println("  --base-directory: path to root directory of package");
            System.out.println("  --non-explore: doesn't explore directories for pom.xml");
            return;
        }
        
        // Default values
        boolean verbose = false;
        String debianPackage = "";
        String packageType = "maven";
        File mavenRepo = null;
        File baseDirectory = new File(".");
        
        DependenciesSolver solver = new DependenciesSolver();
        solver.exploreProjects = true; // can be overriden by args
        
        // Parse parameters
        int i = inc(-1, args);
        while (i < args.length && (args[i].trim().startsWith("-") || args[i].trim().isEmpty())) {
            String arg = args[i].trim();
            if ("--verbose".equals(arg) || "-v".equals(arg)) {
                verbose = true;
            } else if ("--debug".equals(arg)) {
                log.setLevel(Level.FINEST);
            } else if (arg.startsWith("-p")) {
                debianPackage = arg.substring(2);
            } else if (arg.startsWith("--package=")) {
                debianPackage = arg.substring("--package=".length());
            } else if (arg.equals("--ant")) {
                packageType = "ant";
            } else if (arg.equals("--run-tests")) {
                solver.runTests = true;
            } else if (arg.equals("--generate-javadoc")) {
                solver.generateJavadoc = true;
            } else if (arg.equals("--non-interactive")) {
                solver.interactive = false;
            } else if (arg.equals("--offline")) {
                solver.setOffline(true);
            } else if (arg.startsWith("-m")) {
                mavenRepo = new File(arg.substring(2));
            } else if (arg.startsWith("--maven-repo=")) {
                mavenRepo = new File(arg.substring("--maven-repo=".length()));
            } else if (arg.startsWith("-b")) {
                baseDirectory = new File(arg.substring(2));
            } else if (arg.startsWith("--base-directory=")) {
            	baseDirectory = new File(arg.substring("--base-directory=".length()));
            } else if (arg.equals("--non-explore")) {
            	solver.exploreProjects = false;
            }
            i = inc(i, args);
        }

        solver.setBaseDir(baseDirectory);
        solver.setOutputDirectory(new File(baseDirectory, "debian"));
        solver.packageName = debianPackage;
        solver.packageType = packageType;
        File poms = new File(solver.outputDirectory, debianPackage + ".poms");
        solver.setListOfPoms(poms);

        if (mavenRepo != null) {
            Repository repository = new Repository(mavenRepo);
            solver.pomTransformer.setRepository(repository);
            solver.pomTransformer.usePluginVersionsFromRepository();
        }

        if (verbose) {
            String msg = "Solving dependencies for package " + debianPackage;
            if (solver.runTests) {
                msg += " (tests are included)";
            }
            if (solver.generateJavadoc) {
                msg += " (documentation is included)";
            }
            System.out.println(msg);
            solver.verbose = true;
        }

        solver.solveDependencies();

        solver.pomTransformer.getListOfPOMs().save();
        solver.pomTransformer.getRules().save();
        solver.pomTransformer.getIgnoreRules().save();
        solver.cleanIgnoreRules.save();
        solver.pomTransformer.getPublishedRules().save();
        solver.saveSubstvars();

        if (!solver.issues.isEmpty()) {
            System.err.println("Some problems where found in this project, exiting...");
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
