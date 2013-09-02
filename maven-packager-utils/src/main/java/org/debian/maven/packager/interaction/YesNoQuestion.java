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
 * A yes/no question. The question is asked again if the answer is not 'y', 'n',
 * 'yes' or 'no'. The answer is case insensitive. A blank response returns
 * the default choice.
 * 
 * @author Emmanuel Bourg
 */
public class YesNoQuestion extends Question<Boolean> {

    private boolean defaultChoice;

    public YesNoQuestion(String question, boolean defaultChoice) {
        super(question);
        this.defaultChoice = defaultChoice;
    }

    @Override
    public Boolean ask() {
        Boolean choice = null;
        
        // keep asking the question until a valid choice is entered
        while (choice == null) {
            println(question);
            print("[");
            print(defaultChoice ? "Y" : "y");
            print("/");
            print(defaultChoice ? "n" : "N");
            print("]");
            print(" > ");
            String response = readLine();
            if ("".equals(response.trim())) {
                choice = defaultChoice;
            } else if (response.equalsIgnoreCase("y") || response.equalsIgnoreCase("yes")) {
                choice = true;
            } else if (response.equalsIgnoreCase("n") || response.equalsIgnoreCase("no")) {
                choice = false;
            }
        }
        
        return choice;
    }
}
