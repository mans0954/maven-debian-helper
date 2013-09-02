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

/**
 * Asks the user a question with a multi line response.
 * The user finishes the response by entering two empty lines.
 * 
 * @author Emmanuel Bourg
 */
public class MultilineQuestion extends Question<String> {

    public MultilineQuestion(String question) {
        super(question);
    }

    @Override
    public String ask() {
        println(question);
        
        StringBuilder answer = new StringBuilder();
        int emptyLineCount = 0;
        while (emptyLineCount < 2) {
            String line = readLine();
            if (line.isEmpty()) {
                emptyLineCount++;
            } else {
                if (emptyLineCount > 0) {
                    emptyLineCount = 0;
                    answer.append("\n");
                }
                answer.append(line);
                answer.append("\n");
            }
        }
        return answer.toString().trim();
    }
}
