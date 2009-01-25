#!/bin/sh

set -e

SRC_REPO="/usr/share/maven-repo"
DEST_REPO="$1/maven-repo"

find_all_poms() {
  find $SRC_REPO -name '*.pom' -printf '%P\n'
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
  VER_TAG="      <version>$(basename $VER_DIR)</version>"
  BASE_DIR=$(dirname $VER_DIR)
  mkdir -p $DEST_REPO/$BASE_DIR
  ln -s $SRC_REPO/$VER_DIR $DEST_REPO/$BASE_DIR/
  echo "$VER_TAG" >> $DEST_REPO/$BASE_DIR/maven-metadata-tmp.xml
done

find_all_meta | while read META; do
  DIR=$(dirname $META)
  echo_meta > $DIR/maven-metadata.xml
  rm -f $META
done

find -L $DEST_REPO -name "*.pom" | \
  sed -e"s,^$DEST_REPO/,," \
      -e"s,/\([0-9][^/]*\).*,.version = \1," \
      -e"s,/,.,g"                            > $1/auto.properties
