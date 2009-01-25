#!/bin/sh

set -e

SRC_REPO="/usr/share/maven-repo"
DEST_REPO="$1/maven-repo"

find_all_poms() {
  find $SRC_REPO -name '*.pom' -printf '%P\n'
}

echo_property() {
  KEY=$(echo $BASE_DIR | tr / .)
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

find_all_poms | while read POM; do
  VER_DIR=$(dirname $POM)
  VERSION=$(basename $VER_DIR)
  BASE_DIR=$(dirname $VER_DIR)

  mkdir -p $DEST_REPO/$BASE_DIR
  ln -s $SRC_REPO/$VER_DIR $DEST_REPO/$BASE_DIR/
  
  VER_TAG="      <version>$VERSION</version>"
  echo "$VER_TAG" >> $DEST_REPO/$BASE_DIR/maven-metadata-tmp.xml
  
  echo_property
done > $1/auto.properties

find_all_meta | while read META; do
  DIR=$(dirname $META)
  echo_meta > $DIR/maven-metadata-local.xml
  rm -f $META
done

