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

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class LicensesScanner {
    private final UserInteraction userInteraction = new UserInteraction();

    public Set<String> discoverLicenses(List<License> projectLicenses) {
        Set<String> licenses = new TreeSet<String>();
        for (License license : projectLicenses) {
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
                String question = "License " + licenseName + licenseUrl + " was not recognized, please enter a license name preferably in one of:"
                 + getAvailableLicenses();
                String s = userInteraction.ask(question);
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
        for (String license : licenseResult.getLicenses()) {
            boolean recognized = recognizeLicense(licenses, license, "");
            if (!recognized) {
                String question = "License " + license + " was not recognized, please enter a license name preferably in one of:"
                 + getAvailableLicenses();
                String s = userInteraction.ask(question);
                if (s.length() > 0) {
                    licenses.add(s);
                }
            }
        }

        if (licenses.isEmpty()) {
            String question = "License was not found, please enter a license name preferably in one of:"
             + getAvailableLicenses();
            String s = userInteraction.ask(question);
            if (s.length() > 0) {
                licenses.add(s);
            }
        }
        return licenses;
    }

    private String getAvailableLicenses() {
        return "Apache-2.0 Artistic BSD FreeBSD ISC CC-BY CC-BY-SA CC-BY-ND CC-BY-NC CC-BY-NC-SA\n"
         + "CC-BY-NC-ND CC0 CDDL CPL Eiffel Expat GPL-2 GPL-3 LGPL-2 LGPL-2.1 LGPL-3"
         + "GFDL-1.2 GFDL-1.3 GFDL-NIV LPPL MPL Perl PSF QPL W3C-Software ZLIB Zope";
    }

    boolean recognizeLicense(Set<String> licenses, String licenseName, String licenseUrl) {
        boolean recognized = false;
        licenseName = licenseName.toLowerCase();
        licenseUrl = licenseUrl.toLowerCase();
        if (licenseName.contains("mit ") || licenseUrl.contains("mit-license")) {
            licenses.add("MIT");
            recognized = true;
        } else if (licenseName.contains("bsd ") || licenseUrl.contains("bsd-license")) {
            licenses.add("BSD");
            recognized = true;
        } else if (licenseName.contains("artistic ") || licenseUrl.contains("artistic-license")) {
            licenses.add("Artistic");
            recognized = true;
        } else if (licenseName.contains("apache ") || licenseUrl.contains("apache")) {
            if (licenseName.contains("2.") || licenseUrl.contains("2.")) {
                licenses.add("Apache-2.0");
                recognized = true;
            } else if (licenseName.contains("1.0") || licenseUrl.contains("1.0")) {
                licenses.add("Apache-1.0");
                recognized = true;
            } else if (licenseName.contains("1.1") || licenseUrl.contains("1.1")) {
                licenses.add("Apache-1.1");
                recognized = true;
            }
        } else if (licenseName.contains("lgpl ") || licenseUrl.contains("lgpl")) {
            if (licenseName.contains("2.1") || licenseUrl.contains("2.1")) {
                licenses.add("LGPL-2.1");
                recognized = true;
            } else if (licenseName.contains("2") || licenseUrl.contains("2")) {
                licenses.add("LGPL-2");
                recognized = true;
            } else if (licenseName.contains("3") || licenseUrl.contains("3")) {
                licenses.add("LGPL-2");
                recognized = true;
            }
        } else if (licenseName.contains("gpl ") || licenseUrl.contains("gpl")) {
            if (licenseName.contains("2") || licenseUrl.contains("2")) {
                licenses.add("GPL-2");
                recognized = true;
            } else if (licenseName.contains("3") || licenseUrl.contains("3")) {
                licenses.add("GPL-3");
                recognized = true;
            }

        } else if (licenseUrl.contains("http://creativecommons.org/licenses/by-sa/3.0")) {
            licenses.add("CC-BY-SA-3.0");
            recognized = true;
        }
        return recognized;
    }

}
