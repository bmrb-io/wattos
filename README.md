# wattos

Archival copy of the restraints-grid servlet code by Jurgen Doreleijers,
plus a Docker Compose deployment of the NMR Restraints Grid (NRG)
servlet that fronts it.

  * `wattos_orig.tar.gz` — original SVN checkout from Google Code
  * `wattos/` — Java source tree (Ant-based, builds as of 2019), plus
    the exploded webapp under `wattos/NRG/` that Tomcat serves directly
  * `initdb/` — SQL bootstrap scripts run on first MySQL startup
    (`01_createdb.sql` + `wattos_load.sql`)
  * `dbfs/` — block files and seed CSVs bind-mounted into the DB
  * `tomcat/` — Tomcat `server.xml` mounted into the Tomcat container
  * `mysql/` — legacy pre-compose deployment artefacts (no longer used)
  * `scripts/rebuild.sh` — incremental Java rebuild used in place of
    the full Ant build

## wattos

Wattos was originally on Google Code; its commit history was lost when
Google Code shut down. It was imported into git from a local working
copy in 2019, a decade after it was written.

## Running with Docker Compose

`docker compose up -d` from this directory brings up two containers on
a `wattos-network` bridge:

  * **`wattos-mysql`** — `mysql:8` (8.4 LTS). On first start it runs
    everything in `initdb/` (schema + data load); subsequent starts
    reuse the named volume `wattos_mysql-data`. The MySQL 5.7 query
    cache is gone in 8.0+, so warm-page caching now happens
    application-side (see Tomcat below).
  * **`wattos-tomcat`** — `tomcat:11-jdk21-temurin` (Tomcat 11 on
    Temurin JDK 21). The exploded webapp at `wattos/NRG/` is
    bind-mounted into the container as `/usr/local/tomcat/webapps/NRG/`,
    so jar and class changes show up as ordinary git diffs. Only port
    8080 is published, on `127.0.0.1`. JVM heap is set to `-Xmx768m`
    to leave room for the in-process response cache (256 MB Caffeine
    LRU in `MRGridServlet`, replacing the old MySQL query cache —
    landing page goes from ~2.5s cold to ~2 ms warm).

### Configuration

Bind-mount sources and the mysql `user:` are parameterised via
`${VAR:-default}` in `docker-compose.yml`. Defaults are the production
absolute paths under `/projects/BMRB/public/wattos/...`, so the live
server keeps working with no `.env` file.

For local development, copy `.env.example` to `.env` (gitignored) and
set `INITDB_DIR`, `DBFS_DIR`, `HTML_DIR`, `RUNTIME_PROPS`, `NRG_DIR`,
`PDB_DIR`, `MYSQL_USER` to match your checkout.

### Common operations

```bash
# Bring everything up (or apply compose changes after a git pull)
docker compose up -d

# Restart just Tomcat (e.g. after rebuilding Java sources)
docker compose restart wattos-tomcat

# Wipe the DB volume and re-run initdb (~30s on prod hardware)
docker compose down -v && docker compose up -d

# Smoke test the servlet
curl -s -o /dev/null -w '%{http_code}\n' \
    http://127.0.0.1:8080/NRG/MRGridServlet
```

The servlet's only mapping is `Wattos.Servlet.MRGridServlet` →
`/MRGridServlet` (see `wattos/NRG/WEB-INF/web.xml`).

## Rebuilding after Java source changes

The full Ant build (`wattos/buildEclipse.xml`) currently can't run
cleanly — `wattos/lib/` is missing several historical compile-time
jars. For day-to-day work, use the incremental rebuild script:

```bash
# Rebuild one or more .java files into wattos/NRG/WEB-INF/classes/
./scripts/rebuild.sh wattos/src/Wattos/Servlet/MRGridServlet.java
./scripts/rebuild.sh wattos/src/Wattos/Episode_II/SQL_*.java

# Pick up the new .class files
docker compose restart wattos-tomcat
```

The script compiles against the existing compiled tree, the jars in
`wattos/NRG/WEB-INF/lib/`, and `servlet-api.jar` extracted from the
running Tomcat container, and writes the resulting `.class` files back
into `wattos/NRG/WEB-INF/classes/`. Both the jars and the compiled
classes are tracked in git, so dependency or behaviour changes show up
as ordinary diffs.

## Reverse proxy

nginx fronts the container with stock `proxy_pass`:

```
location /NRG/ {
    proxy_pass http://127.0.0.1:8080/NRG/;
}
```

AJP is no longer used (the connector was removed when the unmaintained
`nginx_ajp_module` was retired). If AJP ever needs to come back, add a
`<Connector protocol="AJP/1.3" .../>` to `tomcat/server.xml` with a
`secret="..."` attribute (Tomcat 9.0.31+ rejects AJP without one).

For an Apache front-end, the equivalent is:

```apache
<Proxy *>
    Require all granted
</Proxy>

ProxyPass        /NRG/ http://127.0.0.1:8080/NRG/
ProxyPassReverse /NRG/ http://127.0.0.1:8080/NRG/

RedirectMatch permanent ^/$        /NRG/MRGridServlet
RedirectMatch permanent ^/NRG(/*)$ /NRG/MRGridServlet
```

## Volumes / bind mounts

DB filesystem (mounted at `/wattos/` in both containers):

  * `dbfs/bfiles/` — block files served by the servlet
  * `dbfs/entry.csv`, `mrblock.csv`, `mrfile.csv` — seed data loaded
    by `initdb/wattos_load.sql` on first DB start
  * `html/molgrap/` — molecular-graphics images

Servlet-only:

  * `/pdb/data/structures` — wwPDB structures archive (point at any
    directory if you don't have a local mirror)

Paths are still hardcoded in the SQL scripts and in
`wattos.runtime.properties*` (`/wattos/dbfs`, `/pdb/data/structures`,
`/wattos/html/molgrap/...`), so the bind-mount targets above are not
freely renameable.
