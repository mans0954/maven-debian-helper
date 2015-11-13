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

package org.debian.maven.packager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.debian.maven.packager.util.PackageScanner;
import org.debian.maven.repo.DependencyRuleSetFiles.RulesType;

/**
 * Analyze the Maven dependencies and extract the list of dependent packages,
 * reusable as subvars in the Debian control file and the list of POM files
 * to use and the rules if they did not exist already.
 * 
 * @author Ludovic Claude
 */
@Mojo(name = "dependencies", aggregator = true, requiresDependencyResolution = ResolutionScope.RUNTIME, defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class DependenciesMojo extends AbstractMojo {

    /**
     * The Maven Project Object
     */
    @Parameter(property = "project", readonly = true, required = true)
    protected MavenProject project;
    
    /**
     * A list of every project in this reactor; provided by Maven
     */
    @Parameter(property = "project.collectedProjects")
    protected List<MavenProject> collectedProjects;
    
    /**
     * Location of the file.
     */
    @Parameter(property = "debian.directory", defaultValue = "debian")
    protected File outputDirectory;
    
    /**
     * Name of the package (e.g. 'commons-lang')
     */
    @Parameter(property = "package", required = true)
    protected String packageName;
    
    /**
     * Type of the package (e.g. 'maven' or 'ant')
     */
    @Parameter(property = "packageType", defaultValue = "maven")
    protected String packageType;
    
    /**
     * Should we also resolve Javadoc dependencies
     */
    @Parameter(property = "resolveJavadoc", defaultValue = "false")
    protected boolean resolveJavadoc;
    
    /**
     * Location for the list of POMs file.
     */
    @Parameter(defaultValue = "debian/${package}.poms", required = true)
    protected File listOfPoms;
    
    /**
     * Location of the Maven repository
     */
    @Parameter(property = "maven.repo.local", defaultValue = "/usr/share/maven-repo")
    protected File mavenRepo;
    
    /**
     * Interactive execution will ask questions to the user
     */
    @Parameter(property = "interactive", defaultValue = "true")
    protected boolean interactive;
    
    /**
     * Offline prevents any download from Internet
     */
    @Parameter(property = "offline", defaultValue = "false")
    protected boolean offline;
    
    /**
     * Try to be verbose
     */
    @Parameter(property = "verbose", defaultValue = "false")
    protected boolean verbose;

    public void execute() throws MojoExecutionException {
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }

        DependenciesSolver solver = new DependenciesSolver(outputDirectory, new PackageScanner(offline), interactive);

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
