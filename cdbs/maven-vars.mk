# This file is based on ant-vars.mk

# Copyright © 2003 Stefan Gybas <sgybas@debian.org>
# Copyright © 2008 Torsten Werner <twerner@debian.org>
# Description: Defines useful variables for packages which use Maven
#
# This program is free software; you can redistribute it and/or
# modify it under the terms of the GNU General Public License as
# published by the Free Software Foundation; either version 2, or (at
# your option) any later version.
#
# This program is distributed in the hope that it will be useful, but
# WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program; if not, write to the Free Software
# Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
# 02111-1307 USA.

_cdbs_scripts_path ?= /usr/lib/cdbs
_cdbs_rules_path ?= /usr/share/cdbs/1/rules
_cdbs_class_path ?= /usr/share/cdbs/1/class

ifndef _cdbs_class_maven_vars
_cdbs_class_maven_vars = 1

# Maven home directory.  Doesn't need to be changed except when using
# nonstandard Maven installations.
MAVEN_HOME = /usr/share/maven2

# The home directory of the Java Runtime Environment (JRE) or Java Development
# Kit (JDK). You can either directly set JAVA_HOME in debian/rules or set
# JAVA_HOME_DIRS to multiple possible home directories. The first existing
# directory from this list is used for JAVA_HOME. You can also override
# JAVACMD in case you don't want to use the default JAVA_HOME/bin/java.
JAVA_HOME = $(shell for jh in $(JAVA_HOME_DIRS); do if [ -x "$$jh/bin/java" ]; then \
	    echo $${jh}; exit 0; fi; done)
JAVACMD   = $(JAVA_HOME)/bin/java

# You can list all Java ARchives (JARs) to be added to the class path in
# DEB_JARS, either with their full path or just the basename if the JAR is
# in /usr/share/java. You may also ommit the ".jar" extension. Non-existing
# files will silently be ignored. tools.jar is automatically added to the
# end of the class path if it exists in the JDK's lib directory.
# You can override the complete class path using DEB_CLASSPATH.
DEB_JARS_BASE = /usr/share/java
DEB_CLASSPATH = $(MAVEN_HOME)/boot/classworlds.jar:$(shell for jar in $(DEB_JARS); do \
		if [ -f "$$jar" ]; then echo -n "$${jar}:"; fi; \
		if [ -f "$$jar".jar ]; then echo -n "$${jar}.jar:"; fi; \
		if [ -f $(DEB_JARS_BASE)/"$$jar" ]; then echo -n "$(DEB_JARS_BASE)/$${jar}:"; fi; \
		if [ -f $(DEB_JARS_BASE)/"$$jar".jar ]; then echo -n "$(DEB_JARS_BASE)/$${jar}.jar:"; fi; \
		done; \
		if [ -f "$(JAVA_HOME)/lib/tools.jar" ]; then echo -n "$(JAVA_HOME)/lib/tools.jar"; fi)

# Extra arguments for the Maven command line.
# TODO: DOES NOT WORK YET BECAUSE IT NEEDS PATCHING MAVEN!!!
DEB_MAVEN_ARGS = 

# Property file for Maven, defaults to debian/maven.properties if it exists.
# You may define additional properties. Please note that command-line
# arguments in MAVEN_ARGS (see below) override the settings in pom.xml and
# the property file.
DEB_MAVEN_PROPERTYFILE = $(shell test -f $(CURDIR)/debian/maven.properties && echo $(CURDIR)/debian/maven.properties)

# You can specify additional JVM arguments in MAVEN_OPTS and Maven
# command-line arguments in MAVEN_ARGS. You can additionally define
# MAVEN_ARGS_<package> for each individual package.
DEB_MAVEN_INVOKE = cd $(DEB_BUILDDIR) && $(JAVACMD) -classpath $(DEB_CLASSPATH) \
		 $(MAVEN_EXTRA_OPTS) -Dclassworlds.conf=/etc/maven2/m2-debian.conf \
		 -Dmaven.home=$(MAVEN_HOME) $(DEB_MAVEN_EXTRAPROPS) \
		 org.codehaus.classworlds.Launcher $(DEB_MAVEN_ARGS) \
		 -s/etc/maven2/settings-debian.xml \
		 $(if $(MAVEN_ARGS_$(cdbs_curpkg)),$(MAVEN_ARGS_$(cdbs_curpkg)),$(MAVEN_ARGS))

# TODO: DOES NOT WORK YET
#		 $(if $(DEB_MAVEN_PROPERTYFILE),-propertyfile $(DEB_MAVEN_PROPERTYFILE),)

# Targets to invoke for building, installing, testing and cleaning up.
# Building uses the default target from build.xml, installing and testing is
# only called if the corresponding variable is set. You can also specify
# multiple targets for each step.
DEB_MAVEN_BUILD_TARGET = compile jar:jar   # TODO: should be 'package'
DEB_MAVEN_INSTALL_TARGET =
DEB_MAVEN_CHECK_TARGET =
DEB_MAVEN_CLEAN_TARGET = clean

endif
