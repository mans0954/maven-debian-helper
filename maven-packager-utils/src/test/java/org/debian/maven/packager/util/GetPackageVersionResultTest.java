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

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class GetPackageVersionResultTest {

    private GetPackageVersionResult result = new GetPackageVersionResult();

    @Test
    public void testFilterDpkgOutput() throws Exception {
        //dpkg --status libjavacc-maven-plugin-java
        List<String> dpkgOut = new ArrayList<String>();
        dpkgOut.add("Package: libjavacc-maven-plugin-java");
        dpkgOut.add("Status: install ok installed");
        dpkgOut.add("Priority: optional");
        dpkgOut.add("Section: java");
        dpkgOut.add("Installed-Size: 144");
        dpkgOut.add("Maintainer: Ubuntu Developers <ubuntu-devel-discuss@lists.ubuntu.com>");
        dpkgOut.add("Architecture: all");
        dpkgOut.add("Source: javacc-maven-plugin");
        dpkgOut.add("Version: 2.6-1");
        dpkgOut.add("Depends: javacc, jtb, libdoxia-java, libdoxia-sitetools-java, libmaven-reporting-impl-java, libmaven2-core-java, libplexus-utils-java");
        dpkgOut.add("Description: maven plugin which uses JavaCC to process JavaCC grammar files");

        for (String line : dpkgOut) {
            result.newLine(line);
        }
        assertEquals("2.6", result.getResult());
    }

    @Test
    public void testFilterAptFileOutput() throws Exception {
        // apt-get --no-act --verbose-versions install libmaven-war-plugin-java
        List<String> dpkgOut = new ArrayList<String>();
        dpkgOut.add("NOTE: Ceci n'est qu'une simulation !");
        dpkgOut.add("      apt-get a besoin des privilèges du superutilisateur");
        dpkgOut.add("      pour pouvoir vraiment fonctionner.");
        dpkgOut.add("      Veuillez aussi noter que le verrouillage est désactivé,");
        dpkgOut.add("      et la situation n'est donc pas forcément représentative");
        dpkgOut.add("      de la réalité !");
        dpkgOut.add("Lecture des listes de paquets... Fait");
        dpkgOut.add("Construction de l'arbre des dépendances");
        dpkgOut.add("Lecture des informations d'état... Fait");
        dpkgOut.add("Les NOUVEAUX paquets suivants seront installés :");
        dpkgOut.add("   libmaven-war-plugin-java (2.1~beta1-1build1)");
        dpkgOut.add("0 mis à jour, 1 nouvellement installés, 0 à enlever et 7 non mis à jour.");
        dpkgOut.add("Inst libmaven-war-plugin-java (2.1~beta1-1build1 Ubuntu:11.10/oneiric [all])");
        dpkgOut.add("Conf libmaven-war-plugin-java (2.1~beta1-1build1 Ubuntu:11.10/oneiric [all])");

        for (String line : dpkgOut) {
            result.newLine(line);
        }
        assertEquals("2.1-beta1", result.getResult());
    }

}
