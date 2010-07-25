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
import java.util.Iterator;
import java.util.List;
import org.apache.maven.project.MavenProject;

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
    protected List collectedProjects;
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
     * Type of the package (e.g. 'maven' or 'ant')
     * @parameter expression="${nonInteractive}" default-value="false"
     */
    protected boolean nonInteractive;

    public void execute()
            throws MojoExecutionException {
        File f = outputDirectory;
        if (!f.exists()) {
            f.mkdirs();
        }

        DependenciesSolver solver = new DependenciesSolver();

        File basedir = project.getBasedir();
        List projects = new ArrayList();
        projects.add(project.getFile());
        if (collectedProjects != null) {
            for (Iterator i = collectedProjects.iterator(); i.hasNext();) {
                MavenProject subProject = (MavenProject) i.next();
                projects.add(subProject.getFile());
            }
        }

        solver.setProjects(projects);
        solver.setBaseDir(basedir);
        solver.setMavenRepo(mavenRepo);
        solver.setOutputDirectory(outputDirectory);
        solver.setPackageName(packageName);
        solver.setPackageType(packageType);
        solver.setNonInteractive(nonInteractive);
        solver.setListOfPoms(listOfPoms);

        solver.solveDependencies();

        solver.saveListOfPoms();
        solver.saveMavenRules();
        solver.saveSubstvars();
    }

}
