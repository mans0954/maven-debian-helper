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
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

/**
 * A generic question.
 * 
 * @author Emmanuel Bourg
 */
public abstract class Question<T> {

    protected String question;
    private BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    private PrintWriter out = new PrintWriter(System.out, true);

    protected Question(String question) {
        this.question = question;
    }

    void setInput(BufferedReader in) {
        this.in = in;
    }

    void setOutput(PrintWriter out) {
        this.out = out;
    }

    protected String readLine() {
        try {
            String line = in.readLine();
            return line != null ? line.trim() : "";
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    protected void println(String text) {
        out.println(text);
    }

    protected void print(String text) {
        out.print(text);
        out.flush();
    }

    /**
     * Asks the question and returns the response.
     */
    public abstract T ask();
}
