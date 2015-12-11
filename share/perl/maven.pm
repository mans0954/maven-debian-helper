# A debhelper build system class for handling Maven-based projects.
#
# Copyright: 2011 Canonical Ltd.
# License: GPL-3

package Debian::Debhelper::Buildsystem::maven;

use strict;
use base 'Debian::Debhelper::Buildsystem';
use Debian::Debhelper::Dh_Lib qw(%dh doit);

sub DESCRIPTION {
	"Maven (pom.xml)"
}

sub check_auto_buildable {
	my $this=shift;
	return (-e $this->get_sourcepath("pom.xml")) ? 1 : 0;
}

sub new {
	my $class=shift;
	my $this=$class->SUPER::new(@_);
	my $java_home = (exists $ENV{JAVA_HOME}) ? $ENV{JAVA_HOME} : '/usr/lib/jvm/default-java';

	my @packages = @{$dh{DOPACKAGES}};
	$this->{package} = shift @packages;
	$this->{doc_package} = (grep /-doc$/, @packages)[0];
	my $classconf = '/etc/maven/m2-debian.conf';

	my @classpath = ('/usr/share/maven/boot/plexus-classworlds-2.x.jar');
	if (-e "$java_home/lib/tools.jar") {
		push(@classpath, "$java_home/lib/tools.jar");
	}

	my @jvmopts = ('-noverify', '-cp', join(':',@classpath),
		"-Dmaven.home=/usr/share/maven",
		"-Dmaven.multiModuleProjectDirectory=$this->{cwd}",
		"-Dclassworlds.conf=$classconf");
	if (-e "$this->{cwd}/debian/maven.properties") {
		push (@jvmopts, "-Dproperties.file.manual=$this->{cwd}/debian/maven.properties");
	}

	@{$this->{maven_cmd}} = ($java_home . '/bin/java',
		@jvmopts,
		"org.codehaus.plexus.classworlds.launcher.Launcher",
		"-s/etc/maven/settings-debian.xml",
		"-Ddebian.dir=$this->{cwd}/debian",
		"-Dmaven.repo.local=$this->{cwd}/debian/maven-repo");
	return $this;
}

sub configure {
	my $this=shift;
	my @patch_args;

	doit("/usr/share/maven-debian-helper/copy-repo.sh", "$this->{cwd}/debian");
	$this->doit_in_sourcedir("mh_patchpoms", "-p$this->{package}",
		"--debian-build", "--keep-pom-version",
		"--maven-repo=$this->{cwd}/debian/maven-repo", @patch_args);
	doit("touch", "debian/stamp-poms-patched");
}

sub build {
 	my $this=shift;

	if (!@_) {
		push(@_, "package");
		if ($this->{doc_package}) {
			push(@_, "javadoc:jar", "javadoc:aggregate");
		}
	}

	push(@_, "-DskipTests", "-Dnotimestamp=true", "-Dlocale=en_US");

	$this->doit_in_builddir(@{$this->{maven_cmd}}, @_);
}

sub test {
	my $this=shift;

	if (!@_) {
		push(@_, "test");
	}

	$this->doit_in_builddir(@{$this->{maven_cmd}}, @_);
}

sub install {
	my $this=shift;
	my @resolvedep_args;
	my @install_args = @_;
	shift @install_args;

	opendir(my $dirhandle, "/usr/share/maven-repo/org/debian/maven/maven-packager-utils/")
		|| die "maven debian helper not found";
	my $maven_debian_version = (grep { !/^\./ } readdir($dirhandle))[0];
	closedir $dirhandle;

	if ($this->{doc_package}) {
		push(@resolvedep_args, "--javadoc");
	}
	push(@resolvedep_args, "--base-directory=$this->{cwd}", "--non-explore");

	$this->doit_in_builddir(@{$this->{maven_cmd}}, @install_args,
		"-Ddebian.dir=$this->{cwd}/debian",
		"-Ddebian.package=$this->{package}",
		"-Dmaven.repo.local=$this->{cwd}/debian/maven-repo",
		"-Dinstall.to.usj=true",
		"org.debian.maven:debian-maven-plugin:$maven_debian_version:install");
	$this->doit_in_builddir("mh_resolve_dependencies", "--non-interactive",
		"--offline", "-p$this->{package}", @resolvedep_args);
	if ($this->{doc_package}) {
		doit("cp","debian/$this->{package}.substvars",
			"debian/$this->{doc_package}.substvars");
		# clean up generated docs
		$this->doit_in_builddir("rm", "-f", "target/apidocs/*.sh",
			"target/apidocs/options");
	}
}

sub clean {
	my $this=shift;

	# If this directory if absent, we must not have anything to clean;
	# don't populate the directory just to run a clean target.
	if (-e "$this->{cwd}/debian/maven-repo")
	{
		$this->doit_in_builddir_noerror(@{$this->{maven_cmd}}, "clean");
		doit("rm", "-r", "$this->{cwd}/debian/maven-repo");
	}
	$this->doit_in_builddir('for dir in $(find . -name target -type d); do if [ -f $(echo $dir | sed -e s/target$/pom.xml/) ]; then rm -Rf $dir; fi done');
	$this->doit_in_sourcedir("mh_unpatchpoms", "-p$this->{package}");
	doit("rm", "-f", "debian/stamp-poms-patched");
	doit("mh_clean");
}

1
