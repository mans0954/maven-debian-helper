# This file is based on ant.mk

# Copyright © 2003 Stefan Gybas <sgybas@debian.org>
# Copyright © 2008 Torsten Werner <twerner@debian.org>
# Copyright © 2009-2011 Ludovic Claude <ludovic.claude@laposte.net>
# Copyright © 2010-2011 Damien Raude-Morvan <drazzib@debian.org>
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

DEB_MAVEN_REPO := $(CURDIR)/debian/maven-repo

JAVA_OPTS = \
  $(shell test -n "$(DEB_MAVEN_PROPERTYFILE)" && echo -Dproperties.file.manual=$(DEB_MAVEN_PROPERTYFILE))

DEB_PHONY_RULES += maven-sanity-check before-mvn-build mvn-build after-mvn-build patch-poms unpatch-poms

cdbs_use_maven_substvars := $(shell grep -q "{maven:\w*Depends}" debian/control && echo yes)
cdbs_new_poms_file := $(shell test ! -f debian/$(DEB_JAR_PACKAGE).poms && echo yes)
cdbs_new_maven_rules_file := $(shell test ! -f debian/maven.rules && echo yes)

maven-sanity-check:
	@if ! test -x "$(JAVACMD)"; then \
		echo "You must specify a valid JAVA_HOME or JAVACMD!"; \
		exit 1; \
	fi
	@if ! test -r "$(MAVEN_HOME)/boot/classworlds.jar"; then \
		echo "You must specify a valid MAVEN_HOME directory!"; \
		exit 1; \
	fi

debian/$(DEB_JAR_PACKAGE).poms:
	mh_lspoms --non-interactive --offline -p$(DEB_JAR_PACKAGE)

debian/maven.rules:
	mh_lspoms --non-interactive --offline -p$(DEB_JAR_PACKAGE) --force

ifeq (, $(DEB_DOC_PACKAGE))
DEB_PATCHPOMS_ARGS += --build-no-docs
debian/stamp-maven-build: override MAVEN_CLASSCONF = /etc/maven2/m2-debian-nodocs.conf
endif

debian/stamp-poms-patched: debian/maven-repo
	mh_patchpoms -p$(DEB_JAR_PACKAGE) --debian-build --keep-pom-version --maven-repo=$(DEB_MAVEN_REPO) $(DEB_PATCHPOMS_ARGS)
	touch debian/stamp-poms-patched

patch-poms: debian/$(DEB_JAR_PACKAGE).poms debian/maven.rules debian/stamp-poms-patched

unpatch-poms: debian/$(DEB_JAR_PACKAGE).poms
	mh_unpatchpoms -p$(DEB_JAR_PACKAGE)
	$(RM) -f debian/stamp-poms-patched

debian/maven-repo:
	/usr/share/maven-debian-helper/copy-repo.sh $(CURDIR)/debian

post-patches:: patch-poms

clean:: unpatch-poms
	mh_clean

common-build-arch common-build-indep:: maven-sanity-check debian/stamp-maven-build
debian/stamp-maven-build: debian/maven-repo before-mvn-build mvn-build after-mvn-build
mvn-build:
	# before-build target may be used to unpatch the pom files, so we need to check if
	# patching the pom files is needed here, normally not
	if [ ! -f pom.xml.save ]; then \
		$(MAKE) -f debian/rules patch-poms; \
	fi
	$(DEB_MAVEN_INVOKE) $(DEB_MAVEN_BUILD_TARGET)
	touch $@
# Placeholders to insert custom processing before and after a Maven build
before-mvn-build::
after-mvn-build::

