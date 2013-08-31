package org.debian.maven.packager.util;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.List;

public class UserInteraction {
    private static final List<String> YESNO = new ArrayList<String>(2);
    static {
        YESNO.add("y");
        YESNO.add("n");
    }

    private String readLine() {
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

    private void println(String text) {
        System.out.println(text);
    }

    private void print(String text) {
        System.out.print(text);
    }
}
