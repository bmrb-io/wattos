#!/bin/sh

docker run -d \
    --restart=always \
    -p 8009:8009 -p 8080:8080 \
    -e "JAVA_OPTS=-Duser.home=/" \
    -v /projects/BMRB/public/wattos/wattos/wattos.runtime.properties.uchc:/wattos.runtime.properties \
    -v /projects/BMRB/public/wattos/wattos/NRG.war:/usr/local/tomcat/webapps/NRG.war \
    -v /projects/BMRB/public/wattos/dbfs:/wattos/dbfs \
    -v /projects/BMRB/public/ftp/pub/pdb:/pdb \
    --name=wattos-tomcat \
    --link wattos-mysql \
    tomcat:7-jre8-alpine
#
