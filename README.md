# wattos

Archival copy of the restraints-grid servlet code by Jurgen Doreleijers,
plus a Docker Compose deployment of the NMR Restraints Grid (NRG)
servlet that fronts it.

  * `wattos_orig.tar.gz` — original SVN checkout from Google Code
  * `wattos/` — Java source tree (Ant-based), plus the exploded webapp
    under `wattos/NRG/` that Tomcat serves directly. Compiled classes
    and runtime jars under `NRG/WEB-INF/` are tracked in git, so a
    fresh checkout starts up without a build step.
  * `initdb/` — SQL bootstrap scripts run on first MySQL startup
    (`01_createdb.sql` + `wattos_load.sql`)
  * `dbfs/` — block files and seed CSVs bind-mounted into the DB
  * `tomcat/` — Tomcat `server.xml` mounted into the Tomcat container
  * `mysql/` — legacy pre-compose deployment artefacts (no longer used)
  * `scripts/rebuild.sh` — incremental single-file Java rebuild for
    quick edits (Ant alternative for one-off changes)

## wattos

Wattos was originally on Google Code; its commit history was lost when
Google Code shut down. It was imported into git from a local working
copy in 2019, a decade after it was written.

## Quick start (fresh checkout)

You don't need a JDK or Ant locally — the deployed webapp ships with its
compiled classes and runtime jars under `wattos/NRG/WEB-INF/`. Docker is
the only prerequisite.

```bash
git clone <this repo>
cd wattos                     # the deployment root (contains docker-compose.yml)
docker compose up -d          # starts wattos-mysql and wattos-tomcat
                              # initdb seeds the DB on first start (~30s)

# Smoke test:
curl -s -o /dev/null -w '%{http_code}\n' \
    http://127.0.0.1:8080/NRG/MRGridServlet
# → 200
```

That's it. The landing page is at
`http://127.0.0.1:8080/NRG/MRGridServlet` and works straight out of git
because `NRG/WEB-INF/classes/Wattos/**/*.class` and
`NRG/WEB-INF/lib/*.jar` are checked in. For local-path dev (rather than
the production paths the `docker-compose.yml` defaults point at), copy
`.env.example` to `.env` and set the bind-mount vars — see
[Configuration](#configuration) below.

## Running with Docker Compose

`docker compose up -d` brings up two containers on a `wattos-network`
bridge:

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

You need a JDK (≥17) and Ant only when modifying `.java` sources. Two
flows, both writing back into `wattos/NRG/WEB-INF/classes/` so a Tomcat
restart picks up the new bytecode:

**Ant (recommended for clean rebuilds of the servlet):** the build file
lives one level down from the deployment root, in the Java project dir:

```bash
cd wattos                        # the inner wattos/ — sibling to docker-compose.yml
ant -f buildEclipse.xml          # default install-servlet — compile +
                                 # copy classes into NRG/WEB-INF/classes/
cd ..                            # back to deployment root
docker compose restart wattos-tomcat
```

`install-servlet` compiles the servlet closure
(`Wattos/{Servlet,Episode_II,Database,Common,Utils,Star,Soup}/`) against
the runtime jars already in `NRG/WEB-INF/lib/` and the `servlet-api.jar`
inside the running Tomcat container (auto-cached at
`/tmp/wattos-servlet-api.jar`). No internet access needed for this
target. Other useful targets:

  * `ant compile-servlet` — just `build/Wattos/`, no copy step
  * `ant jar-servlet` — package `lib/Wattos.jar` from the closure
  * `ant clean-servlet` — wipe `build/`

**`scripts/rebuild.sh` (faster for one-file edits):** run from the
deployment root,

```bash
./scripts/rebuild.sh wattos/src/Wattos/Servlet/MRGridServlet.java
./scripts/rebuild.sh wattos/src/Wattos/Episode_II/SQL_*.java
docker compose restart wattos-tomcat
```

Same classpath as `ant install-servlet`, but compiles only the named
files instead of the whole closure. Skips the rest, which is faster for
single-source iteration.

**Full-tree build (CLI + servlet, including the Swing GUI bits):** from
the inner `wattos/` (same as Ant above),

```bash
ant -f buildEclipse.xml jar      # auto-runs fetch-deps if lib/ is empty
ant -f buildEclipse.xml test     # runs the JUnit suite
```

`fetch-deps` populates `wattos/lib/` by mirroring `NRG/WEB-INF/lib/*.jar`
and pulling the build-time-only deps (itext, jfreechart, swing-layout,
junit, hamcrest, …) from Maven Central; the downloads are gitignored.
Currently 24/33 tests pass — the 9 failures are pre-existing data /
network rot, not infrastructure.

Both jar and class files under `NRG/WEB-INF/` are tracked in git, so any
rebuild shows up as an ordinary diff.

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
