/*
 * Copyright 2009 Torsten Werner.
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

package org.debian.maven;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;

import org.apache.maven.cli.MavenCli;
import org.codehaus.plexus.util.IOUtil;

/**
 * This is a wrapper for Maven's main function that allows extra arguments
 * to be specified by a properties file. Properties are read from the file
 * specified by the system property {@value #EXTRA_PROPERTIES_PROPERTY}.
 */
public class Wrapper {
    private static final String EXTRA_PROPERTIES_PROPERTY = "properties.file.manual";
    
    /**
     * Reads the properties in the file specified by the given system property.
     * 
     * @param property system property that specifies the location of the properties file
     * @return {@link Properties} provided by the file
     */
    public static Properties readProperties(String property) throws IOException {
        String filename = System.getProperty(property);
        Properties result = new Properties();
        if (filename == null) {
            return result;
        }
        FileInputStream stream = null;
        try {
            stream = new FileInputStream(filename);
            result.load(stream);
            return result;
        } finally {
            IOUtil.close(stream);
        }
    }

    /**
     * Creates a new arguments array first using the given properties then the original arguments.
     * 
     * @param properties additional properties to add
     * @param args original commandline arguments
     * @return new arguments to use
     */
    public static String[] updateCommandLine(Properties properties, String[] args) throws IOException {
        int argsSize = args.length;
        int extraSize = properties.size();

        String[] newArgs = new String[argsSize + extraSize];

        int i = 0;
        for (Enumeration<?> e = properties.propertyNames(); e.hasMoreElements(); ) {
            String key = (String) e.nextElement();
            String value = properties.getProperty(key);
            newArgs[i] = "-D" + key + "=" + value;
            i++;
        }

        System.arraycopy(args, 0, newArgs, extraSize, argsSize);
        return newArgs;
    }

    /**
     * Wraps maven's main function
     */
    public static int main(String[] args) throws IOException {
        Properties extraArguments = readProperties(EXTRA_PROPERTIES_PROPERTY);
        String[] newArgs = updateCommandLine(extraArguments, args);
        
        MavenCli.main(newArgs);
        return 0;
    }
}
