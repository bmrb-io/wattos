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

# Prefer JDK 21 (Tomcat 11 minimum is Java 17, and the bundled
# jakarta.servlet 6.1 + caffeine jars are Java 17-targeted). Falls
# back through 17 / 11 / 8 / PATH for environments that don't have
# 21 yet.
JAVAC="${JAVAC:-}"
if [ -z "$JAVAC" ]; then
    for candidate in \
        /usr/lib/jvm/java-21-openjdk-amd64/bin/javac \
        /usr/lib/jvm/java-17-openjdk-amd64/bin/javac \
        /usr/lib/jvm/java-11-openjdk-amd64/bin/javac \
        /usr/lib/jvm/java-8-openjdk-amd64/bin/javac
    do
        if [ -x "$candidate" ]; then JAVAC="$candidate"; break; fi
    done
    JAVAC="${JAVAC:-javac}"
fi

CP="$CLASSES_DIR:$SERVLET_API_CACHE"
for jar in "$LIB_DIR"/*.jar; do
    CP="$CP:$jar"
done

echo "javac: $JAVAC"
echo "files: $*"

# -source/-target 17 matches Tomcat 11 / JDK 21. Existing pre-built
# 1.5/1.8 classes in WEB-INF/classes/ still load fine alongside
# 17-targeted output under JRE 21.
"$JAVAC" -source 17 -target 17 -d "$CLASSES_DIR" -cp "$CP" -Xlint:-options "$@"
echo "ok — compiled into $CLASSES_DIR"
