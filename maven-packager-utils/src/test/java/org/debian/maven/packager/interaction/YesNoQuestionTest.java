/*
 * Copyright 2013 Emmanuel Bourg
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

package org.debian.maven.packager.interaction;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

import junit.framework.TestCase;

public class YesNoQuestionTest extends TestCase {

    private String EOL = System.getProperty("line.separator");

    public void testQuestion() {
        StringWriter output = new StringWriter();

        YesNoQuestion question = new YesNoQuestion("Are you a Java programmer?", false);
        question.setInput(new BufferedReader(new StringReader("y\n")));
        question.setOutput(new PrintWriter(output, true));

        boolean answer = question.ask();

        assertEquals("Question", "Are you a Java programmer?" + EOL + "[y/N] > ", output.toString());
        assertEquals("Answer", true, answer);
    }

    public void testDefaultChoice() {
        StringWriter output = new StringWriter();

        YesNoQuestion question = new YesNoQuestion("Are you a Java programmer?", true);
        question.setInput(new BufferedReader(new StringReader("\n")));
        question.setOutput(new PrintWriter(output, true));

        boolean answer = question.ask();

        assertEquals("Question", "Are you a Java programmer?" + EOL + "[Y/n] > ", output.toString());
        assertEquals("Answer", true, answer);
    }

    public void testWrongAnswer() {
        StringWriter output = new StringWriter();

        YesNoQuestion question = new YesNoQuestion("Are you a Java programmer?", true);
        question.setInput(new BufferedReader(new StringReader("X\nno\n")));
        question.setOutput(new PrintWriter(output, true));

        boolean answer = question.ask();

        assertEquals("Question", "Are you a Java programmer?" + EOL + "[Y/n] > " +
                                 "Are you a Java programmer?" + EOL + "[Y/n] > ", output.toString());
        assertEquals("Answer", false, answer);
    }

    public void testUpperCaseAnswer() {
        StringWriter output = new StringWriter();

        YesNoQuestion question = new YesNoQuestion("Are you a Java programmer?", false);
        question.setInput(new BufferedReader(new StringReader("YES\n")));
        question.setOutput(new PrintWriter(output, true));

        boolean answer = question.ask();

        assertEquals("Question", "Are you a Java programmer?" + EOL + "[y/N] > ", output.toString());
        assertEquals("Answer", true, answer);
    }
}
