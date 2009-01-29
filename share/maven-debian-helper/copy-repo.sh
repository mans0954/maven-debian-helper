#!/bin/sh

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

find_dest_poms() {
  find -L $DEST_REPO -name '*.pom' -printf '%P\n'
}

find_all_meta() {
  find $DEST_REPO -name 'maven-metadata-tmp.xml'
}

echo_property() {
  KEY=$(echo $BASEDIR | tr / .)
  case "$KEY" in
    *.maven-*-plugin|*-maven-plugin)
      echo "$KEY.version = $VERSION"
      return
      ;;
    *)
      echo "$KEY.version = [$VERSION]"
      return
      ;;
  esac
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

find_dest_poms | while read POM; do
  VER_DIR=$(dirname $POM)
  VERSION=$(basename $VER_DIR)
  BASEDIR=$(dirname $VER_DIR)
  VER_TAG="      <version>$VERSION</version>"
  echo "$VER_TAG" >> $DEST_REPO/$BASEDIR/maven-metadata-tmp.xml
  echo_property
done > $1/auto.properties

find_all_meta | while read META; do
  DIR=$(dirname $META)
  echo_meta > $DIR/maven-metadata-local.xml
  rm -f $META
done

