Tomcat container needs 

1. Servlet war file(s) in /usr/local/tomcat/webapps

2. These are configurable via wattos.runtime.properties:

ftp/pub/wwpdb/images (/wattos/html/molgrap/images in the container)
ftp/pub/wwpdb/imagesWhite - as above
ftp/pub/pdb/data/structures (/pdb/data/structures in the container)

3. A local network <net> to talk to mysql container

4. Mysql container's ip address that goes into wattos.runtime.properties
-- i.e. mysql container is the one that need fixed ip, this one doesn't really.

5. Host apache configured to
    ProxyPass         /NRG/ ajp://localhost:8009/NRG/
    ProxyPassReverse  /NRG/ ajp://localhost:8009/NRG/

Note that images are served by apache as well, they won't appear when testing
the servlet on port 8080. Apache needs DocumentRoot at /websites/wattos/html
for that.

Command line is 
docker run -d \
    --restart=always \
    -p 8009:8009 -p 8080:8080 \
    -v /websites/wattos/dbfs:/wattos/dbfs \
    -v /websites/pdb:/pdb \
    --name=<name> \
    --network=<net> \
    --ip=<addr> \
    <image>

#    -v /websites/wattos/html:/wattos/html \

6. 20190213: just run the base image tomcat:7-jre8-alpine with
    -e "JAVA_OPTS=-Duser.home=/" \
    -v /websites/wattos/etc/wattos.runtime.properties:/wattos.runtime.properties \
    -v /websites/wattos/etc/NRG.war:/usr/local/tomcat/webapps/NRG.war
in addition to the above.