# This file is based on ant.mk

# Copyright © 2003 Stefan Gybas <sgybas@debian.org>
# Copyright © 2008 Torsten Werner <twerner@debian.org>
# Description: Builds and cleans packages which have an Maven pom.xml file
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

ifndef _cdbs_class_maven
_cdbs_class_maven = 1

include $(_cdbs_rules_path)/buildcore.mk$(_cdbs_makefile_suffix)
include $(_cdbs_class_path)/maven-vars.mk$(_cdbs_makefile_suffix)

DEB_MAVEN_REPO := /usr/share/maven-repo

JAVA_OPTS = \
  $(shell test -n "$(DEB_MAVEN_PROPERTYFILE)" && echo -Dproperties.file.manual=$(DEB_MAVEN_PROPERTYFILE)) \
  -Dproperties.file.auto=$(CURDIR)/debian/auto.properties

DEB_PHONY_RULES += maven-sanity-check

maven-sanity-check:
	@if ! test -x "$(JAVACMD)"; then \
		echo "You must specify a valid JAVA_HOME or JAVACMD!"; \
		exit 1; \
	fi
	@if ! test -r "$(MAVEN_HOME)/boot/classworlds.jar"; then \
		echo "You must specify a valid MAVEN_HOME directory!"; \
		exit 1; \
	fi

debian/auto.properties:
	find $(DEB_MAVEN_REPO) -name '*.pom' | \
	  sed -e's,^$(DEB_MAVEN_REPO)/,,' \
	      -e's,/\([0-9][^/]*\).*,.version = \1,' \
	      -e's,/,.,g'                            > $@

common-build-arch common-build-indep:: debian/stamp-maven-build maven-sanity-check
debian/stamp-maven-build: debian/auto.properties
	$(DEB_MAVEN_INVOKE) $(DEB_MAVEN_BUILD_TARGET)
	touch $@

cleanbuilddir:: maven-sanity-check apply-patches debian/auto.properties
	-$(DEB_MAVEN_INVOKE) $(DEB_MAVEN_CLEAN_TARGET)
	$(RM) debian/auto.properties debian/stamp-maven-build

# extra arguments for the installation step
PLUGIN_ARGS = -Ddebian.dir=$(CURDIR)/debian -Ddebian.package=$(DEB_JAR_PACKAGE)

common-install-arch common-install-indep:: common-install-impl
common-install-impl::
	$(if $(DEB_MAVEN_INSTALL_TARGET),$(DEB_MAVEN_INVOKE) $(DEB_MAVEN_INSTALL_TARGET) $(PLUGIN_ARGS),@echo "DEB_MAVEN_INSTALL_TARGET unset, skipping default maven.mk common-install target")

ifeq (,$(findstring nocheck,$(DEB_BUILD_OPTIONS)))
common-build-arch common-build-indep:: debian/stamp-maven-check
debian/stamp-maven-check: debian/stamp-maven-build
	$(if $(DEB_MAVEN_CHECK_TARGET),$(DEB_MAVEN_INVOKE) $(DEB_MAVEN_CHECK_TARGET),@echo "DEB_MAVEN_CHECK_TARGET unset, not running checks")
	$(if $(DEB_MAVEN_CHECK_TARGET),touch $@)

clean::
	$(if $(DEB_MAVEN_CHECK_TARGET),$(RM) debian/stamp-maven-check)
endif

endif
