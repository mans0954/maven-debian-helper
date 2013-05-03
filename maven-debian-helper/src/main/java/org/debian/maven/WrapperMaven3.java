package org.debian.maven;

/*
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

import java.io.IOException;
import java.lang.reflect.Method;

import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.classworlds.realm.NoSuchRealmException;

/**
 * This is a wrapper for the main function of Maven 3.
 */
public class WrapperMaven3 extends WrapperBase {

    public static int main(String[] args, ClassWorld classWorld) throws IOException {
        init(args);

        int result = -1;

        try {
            ClassRealm realm = classWorld.getRealm("debian");

            Class cwClass = realm.loadClass(ClassWorld.class.getName());
            Class mainClass = realm.loadClass("org.apache.maven.cli.MavenCli");
            Method mainMethod = mainClass.getMethod("main", new Class[]{ String[].class, cwClass });

            Object ret = mainMethod.invoke(mainClass, new Object[]{ newArgs, classWorld } );

            result = ((Integer)ret).intValue();
        } catch (NoSuchRealmException e) {
            System.err.println("ClassWorld realm 'debian' not found!");
        } catch (Exception e) {
            System.err.println("Unable to invoke Maven main method: " + e.toString());
        } finally {
            return result;
        }
    }
}
