#!/bin/sh

# This horror script is used to bootstrap maven-debian-helper.

set -e

JAVA_HOME=/usr/lib/jvm/java-6-openjdk
REPO=/usr/share/maven-repo
ODM=$REPO/org/debian/maven
VERSION=$(dpkg-parsechangelog | sed -ne 's/Version: //p')
DESTDIR=debian/maven-debian-helper

boot() {
  PKG=org/apache/maven/project
  POM=$PKG/pom-4.0.0.xml
  DIR=maven-debian-helper/src/main/resources
  mkdir -p org/apache/maven/project/
  sed '/startworkaround/,/endworkaround/d' $DIR/$POM > $POM
  jar cf boot.jar org
  rm -rf org
  sed -e's,/.*/maven-debian-helper.jar,boot.jar,' \
      -e's,org.debian.maven.Wrapper,org.apache.maven.cli.MavenCli,' \
      etc/m2-debian.conf > boot.conf
}

scan() {
  P_ARCHIVER=$(ls $REPO/org/codehaus/plexus/plexus-archiver/)
  P_INTERPOLATION=$(ls $REPO/org/codehaus/plexus/plexus-interpolation/)
  M_RESOURCES_P=$(ls $REPO/org/apache/maven/plugins/maven-resources-plugin/)
  M_PLUGIN_P=$(ls $REPO/org/apache/maven/plugins/maven-plugin-plugin/)
}

maven() {
  $JAVA_HOME/bin/java -cp /usr/share/maven2/boot/classworlds.jar \
    -D"classworlds.conf=boot.conf" org.codehaus.classworlds.Launcher \
    -s"etc/settings-debian.xml" package \
    -D"org.codehaus.plexus.plexus-archiver.version=$P_ARCHIVER" \
    -D"org.codehaus.plexus.plexus-interpolation.version=$P_INTERPOLATION" \
    -D"org.apache.maven.plugins.maven-resources-plugin.version=$M_RESOURCES_P" \
    -D"org.apache.maven.plugins.maven-plugin-plugin.version=$M_PLUGIN_P" \
    "$@"
}

debian_install() {
  # parent pom
  install -D -m644 pom.xml \
    $DESTDIR/$ODM/maven-debian/$VERSION/maven-debian-$VERSION.pom
  # maven-debian-helper
  dh_install -pmaven-debian-helper maven-debian-helper/target/*.jar \
    $ODM/maven-debian-helper/$VERSION/
  dh_link -pmaven-debian-helper \
    $ODM/maven-debian-helper/$VERSION/maven-debian-helper-$VERSION.jar \
    /usr/share/java/maven-debian-helper.jar
  install -D -m644 maven-debian-helper/pom.xml \
    $DESTDIR/$ODM/maven-debian-helper/$VERSION/maven-debian-helper-$VERSION.pom
  # maven-debian-plugin
  dh_install -pmaven-debian-helper maven-debian-plugin/target/*.jar \
    $ODM/maven-debian-plugin/$VERSION/
  dh_link -pmaven-debian-helper \
    $ODM/maven-debian-plugin/$VERSION/maven-debian-plugin-$VERSION.jar \
    /usr/share/java/maven-debian-plugin.jar
  install -D -m644 maven-debian-plugin/pom.xml \
    $DESTDIR/$ODM/maven-debian-plugin/$VERSION/maven-debian-plugin-$VERSION.pom
}

cleanup() {
  maven clean
  rm -f boot.jar boot.conf
}

boot
scan
maven package
debian_install
cleanup

