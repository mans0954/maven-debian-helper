package org.debian.maven;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import org.apache.maven.cli.MavenCli;
import org.codehaus.classworlds.ClassWorld;

public class Wrapper
{
  /* Opens the filename specified by property 'key' and adds its content to
   * the properties.
   */
  public static void loadProperties(Properties properties, String key) throws IOException
  {
    String filename = properties.getProperty(key);
    if (filename != null)
    {
      properties.load(new FileInputStream(filename));
    }
  }

  /* Add more properties to the system properties. The files specified
   * by 'properties.file.auto' and 'properties.file.manual' are read.
   */
  public static void updateSystemProperties() throws IOException
  {
    Properties systemproperties = System.getProperties();
    loadProperties(systemproperties, "properties.file.auto");
    loadProperties(systemproperties, "properties.file.manual");
    System.setProperties(systemproperties);
  }

  public static void main(String[] args, ClassWorld classWorld) throws IOException
  {
    updateSystemProperties();
    MavenCli.main(args, classWorld);
  }
}
