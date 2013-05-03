# A debhelper build system class for handling Maven3-based projects.
#
# Copyright: 2013 Debian Java Maintainers
# License: GPL-3

package Debian::Debhelper::Buildsystem::maven3;

use strict;
use warnings;
use base 'Debian::Debhelper::Buildsystem::maven';

sub DESCRIPTION {
	"Maven3 (pom.xml)"
}

sub maven_dir {
	return '/usr/share/maven';
}

sub boot_jar {
	my $this=shift;
	return $this->maven_dir() . '/boot/plexus-classworlds-2.x.jar';
}

1
