package org.debian.maven;

/*
 * Copyright 2009 Torsten Werner.
 * Copyright 2013 Debian Java Developers.
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

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;

/**
 * This is a base class for Maven's main function wrappers. A subclass for
 * specific Maven version should read 2 property files: debian/auto.properties
 * and debian/manual.properties and add their content to maven's commandline.
 */
public class WrapperBase {

    /** Holds extra properties that are read from property files */
    private static Properties extraProperties = new Properties();

    /** The extended command line for maven's main function */
    protected static String[] newArgs;

    /** 
     * Opens the filename specified by property 'key' and loads its
     * properties into extraProperties
     */
    public static void updateProperties(String key) throws IOException {
        String filename = System.getProperty(key);
        if (filename == null) {
            return;
        }
        FileInputStream stream = null;
        try {
            stream = new FileInputStream(filename);
            extraProperties.load(stream);
        } finally {
            if (stream != null) {
                stream.close();
            }
        }
    }

    /**
     * Fill new commandline array 'newArgs' with properties from
     * extraProperties and the current commandline array 'args.
     */
    public static void updateCommandLine(String[] args) throws IOException {
        int argsSize = args.length;
        int extraSize = extraProperties.size();

        newArgs = new String[argsSize + extraSize];

        int i = 0;
        for (Enumeration e = extraProperties.propertyNames(); e.hasMoreElements(); ) {
            String key = (String) e.nextElement();
            String value = extraProperties.getProperty(key);
            newArgs[i] = "-D" + key + "=" + value;
            i++;
        }

        System.arraycopy(args, 0, newArgs, extraSize, argsSize);
    }

    public static void init(String[] args) throws IOException {
        updateProperties("properties.file.manual");

        updateCommandLine(args);
    }
}
