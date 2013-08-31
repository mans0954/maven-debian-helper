package org.debian.maven.packager.util;

import java.util.Set;
import java.util.TreeSet;

import org.debian.maven.repo.Dependency;

public class IgnoreDependencyQuestions {

    private Set<Dependency> notIgnoredDependencies = new TreeSet<Dependency>();
    private final UserInteraction userInteraction;
    private final boolean interactive;

    // Plugins not useful for the build or whose use is against the
    // Debian policy
    private static final String[][] PLUGINS_TO_IGNORE = {
        {"org.apache.maven.plugins", "maven-archetype-plugin"},
        {"org.apache.maven.plugins", "changelog-maven-plugin"},
        {"org.apache.maven.plugins", "maven-deploy-plugin"},
        {"org.apache.maven.plugins", "maven-release-plugin"},
        {"org.apache.maven.plugins", "maven-repository-plugin"},
        {"org.apache.maven.plugins", "maven-scm-plugin"},
        {"org.apache.maven.plugins", "maven-scm-publish-plugin"},
        {"org.apache.maven.plugins", "maven-stage-plugin"},
        {"org.apache.maven.plugins", "maven-eclipse-plugin"},
        {"org.apache.maven.plugins", "maven-idea-plugin"},
        {"org.apache.maven.plugins", "maven-source-plugin"},
        {"org.codehaus.mojo", "changelog-maven-plugin"},
        {"org.codehaus.mojo", "netbeans-freeform-maven-plugin"},
        {"org.codehaus.mojo", "nbm-maven-plugin"},
        {"org.codehaus.mojo", "ideauidesigner-maven-plugin"},
        {"org.codehaus.mojo", "scmchangelog-maven-plugin"},
        {"com.github.github", "site-maven-plugin"},
    };
    private static final String[][] PLUGINS_THAT_CAN_BE_IGNORED = {
        {"org.apache.maven.plugins", "maven-ant-plugin"},
        {"org.apache.maven.plugins", "maven-assembly-plugin"},
        {"org.apache.maven.plugins", "maven-enforcer-plugin"},
        {"org.apache.maven.plugins", "maven-gpg-plugin"},
        {"org.apache.rat", "apache-rat-plugin"},
        {"org.codehaus.mojo", "rat-maven-plugin"},
        {"org.codehaus.mojo", "buildnumber-maven-plugin"},
        {"org.apache.maven.plugins", "maven-verifier-plugin"},
        {"org.codehaus.mojo", "findbugs-maven-plugin"},
        {"org.codehaus.mojo", "fitnesse-maven-plugin"},
        {"org.codehaus.mojo", "ianal-maven-plugin"},
        {"org.codehaus.mojo", "selenium-maven-plugin"},
        {"org.codehaus.mojo", "dbunit-maven-plugin"},
        {"org.codehaus.mojo", "failsafe-maven-plugin"},
        {"org.codehaus.mojo", "shitty-maven-plugin"},
        {"com.mycila.maven-license-plugin", "maven-license-plugin"},
    };
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
        {"org.codehaus.mojo", "l10n-maven-plugin"},
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
        {"org.kathrynhuxtable.maven.wagon", "wagon-gitsite"},
        {"com.github.stephenc.wagon", "wagon-gitsite"},
        {"com.google.code.maven-svn-wagon", "maven-svn-wagon"},
    };


    public IgnoreDependencyQuestions(UserInteraction userInteraction, boolean interactive) {
        this.interactive = interactive;
        this.userInteraction = userInteraction;
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

    public boolean askIgnoreDependency(String sourcePomLoc, Dependency dependency, String message) {
        return askIgnoreDependency(sourcePomLoc, dependency, message, true);
    }

    private boolean askIgnoreDependency(String sourcePomLoc, Dependency dependency, String message, boolean defaultToIgnore) {
        if (!interactive || notIgnoredDependencies.contains(dependency)) {
            return false;
        }
        String q = "\n" + "In " + sourcePomLoc + ":" + message + "  " + dependency;
        boolean ignore = userInteraction.askYesNo(q, defaultToIgnore);
        if (!ignore) {
            notIgnoredDependencies.add(dependency);
        }
        return ignore;
    }

    public boolean askIgnoreUnnecessaryDependency(Dependency dependency, String sourcePomLoc,
                                                  boolean runTests, boolean generateJavadoc) {

        if (containsPlugin(PLUGINS_TO_IGNORE, dependency)
         && askIgnoreDependency(sourcePomLoc, dependency,
         "This plugin is not useful for the build or its use is against Debian policies. Ignore this plugin?"))
            return true;

        if (containsPlugin(EXTENSIONS_TO_IGNORE, dependency)
         && askIgnoreDependency(sourcePomLoc, dependency,
         "This extension is not useful for the build or its use is against Debian policies. Ignore this extension?"))
            return true;

        if (containsPlugin(PLUGINS_THAT_CAN_BE_IGNORED, dependency)
         && askIgnoreDependency(sourcePomLoc, dependency, "This plugin may be ignored in some cases. Ignore this plugin?"))
            return true;
   
        if (!runTests) {
            if ("test".equals(dependency.getScope())
             && askIgnoreDependency(sourcePomLoc, dependency, "Tests are turned off. Ignore this test dependency?"))
                return true;
            if (containsPlugin(TEST_PLUGINS, dependency)
             && askIgnoreDependency(sourcePomLoc, dependency, "Tests are turned off. Ignore this test plugin?"))
                return true;
        }
        // is Documentation Or Report Plugin?
        if (!generateJavadoc && containsPlugin(DOC_PLUGINS, dependency)
             && askIgnoreDependency(sourcePomLoc, dependency,
                 "Documentation is turned off. Ignore this documentation plugin?"))
            return true;

        return false;
    }

    public boolean askIgnoreDocOrReportPlugin(String sourcePomLoc, Dependency dependency) {
        return containsPlugin(DOC_PLUGINS, dependency)
            && askIgnoreDependency(sourcePomLoc, dependency,
            "This documentation or report plugin cannot be found in the Maven repository for Debian. Ignore this plugin?");
    }

    public String askIgnoreNeededDependency(String sourcePomLoc, Dependency dependency) {
        String type = dependency.isPlugin() ? "plugin" : "dependency";
        String question = "This " + type + " cannot be found in the Debian Maven repository. Ignore this " + type + "?";

        if (!askIgnoreDependency(sourcePomLoc, dependency, question, false)) {
            return sourcePomLoc + ": " + type + " is not packaged in the Maven repository for Debian: " + dependency.getGroupId() + ":"
                    + dependency.getArtifactId() + ":" + dependency.getVersion();
        }
        return "";
    }
}
 