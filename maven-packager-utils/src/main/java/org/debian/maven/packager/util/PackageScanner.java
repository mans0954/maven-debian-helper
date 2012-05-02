package org.debian.maven.packager.util;

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


import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PackageScanner {

    private boolean offline;
    // Keep the list of known files and their package
    private Map<File, String> filesInPackages = new HashMap<File, String>();
    private Map<String, List<String>> cacheOfSharedJars = new HashMap<String, List<String>>();

    public void setOffline(boolean offline) {
        this.offline = offline;
    }

    public String searchPkg(File dir, String extension) {
        GetPackageContainingPatternResult packageResult = new GetPackageContainingPatternResult(extension);
        File cacheId = new File(dir, "<ANY>" + extension);

        if (filesInPackages.containsKey(cacheId)) {
            return filesInPackages.get(cacheId);
        }

        IOUtil.executeProcess(new String[]{"dpkg", "--search", dir.getAbsolutePath() + "/*/*"}, packageResult);

        String pkg = null;
        if (!packageResult.getPackages().isEmpty()) {
            pkg = packageResult.getPackages().iterator().next();
            filesInPackages.put(cacheId, pkg);
            return pkg;
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
            pkg = packageResult.getPackages().iterator().next();
            filesInPackages.put(cacheId, pkg);
        }
        return pkg;

    }

    public String searchPkg(File fileToSearch) {
        GetPackageResult packageResult = new GetPackageResult();

        if (filesInPackages.containsKey(fileToSearch)) {
            return filesInPackages.get(fileToSearch);
        }

        String pkg = null;
        IOUtil.executeProcess(new String[]{"dpkg", "--search", fileToSearch.getAbsolutePath()}, packageResult);
        if (!packageResult.getResult().isEmpty()) {
            pkg = packageResult.getResult().iterator().next();
            filesInPackages.put(fileToSearch, pkg);
            return pkg;
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
            pkg = packageResult.getResult().iterator().next();
            filesInPackages.put(fileToSearch, pkg);
        }
        return pkg;
    }

    public String getPackageVersion(String pkg, boolean onlyInstalled) {
        GetPackageVersionResult packageResult = new GetPackageVersionResult();
        IOUtil.executeProcess(new String[]{"dpkg", "--status", pkg}, packageResult);
        if (packageResult.getResult() != null) {
            return packageResult.getResult();
        }
        if (!onlyInstalled) {
            GetChangelogVersionResult versionResult = new GetChangelogVersionResult(pkg);
            IOUtil.executeProcess(new String[]{"apt-get", "--no-act", "--verbose-versions", "install", pkg}, versionResult);
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
        IOUtil.executeProcess(new String[]{"dpkg", "--listfiles", library},
                new SharedJarOutputHandler(jars));
        cacheOfSharedJars.put(library, jars);
        return jars;
    }

    public void makeExecutable(String file) {
        IOUtil.executeProcess(new String[]{"chmod", "+x", file}, new NoOutputHandler());
    }

}
