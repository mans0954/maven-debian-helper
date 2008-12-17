package org.debian.maven;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;
import org.apache.maven.cli.MavenCli;
import org.codehaus.classworlds.ClassWorld;

/* This is a wrapper for Maven's main function that implements reading 2
 * property files: debian/auto.properties and debian/manual.properties.
 */

public class Wrapper
{
  /* Opens the filename specified by property 'key' and returns its content as
   * a String array of items -Dkey=value.
   */
  public static String[] getProperties(String key) throws IOException
  {
    Properties systemproperties = System.getProperties();
    String filename = systemproperties.getProperty(key);
    if (filename != null)
    {
      Properties extraProperties = new Properties();
      extraProperties.load(new FileInputStream(filename));
      String[] extraArgs = new String[extraProperties.size()];
      int i = 0;
      for(Enumeration e = extraProperties.propertyNames(); e.hasMoreElements(); )
      {
	String k = (String) e.nextElement();
	String v = (String) extraProperties.get(k);
	extraArgs[i] = "-D" + k + "=" + v;
	i++;
      }
      return extraArgs;
    }
    return new String[0];
  }

  /* Add more properties to the commandline. The files specified
   * by '-Dproperties.file.auto=' and '-Dproperties.file.manual=' are read.
   */
  public static String[] updateCommandLine(String[] args) throws IOException
  {
    String[] autoArgs = getProperties("properties.file.auto");
    String[] manualArgs = getProperties("properties.file.manual");

    int argsSize = args.length;
    int autoSize = autoArgs.length;
    int manualSize = manualArgs.length;
    
    String[] newArgs = new String[argsSize + autoSize + manualSize];
    
    System.arraycopy(autoArgs, 0, newArgs, 0, autoSize);
    System.arraycopy(manualArgs, 0, newArgs, autoSize, manualSize);
    System.arraycopy(args, 0, newArgs, autoSize + manualSize, argsSize);
    
    return newArgs;
  }

  public static int main(String[] args, ClassWorld classWorld) throws IOException
  {
    return MavenCli.main(updateCommandLine(args), classWorld);
  }
}
