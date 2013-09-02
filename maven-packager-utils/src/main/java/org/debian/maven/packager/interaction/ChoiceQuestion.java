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

import java.util.Collection;

/**
 * A question with multiple choices.
 * 
 * @author Emmanuel Bourg
 */
public class ChoiceQuestion extends Question<Integer> {

    private int defaultChoice;
    private Collection<String> choices;

    public ChoiceQuestion(String question, int defaultChoice, Collection<String> choices) {
        super(question);
        this.defaultChoice = defaultChoice;
        this.choices = choices;
    }

    @Override
    public Integer ask() {
        Integer choice = null;
        
        // keep asking the question until a valid choice is entered
        while (choice == null) {
            println(question);
            printChoices(choices);
            print("> ");
            String response = readLine();
            if ("".equals(response.trim())) {
                choice = defaultChoice;
            } else {
                try {
                    int c = Integer.parseInt(response);
                    if (c >= 0 && c < choices.size()) {
                        choice = c;
                    }
                } catch (NumberFormatException e) {
                }
            }
        }
        
        return choice;
    }

    private void printChoices(Collection<String> choices) {
        int counter = 0;
        for (String choice : choices) {
            StringBuilder line = new StringBuilder();
            if (counter == defaultChoice) {
                line.append("[").append(counter).append("]");
            } else {
                line.append(" ").append(counter).append(" ");
            }
            line.append(" - ").append(choice);
            println(line.toString());
            ++counter;
        }
    }
}
