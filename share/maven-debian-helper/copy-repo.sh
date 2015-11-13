#!/bin/sh

# Copyright 2009 Torsten Werner.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

set -e

SRC_REPO="/usr/share/maven-repo"
DEST_REPO="$1/maven-repo"
PLUGIN_GROUPS="org.apache.maven.plugins org.codehaus.mojo"
METADATA_NAME="maven-metadata-local.xml"

find_src_poms() {
  find -L $SRC_REPO -name '*.pom' -printf '%P\n'
}

find_group_artifact_ids() {
  find -L $SRC_REPO/$1/* -type d -prune -printf '%f\n'
}

read_maven_plugin_xpath() {
  unzip -q -c "$1" META-INF/maven/plugin.xml 2>/dev/null | xmllint --xpath "$2" - 2>/dev/null || true
}

list_fakes()
{
  CONFFILES="/etc/maven/fake-poms.conf"
  if [ -r debian/fake-poms.conf ]
  then
    CONFFILES="$CONFFILES debian/fake-poms.conf"
  fi
  sed -e's,#.*,,' $CONFFILES
}

if [ -z "$1" ]; then
  echo "ABORT: missing destination dir"
  exit 1
fi

find_src_poms | while read POM; do
  VER_DIR=$(dirname $POM)
  BASEDIR=$(dirname $VER_DIR)
  mkdir -p $DEST_REPO/$BASEDIR
  ln -s $SRC_REPO/$VER_DIR $DEST_REPO/$BASEDIR/
done

list_fakes | while read GROUPID ARTIFACTID JARFILE VERSION
do
  GROUPDIR=$(echo $GROUPID | tr . /)
  BASEDIR="$DEST_REPO/$GROUPDIR/$ARTIFACTID"
  JARFILE=${JARFILE:-"/usr/share/java/$ARTIFACTID.jar"}
  VERSION=${VERSION:-"debian"}
  if [ -d $BASEDIR/$VERSION ]
  then
    echo "skip faking of existing $GROUPID:$ARTIFACTID::$VERSION"
    continue
  else
    mkdir -p $BASEDIR/$VERSION/
  fi
  if [ -r $JARFILE ]
  then
    PACKAGING="jar"
    ln -s $JARFILE $BASEDIR/$VERSION/$ARTIFACTID-$VERSION.jar
  else
    PACKAGING="pom"
  fi
  cat > $BASEDIR/$VERSION/$ARTIFACTID-$VERSION.pom <<.EOF
<project>
  <modelVersion>4.0.0</modelVersion>
  <groupId>$GROUPID</groupId>
  <artifactId>$ARTIFACTID</artifactId>
  <version>$VERSION</version>
  <packaging>$PACKAGING</packaging>
</project>
.EOF
done

# construct plugin metadata
for groupId in $PLUGIN_GROUPS; do
  GROUP=$(echo $groupId | tr . \/)
  if test ! -d "$DEST_REPO/$GROUP"; then
    continue
  fi

  # plugin group metadata
  cat > $DEST_REPO/$GROUP/$METADATA_NAME <<EOF
<metadata>
  <plugins>
EOF
  find_group_artifact_ids $GROUP | while read artifactId; do
    for jar in $SRC_REPO/$GROUP/$artifactId/*/*.jar; do
	  prefix=$(read_maven_plugin_xpath "$jar" '/plugin/goalPrefix/text()')
      if test -z "$prefix"; then
        continue
      fi
      name=$(read_maven_plugin_xpath "$jar" '/plugin/name/text()')
      cat >> $DEST_REPO/$GROUP/$METADATA_NAME <<EOF
    <plugin>
      <name>$name</name>
      <prefix>$prefix</prefix>
      <artifactId>$artifactId</artifactId>
    </plugin>
EOF
      break
    done
  done
  cat >> $DEST_REPO/$GROUP/$METADATA_NAME <<EOF
  </plugins>
</metadata>
EOF

  # plugin version metadata
  find_group_artifact_ids $GROUP | while read artifactId; do
    cat > $DEST_REPO/$GROUP/$artifactId/$METADATA_NAME <<EOF
<metadata>
  <groupId>$groupId</groupId>
  <artifactId>$artifactId</artifactId>
  <versioning>
    <versions>
EOF
    find $SRC_REPO/$GROUP/$artifactId/*/*.jar | while read jar; do
      version=$(basename $(dirname $jar))
      cat >> $DEST_REPO/$GROUP/$artifactId/$METADATA_NAME <<EOF
      <version>$version</version>
EOF
    done
    cat >> $DEST_REPO/$GROUP/$artifactId/$METADATA_NAME <<EOF
    </versions>
  </versioning>
</metadata>
EOF
  done
done