cleanbuilddir:: DEB_PATCHPOMS_ARGS += --ignore-rules=debian/maven.ignoreRules --clean-ignore-rules=debian/maven.cleanIgnoreRules
cleanbuilddir:: maven-sanity-check post-patches debian/maven-repo
	-$(DEB_MAVEN_INVOKE) $(DEB_MAVEN_CLEAN_TARGET)
	$(RM) -r $(DEB_MAVEN_REPO) debian/stamp-maven-build
	$(if $(cdbs_new_poms_file), $(RM) debian/$(DEB_JAR_PACKAGE).poms)
	$(if $(cdbs_new_maven_rules_file), $(RM) debian/maven.rules)
	$(if $(cdbs_use_maven_substvars), $(RM) debian/*.substvars)
	$(MAKE) -f debian/rules unpatch-poms

# extra arguments for the installation step
PLUGIN_ARGS = -Ddebian.dir=$(CURDIR)/debian -Ddebian.package=$(DEB_JAR_PACKAGE) -Dmaven.repo.local=$(DEB_MAVEN_REPO) -Dinstall.to.usj=$(DEB_MAVEN_INSTALL_TO_USJ)

ifneq (, $(DEB_DOC_PACKAGE))
DEB_RESOLVEDEP_ARGS += --javadoc
endif
DEB_RESOLVEDEP_ARGS += --base-directory=$(CURDIR) --non-explore

common-install-arch common-install-indep:: common-install-impl
common-install-impl::
	$(if $(DEB_MAVEN_INSTALL_TARGET),$(DEB_MAVEN_INVOKE) $(PLUGIN_ARGS) $(DEB_MAVEN_INSTALL_TARGET),@echo "DEB_MAVEN_INSTALL_TARGET unset, skipping default maven.mk common-install target")
	$(if $(cdbs_use_maven_substvars), mh_resolve_dependencies --non-interactive --offline -p$(DEB_JAR_PACKAGE) $(DEB_RESOLVEDEP_ARGS) )

ifeq (,$(findstring nocheck,$(DEB_BUILD_OPTIONS)))
common-build-arch common-build-indep:: debian/stamp-maven-check
debian/stamp-maven-check: debian/stamp-maven-build
	$(if $(DEB_MAVEN_CHECK_TARGET),$(DEB_MAVEN_INVOKE) $(PLUGIN_ARGS) $(DEB_MAVEN_CHECK_TARGET),@echo "DEB_MAVEN_CHECK_TARGET unset, not running checks")
	$(if $(DEB_MAVEN_CHECK_TARGET),touch $@)

clean:: 
	$(if $(DEB_MAVEN_CHECK_TARGET),$(RM) debian/stamp-maven-check)
endif

ifneq (,$(DEB_DOC_PACKAGE))
# extra arguments for the installation step
PLUGIN_DOC_ARGS = -Ddebian.dir=$(CURDIR)/debian -Ddebian.package=$(DEB_DOC_PACKAGE)

common-build-arch common-build-indep:: debian/stamp-maven-doc
debian/stamp-maven-doc: debian/stamp-maven-build
	if [ ! -f pom.xml.save ]; then \
		$(MAKE) -f debian/rules patch-poms; \
	fi
	$(if $(DEB_MAVEN_DOC_TARGET),$(DEB_MAVEN_INVOKE) $(PLUGIN_DOC_ARGS) $(DEB_MAVEN_DOC_TARGET),@echo "DEB_MAVEN_DOC_TARGET unset, not generating documentation")
	$(if $(DEB_MAVEN_DOC_TARGET),touch $@)

common-install-impl:: 
	$(if $(DEB_MAVEN_INSTALL_DOC_TARGET),$(DEB_MAVEN_INVOKE) $(PLUGIN_DOC_ARGS) $(DEB_MAVEN_INSTALL_DOC_TARGET),@echo "DEB_MAVEN_INSTALL_DOC_TARGET unset, skipping documentation maven.mk common-install target")
	$(if $(cdbs_use_maven_substvars), cp debian/$(DEB_JAR_PACKAGE).substvars debian/$(DEB_DOC_PACKAGE).substvars)
	# cleanup generated docs
	$(RM) -f target/apidocs/*.sh target/apidocs/options 

clean:: 
	$(if $(DEB_MAVEN_DOC_TARGET),$(RM) debian/stamp-maven-doc)
endif

endif
