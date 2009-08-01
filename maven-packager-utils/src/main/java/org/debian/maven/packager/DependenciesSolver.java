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

    protected File baseDir;
    protected File outputDirectory;
    protected String packageName;
    protected String packageType;
    protected File mavenRepo = new File("/usr/share/maven-repo");
    protected boolean exploreProjects;
    protected List projects = new ArrayList();

    private Repository repository;
    private List issues = new ArrayList();
    private List pomsConfig = new ArrayList();
    private Set compileDepends = new TreeSet();
    private Set testDepends = new TreeSet();
    private Set runtimeDepends = new TreeSet();
    private Set optionalDepends = new TreeSet();
    private Set rules = new TreeSet();
    private boolean checkedAptFile;

    public File getBaseDir() {
        return baseDir;
    }

    public void saveListOfPoms() {
        File listPoms = new File(outputDirectory, packageName + ".poms");
        if (!listPoms.exists()) {
            try {
                PrintWriter out = new PrintWriter(new FileWriter(listPoms));
                for (Iterator i = pomsConfig.iterator(); i.hasNext();) {
                    String config = (String) i.next();
                    out.println(config);
                }
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
                for (Iterator i = rules.iterator(); i.hasNext();) {
                    String rule = (String) i.next();
                    out.println(rule);
                }
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
        docRuntimeDepends.add("openjdk-6-doc | classpath-doc");
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

        for (Iterator i = issues.iterator(); i.hasNext();) {
            String issue = (String) i.next();
            System.err.println(issue);
        }
    }

    private void resolveDependencies(File projectPom) {
        POMReader reader = new POMReader();
        String pomRelPath = projectPom.getAbsolutePath().substring(baseDir.getAbsolutePath().length() + 1);
        try {
            POMInfo pom = reader.readPom(projectPom);
            repository.registerPom(projectPom, pom);

            boolean noParent = false;
            if (pom.getParent() != null) {
                POMInfo parentPom = repository.searchMatchingPOM(pom.getParent());
                if (parentPom == null) {
                    noParent = true;
                }
                if (!baseDir.equals(projectPom.getParentFile())) {
                    System.out.println("Check the parent dependency in the sub project " + projectPom);
                    Set parentDependencies = new TreeSet();
                    parentDependencies.add(pom.getParent());
                    resolveDependencies(parentDependencies, false, false);
                }
            }

            if (noParent) {
                pomsConfig.add(pomRelPath + " --no-parent");
            } else {
                pomsConfig.add(pomRelPath);
            }

            resolveDependencies(pom.getDependencies(), false, false);
            resolveDependencies(pom.getPlugins(), true, true);
            resolveDependencies(pom.getPluginDependencies(), true, true);
            resolveDependencies(pom.getExtensions(), true, true);

            if (exploreProjects) {
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

    private void resolveDependencies(Collection poms, boolean buildTime, boolean mavenExtension) {
        for (Iterator i = poms.iterator(); i.hasNext();) {
            Dependency dependency = (Dependency) i.next();
            POMInfo pom = repository.searchMatchingPOM(dependency);
            if (pom == null && "maven-plugin".equals(dependency.getType())) {
                List matchingPoms = repository.searchMatchingPOMsIgnoreVersion(dependency);
                if (matchingPoms.size() > 1) {
                    issues.add("More than one version matches the plugin " + dependency.getGroupId() + ":" +
                        dependency.getArtifactId() + ":" + dependency.getVersion());
                }
                if (!matchingPoms.isEmpty()) {
                    pom = (POMInfo) matchingPoms.get(0);
                    // Adapt the version of the plugin to what is in the repository
                    rules.add(dependency.getGroupId() + " " +
                        dependency.getArtifactId() + " maven-plugin s/.*/" + pom.getOriginalVersion() + "/");
                }
            }
            if (pom == null) {
                issues.add("Dependency is not packaged in the Maven repository for Debian: " + dependency.getGroupId() + ":" +
                        dependency.getArtifactId() + ":" + dependency.getVersion());
                return;
            }
            String library = null;
            if (pom.getProperties() != null) {
                library = (String) pom.getProperties().get("debian.package");
            }
            if (library == null) {
                issues.add("Dependency is missing the Debian properties in its POM: " + dependency.getGroupId() + ":" +
                        dependency.getArtifactId() + ":" + dependency.getVersion());
                File pomFile = new File(mavenRepo, dependency.getGroupId().replace(".", "/") + "/" + dependency.getArtifactId() + "/" + dependency.getVersion() + "/" + dependency.getArtifactId() + "-" + dependency.getVersion() + ".pom");
                library = searchPkg(pomFile);
            }
            if (library != null) {
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
                    } else if ("compile".equals(dependency.getScope())) {
                        compileDepends.add(library);
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
            } else if (arg.startsWith("--ant")) {
                packageType = "ant";
            }
            i = inc(i, args);
        }

        solver.setPackageName(debianPackage);
        solver.setPackageType(packageType);
        solver.setExploreProjects(true);

        if (verbose) {
            System.out.println("Solving dependencies for package " + debianPackage);
        }

        solver.solveDependencies();

        solver.saveListOfPoms();
        solver.saveMavenRules();
        solver.saveSubstvars();
    }

    private static int inc(int i, String[] args) {
        do {
            i++;
        } while (i < args.length && args[i].isEmpty());
        return i;
    }

}
