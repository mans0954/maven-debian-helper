package org.debian.maven.packager.util;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.debian.maven.repo.Dependency;
import org.debian.maven.repo.Rule;

public class UserInteraction {
    private static final List<String> YESNO = new ArrayList<String>(2);
    static {
        YESNO.add("y");
        YESNO.add("n");
    }

    public String readLine() {
        LineNumberReader consoleReader = new LineNumberReader(new InputStreamReader(System.in));
        try {
            return consoleReader.readLine().trim();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    public String ask(String question) {
        println(question);
        print("> ");
        return readLine();
    }

    public boolean askYesNo(String question, boolean defaultOpt) {
        println(question);
        print(formatChoicesShort(defaultOpt ? 0 : 1, YESNO));
        print(" > ");
        String response = readLine();
        if ("".equals(response)) {
            return defaultOpt;
        } else {
            return response.startsWith("y");
        }
    }

    private String formatChoicesShort(int defaultOpt, Iterable<String> choices) {
        StringBuilder sb = new StringBuilder();
        int counter = 0;
        for (String choice : choices) {
            if (counter > 0) {
                sb.append("/");
            }
            if (counter == defaultOpt) {
                sb.append("[").append(choice).append("]");
            } else {
                sb.append(choice);
            }
            ++counter;
        }
        return sb.toString();
    }

    public int askChoices(String question, int defaultOpt, Iterable<String> choices) {
        println(question);
        print(formatChoicesLong(defaultOpt, choices));
        print("> ");
        String response = readLine();
        if ("".equals(response)) {
            return defaultOpt;
        }
        try {
            return Integer.parseInt(response);
        } catch (NumberFormatException e) {
            return defaultOpt;
        }
    }

    private String formatChoicesLong(int defaultOpt, Iterable<String> choices) {
        StringBuilder sb = new StringBuilder();
        int counter = 0;
        for (String choice : choices) {
            if (counter == defaultOpt) {
                sb.append("[").append(counter).append("]");
            } else {
                sb.append(" ").append(counter).append(" ");
            }
            sb.append(" - ").append(choice).append("\n");
            ++counter;
        }
        return sb.toString();
    }

    /**
     * Asks the user a question with a multi line response.
     *
     * The user finishes the response by entering two empty lines.
     */
    public String askMultiLine(String question) {
        println(question);
        StringBuilder sb = new StringBuilder();
        int emptyEnterCount = 0;
        while (emptyEnterCount < 2) {
            String s = readLine();
            if (s.isEmpty()) {
                emptyEnterCount++;
            } else {
                if (emptyEnterCount > 0) {
                    emptyEnterCount = 0;
                    sb.append("\n");
                }
                sb.append(s);
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    public void println(String text) {
        System.out.println(text);
    }

    public void print(String text) {
        System.out.print(text);
    }

    // extracted from DependencySolver
    // TODO can be simplified / cleaned up
    public Rule askForVersionRule(Dependency dependency, Map<String, Rule> versionToRules, List<Rule> defaultRules) {
        String question = "\n"
                + "Version of " + dependency.getGroupId() + ":"
                + dependency.getArtifactId() + " is " + dependency.getVersion()
                + "\nChoose how it will be transformed:";

        List<Rule> choices = new ArrayList<Rule>();

        if (versionToRules.containsKey(dependency.getVersion())) {
            choices.add(versionToRules.get(dependency.getVersion()));
        }

        Pattern p = Pattern.compile("(\\d+)(\\..*)");
        Matcher matcher = p.matcher(dependency.getVersion());
        if (matcher.matches()) {
            String mainVersion = matcher.group(1);
            Rule mainVersionRule = new Rule("s/" + mainVersion + "\\..*/" + mainVersion + ".x/",
                    "Replace all versions starting by " + mainVersion + ". with " + mainVersion + ".x");
            if (!choices.contains(mainVersionRule)) {
                choices.add(mainVersionRule);
            }
        }
        for (Rule rule : defaultRules) {
            if (!choices.contains(rule)) {
                choices.add(rule);
            }
        }

        List<String> choicesDescriptions = new ArrayList<String>();
        for (Rule choice : choices) {
            choicesDescriptions.add(choice.getDescription());
        }
        int choice = askChoices(question, 0, choicesDescriptions);
        Rule selectedRule = choices.get(choice);
        return selectedRule;
    }

}
