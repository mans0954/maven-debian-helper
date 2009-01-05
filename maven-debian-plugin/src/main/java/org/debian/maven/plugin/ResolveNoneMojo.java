package org.debian.maven.plugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * dummy goal that just reads the pom.xml files including parents
 *
 * @goal resolve-none
 * @aggregator true
 */
public class ResolveNoneMojo extends AbstractMojo
{
  public void execute() throws MojoExecutionException
  {
  }
}
