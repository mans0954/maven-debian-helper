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
import java.util.ArrayList;
import java.util.List;
import org.apache.maven.project.MavenProject;
import org.debian.maven.packager.util.PackageScanner;
import org.debian.maven.repo.DependencyRuleSetFiles.RulesType;

/**
 * Analyze the Maven dependencies and extract the list of dependent packages,
 * reusable as subvars in the Debian control file and the list of POM files
 * to use and the rules if they did not exist already.
 *
 * @goal dependencies
 * @aggregator
 * @requiresDependencyResolution
 * @phase process-sources
 * 
 * @author Ludovic Claude
 */
public class DependenciesMojo
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
    protected List<MavenProject> collectedProjects;
    /**
     * Location of the file.
     * @parameter expression="${debian.directory}"
     *   default-value="debian"
     */
    protected File outputDirectory;
    /**
     * Name of the package (e.g. 'commons-lang')
     * @parameter expression="${package}"
     * @required
     */
    protected String packageName;
    /**
     * Type of the package (e.g. 'maven' or 'ant')
     * @parameter expression="${packageType}" default-value="maven"
     */
    protected String packageType;
    /**
     * Should we also resolve Javadoc dependencies
     * @parameter expression="${resolveJavadoc}" default-value="false"
     */
    protected boolean resolveJavadoc;
    /**
     * Location for the list of POMs file.
     * @required
     * @parameter expression="debian/${package}.poms"
     */
    protected File listOfPoms;
    /**
     * Location of the Maven repository
     *
     * @parameter expression="${maven.repo.local}" default-value="/usr/share/maven-repo"
     */
    protected File mavenRepo;
    /**
     * Interactive execution will ask questions to the user
     * @parameter expression="${interactive}" default-value="true"
     */
    protected boolean interactive;
    /**
     * Offline prevents any download from Internet
     * @parameter expression="${offline}" default-value="false"
     */
    protected boolean offline;
    /**
     * Try to be verbose
     * @parameter expression="${verbose}" default-value="false"
     */
    protected boolean verbose;

    public void execute()
            throws MojoExecutionException {
        File f = outputDirectory;
        if (!f.exists()) {
            f.mkdirs();
        }

        DependenciesSolver solver = new DependenciesSolver(outputDirectory, new PackageScanner(offline));

        File basedir = project.getBasedir();
        // TODO: use the list of project defined here for some initialisation step, I've forgotten what to do...
        List<File> projects = new ArrayList<File>();
        projects.add(project.getFile());
        if (collectedProjects != null) {
            for (MavenProject subProject : collectedProjects) {
                projects.add(subProject.getFile());
            }
        }

        solver.setBaseDir(basedir);
        solver.mavenRepo = mavenRepo;
        solver.packageName = packageName;
        solver.packageType = packageType;
        solver.generateJavadoc = resolveJavadoc;
        solver.interactive = interactive;
        solver.setListOfPoms(listOfPoms);
        solver.verbose = verbose;

        if (solver.pomTransformer.getListOfPOMs().getFirstPOM() == null && collectedProjects != null) {
            for (MavenProject subProject : collectedProjects) {
                solver.pomTransformer.getListOfPOMs().addPOM(subProject.getFile());
            }
        }

        solver.solveDependencies();

        solver.pomTransformer.getListOfPOMs().save();
        solver.pomTransformer.getRulesFiles().save(outputDirectory, RulesType.RULES);
        solver.saveSubstvars();
    }

}
