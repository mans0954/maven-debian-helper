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


import org.apache.maven.model.License;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.debian.maven.packager.util.IOUtil.readLine;

public class LicensesScanner {

    public Set<String> discoverLicenses(List<License> projectLicenses) {
        Set<String> licenses = new TreeSet<String>();
        for (Iterator<License> i = projectLicenses.iterator(); i.hasNext(); ) {
            License license =  i.next();
            String licenseName = "";
            if (license.getName() != null) {
                licenseName = license.getName() + " ";
            }
            String licenseUrl = "";
            if (license.getUrl() != null) {
                licenseUrl = license.getUrl();
            }
            boolean recognized = recognizeLicense(licenses, licenseName, licenseUrl);
            if (!recognized) {
                System.out.println("License " + licenseName + licenseUrl + " was not recognized, please enter a license name preferably in one of:");
                printAvailableLicenses();
                System.out.print("> ");
                String s = readLine();
                if (s.length() > 0) {
                    licenses.add(s);
                }
            }
        }

        System.out.println();
        System.out.println("Checking licenses in the upstream sources...");
        LicenseCheckResult licenseResult = new LicenseCheckResult();
        IOUtil.executeProcess(new String[]{"/bin/sh", "-c", "licensecheck `find . -type f`"},
                licenseResult);
        for (Iterator<String> i = licenseResult.getLicenses().iterator(); i.hasNext(); ) {
            String license = i.next();
            boolean recognized = recognizeLicense(licenses, license, "");
            if (!recognized) {
                System.out.println("License " + license + " was not recognized, please enter a license name preferably in one of:");
                printAvailableLicenses();
                System.out.print("> ");
                String s = readLine();
                if (s.length() > 0) {
                    licenses.add(s);
                }
            }
        }

        if (licenses.isEmpty()) {
            System.out.println("License was not found, please enter a license name preferably in one of:");
            printAvailableLicenses();
            System.out.print("> ");
            String s = readLine();
            if (s.length() > 0) {
                licenses.add(s);
            }
        }
        return licenses;
    }

    private void printAvailableLicenses() {
        System.out.println("Apache-2.0 Artistic BSD FreeBSD ISC CC-BY CC-BY-SA CC-BY-ND CC-BY-NC CC-BY-NC-SA");
        System.out.println("CC-BY-NC-ND CC0 CDDL CPL Eiffel Expat GPL-2 GPL-3 LGPL-2 LGPL-2.1 LGPL-3");
        System.out.println("GFDL-1.2 GFDL-1.3 GFDL-NIV LPPL MPL Perl PSF QPL W3C-Software ZLIB Zope");
    }

    boolean recognizeLicense(Set<String> licenses, String licenseName, String licenseUrl) {
        boolean recognized = false;
        licenseName = licenseName.toLowerCase();
        licenseUrl = licenseUrl.toLowerCase();
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

        } else if (licenseUrl.indexOf("http://creativecommons.org/licenses/by-sa/3.0") >= 0) {
            licenses.add("CC-BY-SA-3.0");
            recognized = true;
        }
        return recognized;
    }

}
