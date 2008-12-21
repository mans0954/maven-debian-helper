package org.debian.maven;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;
import org.apache.maven.cli.MavenCli;
import org.codehaus.classworlds.ClassWorld;

/* This is a wrapper for Maven's main function that reads 2 property
 * files: debian/auto.properties and debian/manual.properties and adds
 * their content to maven's commandline.
 */

public class Wrapper
{
  /* holds system properties
   */

  private static Properties systemProperties = System.getProperties();

  /* holds extra properties that are read from property files
   */

  private static Properties extraProperties = new Properties();

  /* the extended command line for maven's main function
   */

  private static String[] newArgs;

  /* Opens the filename specified by property 'key' and loads its
   * properties into extraProperties
   */

  public static void updateProperties(String key) throws IOException
  {
    String filename = systemProperties.getProperty(key);
    if (filename != null)
    {
      extraProperties.load(new FileInputStream(filename));
    }
  }

  /* Fill new commandline array 'newArgs' with properties from
   * extraProperties and the current commandline array 'args.
   */

  public static void updateCommandLine(String[] args) throws IOException
  {
    int argsSize  = args.length;
    int extraSize = extraProperties.size();
    
    newArgs = new String[argsSize + extraSize];

    int i = 0;
    for(Enumeration e = extraProperties.propertyNames(); e.hasMoreElements(); )
    {
      String key   = (String) e.nextElement();
      String value = extraProperties.getProperty(key);
      newArgs[i] = "-D" + key + "=" + value;
      i++;
    }
    
    System.arraycopy(args, 0, newArgs, extraSize, argsSize);
  }

  /* wraps maven's main function
   */

  public static int main(String[] args, ClassWorld classWorld) throws IOException
  {
    updateProperties("properties.file.auto");
    updateProperties("properties.file.manual");

    updateCommandLine(args);

    return MavenCli.main(newArgs, classWorld);
  }
}
