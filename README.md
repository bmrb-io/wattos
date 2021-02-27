# wattos

Archival copy of restraints grid servlet code by Jurgen Dorelejers
plus containerized version of the NMR Restraints Grid servlet.

  * `wattos_orig.tar.gz` : SVN checkout from Google Code
  * `wattos/` : source tree that builds as of 2019
  * `mysql/` : DB Docker container
  * `tomcat/` : Tomcat Docker container

## wattos
------

Wattos was originally on google code and its commit history went away with that.
It was imported into git from out local working copy in 2019, a decade after it
was written.

## NRG containers:
-------------------------------

1. mysql

2. tomcat

Tomcat container needs to talk to mysql, the easiest way to do this is `--link` (where supported).
Tomcat container also needs `mysql` server's (container's) name in `wattos.properties` file.

### Volumes:
--------

DB filesystem (`/wattos` in the containers):
 - bfiles
 - wattos tables (need to be loaded on startup & update w/ docker exec)

-- *NOTE* that the paths are hardcoded in SQL scripts and servlet's `wattos.properties`
 
DB init to create wattos DB on `mysql` startup. 

ftp/pub/wwpdb/images (/wattos/html/molgrap/images in the container)
ftp/pub/wwpdb/imagesWhite - as above
ftp/pub/pdb/data/structures (/pdb/data/structures in the container)
-- servlet container only

## httpd config

apache 2.4, adjust to taste
```
<Proxy *>
    Require all granted
</Proxy>

ProxyPass         /NRG/ ajp://127.0.0.1:8009/NRG/
ProxyPassReverse  /NRG/ ajp://127.0.0.1:8009/NRG/
AddOutputFilterByType SUBSTITUTE text/html

RedirectMatch permanent ^/$ /NRG/MRGridServlet
RedirectMatch permanent ^/NRG(/*)$ /NRG/MRGridServlet

<DirectoryMatch /CVS/>
    Require all denied
</DirectoryMatch>
<DirectoryMatch /.svn/>
    Require all denied
</DirectoryMatch>
<DirectoryMatch /.git/>
    Require all denied
</DirectoryMatch>

DocumentRoot "/websites/wattos/html"
<Directory "/websites/wattos/html">
    AllowOverride None
    Require all granted
</Directory> 
```
