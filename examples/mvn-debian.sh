#!/bin/sh

exec java -cp /usr/share/java/classworlds.jar:/usr/share/java/debian-maven-boot.jar \
  -Dclassworlds.conf=/etc/maven2/m2-offline.conf \
  -Dproperties.file.manual=debian/maven.properties \
  -Dproperties.file.auto=debian/auto.properties \
  org.debian.maven.Wrapper "$@"
