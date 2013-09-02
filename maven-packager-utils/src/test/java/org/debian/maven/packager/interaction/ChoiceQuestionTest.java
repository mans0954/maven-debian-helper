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
import java.util.Arrays;

import junit.framework.TestCase;

public class ChoiceQuestionTest extends TestCase {
    
    private String EOL = System.getProperty("line.separator");

    public void testQuestion() {
        StringWriter output = new StringWriter();

        ChoiceQuestion question = new ChoiceQuestion("What's the color of your poney?",2, Arrays.asList("Red", "Green", "Blue"));
        question.setInput(new BufferedReader(new StringReader("1\n")));
        question.setOutput(new PrintWriter(output, true));

        int answer = question.ask();

        assertEquals("Question", "What's the color of your poney?" + EOL +
                " 0  - Red" + EOL +
                " 1  - Green" + EOL +
                "[2] - Blue" + EOL +
                "> ", output.toString());
        assertEquals("Answer", 1, answer);
    }

    public void testDefaultChoice() {
        StringWriter output = new StringWriter();

        ChoiceQuestion question = new ChoiceQuestion("What's the color of your poney?",2, Arrays.asList("Red", "Green", "Blue"));
        question.setInput(new BufferedReader(new StringReader("\n")));
        question.setOutput(new PrintWriter(output, true));

        int answer = question.ask();

        assertEquals("Question", "What's the color of your poney?" + EOL +
                " 0  - Red" + EOL +
                " 1  - Green" + EOL +
                "[2] - Blue" + EOL +
                "> ", output.toString());
        assertEquals("Answer", 2, answer);
    }
    
    public void testOutOfRangeChoice() {
        StringWriter output = new StringWriter();

        ChoiceQuestion question = new ChoiceQuestion("What's the color of your poney?",1, Arrays.asList("Red", "Green", "Blue"));
        question.setInput(new BufferedReader(new StringReader("3\n0\n")));
        question.setOutput(new PrintWriter(output, true));

        int answer = question.ask();

        assertEquals("Question", "What's the color of your poney?" + EOL +
                " 0  - Red" + EOL +
                "[1] - Green" + EOL +
                " 2  - Blue" + EOL +
                "> " +
                "What's the color of your poney?" + EOL +
                " 0  - Red" + EOL +
                "[1] - Green" + EOL +
                " 2  - Blue" + EOL +
                "> ", output.toString());
        
        assertEquals("Answer", 0, answer);
    }

    public void testNegativeChoice() {
        StringWriter output = new StringWriter();

        ChoiceQuestion question = new ChoiceQuestion("What's the color of your poney?",1, Arrays.asList("Red", "Green", "Blue"));
        question.setInput(new BufferedReader(new StringReader("-1\n0\n")));
        question.setOutput(new PrintWriter(output, true));

        int answer = question.ask();

        assertEquals("Question", "What's the color of your poney?" + EOL +
                " 0  - Red" + EOL +
                "[1] - Green" + EOL +
                " 2  - Blue" + EOL +
                "> " +
                "What's the color of your poney?" + EOL +
                " 0  - Red" + EOL +
                "[1] - Green" + EOL +
                " 2  - Blue" + EOL +
                "> ", output.toString());
        
        assertEquals("Answer", 0, answer);
    }

    public void testNonNumericChoice() {
        StringWriter output = new StringWriter();

        ChoiceQuestion question = new ChoiceQuestion("What's the color of your poney?",1, Arrays.asList("Red", "Green", "Blue"));
        question.setInput(new BufferedReader(new StringReader("X\n2\n")));
        question.setOutput(new PrintWriter(output, true));

        int answer = question.ask();

        assertEquals("Question", "What's the color of your poney?" + EOL +
                " 0  - Red" + EOL +
                "[1] - Green" + EOL +
                " 2  - Blue" + EOL +
                "> " +
                "What's the color of your poney?" + EOL +
                " 0  - Red" + EOL +
                "[1] - Green" + EOL +
                " 2  - Blue" + EOL +
                "> ", output.toString());
        
        assertEquals("Answer", 2, answer);
    }
}
