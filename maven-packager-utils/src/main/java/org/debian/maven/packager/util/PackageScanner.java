/*
 * Copyright 2012 Ludovic Claude.
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

package org.debian.maven.packager.util;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.debian.maven.packager.DebianDependency;
import org.debian.maven.repo.Dependency;

public class PackageScanner {

    private final boolean offline;
    // Keep the list of known files and their package
    private Map<File, String> filesInPackages = new HashMap<File, String>();
    private Map<String, List<String>> cacheOfSharedJars = new HashMap<String, List<String>>();

    public PackageScanner(boolean offline) {
        this.offline = offline;
    }

    public PackageScanner newInstanceWithFreshCaches() {
        return new PackageScanner(offline);
    }

    public DebianDependency searchPkg(File dir, String extension) {
        // lookup the cache first
        File cacheId = new File(dir, "<ANY>" + extension);        
        if (filesInPackages.containsKey(cacheId)) {
            return new DebianDependency(filesInPackages.get(cacheId));
        }

        GetPackageContainingPatternResult packageResult = new GetPackageContainingPatternResult(extension);

        IOUtil.executeProcess(new String[]{"dpkg", "--search", dir.getAbsolutePath() + "/*/*"}, packageResult);

        if (!packageResult.getPackages().isEmpty()) {
            String pkg = packageResult.getPackages().iterator().next();
            filesInPackages.put(cacheId, pkg);
            return new DebianDependency(pkg);
        }

        // Debian policy prevents the use of apt-file during a build
        if (offline) {
            System.err.println("Offline mode. Give up looking for package containing " + dir);
            return null;
        }

        if (!new File("/usr/bin/apt-file").exists()) {
            System.err.println("/usr/bin/apt-file not found. Give up looking for package containing " + dir);
            return null;
        }
        IOUtil.executeProcess(new String[]{"apt-file", "search", dir.getAbsolutePath()}, packageResult);
        if (!packageResult.getPackages().isEmpty()) {
            String pkg = packageResult.getPackages().iterator().next();
            filesInPackages.put(cacheId, pkg);
            new DebianDependency(pkg);
        }
        
        return null; 
    }


    public DebianDependency searchPkgContainingPom(Dependency dependency) {
        // TODO shouldn't we use the mavenRepo property of DependencySolver for the mavenRepo path?
        return searchPkg(new File("/usr/share/maven-repo/" + dependency.getGroupId().replace('.', '/')
            + "/" + dependency.getArtifactId()) , ".pom");
    }

    public DebianDependency searchPkgContainingJar(Dependency dependency) {
        return searchPkg(new File("/usr/share/java/" + dependency.getArtifactId() + ".jar"));
    }

    /**
     * Searches the package containing the Javadoc for the specified package.
     * 
     * @param dependency
     */
    public DebianDependency searchJavaDocPkg(DebianDependency dependency) {
        DebianDependency pkg = searchPkg(new File("/usr/share/doc/" + dependency.getPackageName() + "/api/index.html"));
        if (pkg == null) {
            pkg = searchPkg(new File("/usr/share/doc/" + dependency.getPackageName() + "-doc/api/index.html"));
        }
        if (pkg == null) {
            pkg = searchPkg(new File("/usr/share/doc/" + dependency.getPackageName() + "/apidocs/index.html"));
        }
        if (pkg == null) {
            pkg = searchPkg(new File("/usr/share/doc/" + dependency.getPackageName() + "-doc/apidocs/index.html"));
        }
        return pkg;
    }

    public DebianDependency searchPkg(File fileToSearch) {
        // lookup the cache first
        if (filesInPackages.containsKey(fileToSearch)) {
            return new DebianDependency(filesInPackages.get(fileToSearch));
        }

        GetPackageResult packageResult = new GetPackageResult();

        IOUtil.executeProcess(new String[]{"dpkg", "--search", fileToSearch.getAbsolutePath()}, packageResult);
        if (!packageResult.getResult().isEmpty()) {
            String pkg = packageResult.getResult().iterator().next();
            filesInPackages.put(fileToSearch, pkg);
            return new DebianDependency(pkg);
        }

        // Debian policy prevents the use of apt-file during a build
        if (offline) {
            System.err.println("Offline mode. Give up looking for package containing " + fileToSearch);
            return null;
        }

        if (!new File("/usr/bin/apt-file").exists()) {
            System.err.println("/usr/bin/apt-file not found. Give up looking for package containing " + fileToSearch);
            return null;
        }
        IOUtil.executeProcess(new String[]{"apt-file", "search", fileToSearch.getAbsolutePath()}, packageResult);
        if (!packageResult.getResult().isEmpty()) {
            String pkg = packageResult.getResult().iterator().next();
            filesInPackages.put(fileToSearch, pkg);
            return new DebianDependency(pkg);
        }
        
        return null;
    }

    public String getPackageVersion(DebianDependency pkg, boolean onlyInstalled) {
        GetPackageVersionResult packageResult = new GetPackageVersionResult();
        IOUtil.executeProcess(new String[]{"dpkg", "--status", pkg.getPackageName()}, packageResult);
        if (packageResult.getResult() != null) {
            return packageResult.getResult();
        }
        if (!onlyInstalled) {
            GetChangelogVersionResult versionResult = new GetChangelogVersionResult(pkg.getPackageName());
            IOUtil.executeProcess(new String[]{"apt-get", "--no-act", "--verbose-versions", "install", pkg.getPackageName()}, versionResult);
            if (versionResult.getResult() != null) {
                return versionResult.getResult();
            }
        }
        return null;
    }

    public List<String> listSharedJars(String library) {
        if (cacheOfSharedJars.get(library) != null) {
            return cacheOfSharedJars.get(library);
        }

        final List<String> jars = new ArrayList<String>();
        if (library.indexOf("(") > 0) {
            library = library.substring(0, library.indexOf("(")).trim();
        }
        System.out.println();
        System.out.println("Looking for shared jars in package " + library + "...");
        IOUtil.executeProcess(new String[]{"dpkg", "--listfiles", library}, new SharedJarOutputHandler(jars));
        cacheOfSharedJars.put(library, jars);
        return jars;
    }

    public List<DebianDependency> addDocDependencies(Collection<DebianDependency> debianDeps, Map<DebianDependency,
        Dependency> versionedPackagesAndDependencies) {
        List<DebianDependency> docDeps = new ArrayList<DebianDependency>();
        for (DebianDependency dependency : debianDeps) {
            Dependency runtimeDependency = versionedPackagesAndDependencies.get(dependency);
            if (runtimeDependency != null && runtimeDependency.isPom()) {
                continue;
            }
            DebianDependency docPkg = searchJavaDocPkg(dependency);
            if (docPkg != null) {
                docDeps.add(docPkg);
            }
        }
        return docDeps;
    }

}
