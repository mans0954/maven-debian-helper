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
 * Asks the user a question with a single line response.
 * 
 * @author Emmanuel Bourg
 */
public class SimpleQuestion extends Question<String> {

    private String defaultValue;

    public SimpleQuestion(String question) {
        super(question);
    }

    public SimpleQuestion(String question, String defaultValue) {
        super(question);
        this.defaultValue = defaultValue;
    }

    @Override
    public String ask() {
        println(question);
        if (defaultValue != null) {
            print("[");
            print(defaultValue);
            print("] ");
        }
        print("> ");
        return readLine();
    }
}
