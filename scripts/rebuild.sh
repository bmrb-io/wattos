#!/bin/bash
# Incremental rebuild of Wattos source files into the deployed exploded
# webapp at wattos/NRG/. Avoids the full Ant build (which currently can't
# run because lib/ is missing several historical compile-time jars).
#
# Compiles the named .java files (or all .java files matching a pattern)
# against:
#   - existing compiled classes in wattos/NRG/WEB-INF/classes/
#   - bundled jars in wattos/NRG/WEB-INF/lib/
#   - servlet-api.jar pulled from the running wattos-tomcat container
# and writes the .class output back into wattos/NRG/WEB-INF/classes/.
#
# Usage:
#   ./scripts/rebuild.sh wattos/src/Wattos/Servlet/MRGridServlet.java
#   ./scripts/rebuild.sh wattos/src/Wattos/Servlet/*.java wattos/src/Wattos/Episode_II/SQL_*.java
#
# After a successful build, restart Tomcat:
#   docker compose restart wattos-tomcat
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$REPO_ROOT"

if [ $# -lt 1 ]; then
    echo "usage: $0 <source.java> [more.java ...]" >&2
    exit 64
fi

CLASSES_DIR="wattos/NRG/WEB-INF/classes"
LIB_DIR="wattos/NRG/WEB-INF/lib"
SERVLET_API_CACHE="${SERVLET_API_JAR:-/tmp/wattos-servlet-api.jar}"

# Pull servlet-api.jar from the running container if we don't have it cached.
if [ ! -f "$SERVLET_API_CACHE" ]; then
    docker cp wattos-tomcat:/usr/local/tomcat/lib/servlet-api.jar "$SERVLET_API_CACHE"
fi

# Prefer JDK 11 if available — the deployed JRE is 11 and some
# bundled jars (caffeine) are Java 11-targeted, so javac 8 can't read
# them. We still emit 1.8 bytecode below to match the rest of the
# tree.
JAVAC="${JAVAC:-}"
if [ -z "$JAVAC" ]; then
    if [ -x /usr/lib/jvm/java-11-openjdk-amd64/bin/javac ]; then
        JAVAC=/usr/lib/jvm/java-11-openjdk-amd64/bin/javac
    elif [ -x /usr/lib/jvm/java-8-openjdk-amd64/bin/javac ]; then
        JAVAC=/usr/lib/jvm/java-8-openjdk-amd64/bin/javac
    else
        JAVAC=javac
    fi
fi

CP="$CLASSES_DIR:$SERVLET_API_CACHE"
for jar in "$LIB_DIR"/*.jar; do
    CP="$CP:$jar"
done

echo "javac: $JAVAC"
echo "files: $*"

"$JAVAC" -source 1.8 -target 1.8 -d "$CLASSES_DIR" -cp "$CP" -Xlint:-options "$@"
echo "ok — compiled into $CLASSES_DIR"
