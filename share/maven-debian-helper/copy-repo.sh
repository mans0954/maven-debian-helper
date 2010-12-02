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

find_src_poms() {
  find -L $SRC_REPO -name '*.pom' -printf '%P\n'
}

list_fakes()
{
  CONFFILES="/etc/maven2/fake-poms.conf"
  if [ -r debian/fake-poms.conf ]
  then
    CONFFILES="$CONFFILES debian/fake-poms.conf"
  fi
  sed -e's,#.*,,' $CONFFILES
}

find_all_meta() {
  find $DEST_REPO -name 'maven-metadata-tmp.xml'
}

header() {
  echo '<?xml version="1.0" encoding="UTF-8"?>'
  echo '<metadata>'
  echo '  <versioning>'
  echo '    <versions>'
}

footer() {
  echo '    </versions>'
  echo '  </versioning>'
  echo '</metadata>'
}

echo_meta() {
  header
  cat $META
  footer
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

find_all_meta | while read META; do
  DIR=$(dirname $META)
  echo_meta > $DIR/maven-metadata-local.xml
  rm -f $META
done

