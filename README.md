# wattos

Archival copy of restraints grid servlet code by Jurgen Dorelejers
plus containerized version of the NMR Restraints Grid servlet.

  * `wattos_orig.tar.gz` : SVN checkout from Google Code
  * `wattos/` : source tree that builds as of 2019
  * `mysql/` : MariaDB Docker container
  * `tomcat/` : Tomcat Docker container

## wattos
------

Wattos was originally on google code and its commit history went away with that.
It was imported into git from out local working copy in 2019, a decade after it's
been written.

## NRG containers:
-------------------------------

1. mysql

2. tomcat

#1 needs to be reachable by #2 so mysql server's ip address is hardcoded to 172.18.0.2

  - on centos 6 docker 1.7.1:

    -- put ''other_args=--bip=172.18.0.1/24'' in /etc/sysconfig/docker and delete the exsisting bridge
before starting docker daemon

  - on newer docker:

    -- ''docker network create <somename> --subnet 172.18.0.0/24''

    -- run both containers with ''--network <somename>''

Mysql server's ip address is hardcoded in wattos.properties file in the tomcat container 
and also in mysql container's startup script. Mysql container must be run with
"--cap-add=NET_ADMIN".

### Volumes:
--------

DB filesystem (/wattos in the containers):
 - bfiles
 - wattos tables (need to be loaded on startup & update w/ docker exec)

-- NOTE that the paths are hardcoded in SQL scripts and servlet's wattos.properties

ftp/pub/wwpdb/images (/wattos/html/molgrap/images in the container)
ftp/pub/wwpdb/imagesWhite - as above
ftp/pub/pdb/data/structures (/pdb/data/structures in the container)
-- servlet container only

  -- can be created as "volumes" with
    ''docker volume create --driver local -o type=nfs -o o=addr=144.92.167.223,ro -o device=:/pdb_ftp/pdb pdbftp_ro''
    if not local

On the httpd vhost: port 8009 exposed and apache config
    ProxyPass         /NRG/ ajp://localhost:8009/NRG/
    ProxyPassReverse  /NRG/ ajp://localhost:8009/NRG/
