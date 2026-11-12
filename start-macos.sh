#!/bin/sh
# Starts this fork of Pokemon DS Map Studio on macOS. The fork is the only editor: if the sources are
# newer than the built jar, the jar is rebuilt first (gradle needs JDK 21 or older), then the editor starts.
#
# Needs libs-macos/jogamp-fat-2.6.0.jar (its 3D library fixes the macOS 26+ crash and must come first on
# the classpath, before the older copy bundled in the app jar). Wine is put on PATH for g3dcvtr.exe
# (converter/, not in git). The editor runs in .run/ so export temp files stay out of the source tree.
cd "$(dirname "$0")" || exit 1
ROOT=$(pwd)
JAR="$ROOT/build/libs/Pokemon DS Map Studio-2.4.jar"
JOGAMP="$ROOT/libs-macos/jogamp-fat-2.6.0.jar"

if [ ! -f "$JOGAMP" ]; then
    echo "Missing $JOGAMP (jogamp-fat.jar 2.6.0 from jogamp.org, renamed, put into libs-macos/)" >&2
    exit 1
fi

if [ ! -f "$JAR" ] || [ -n "$(find src/main build.gradle -type f -newer "$JAR" 2>/dev/null | head -1)" ]; then
    JDK=""
    for c in "$MAPSTUDIO_JDK21" "$ROOT"/.jdk21/jdk-21*/Contents/Home "$(/usr/libexec/java_home -v 21 2>/dev/null)"; do
        [ -n "$c" ] && [ -x "$c/bin/java" ] && JDK="$c" && break
    done
    if [ -z "$JDK" ]; then
        echo "Sources changed but no JDK 21 found (gradle fails on newer JDKs). Put one in .jdk21/ or set MAPSTUDIO_JDK21." >&2
        exit 1
    fi
    echo "Building the editor (sources changed)..." >&2
    JAVA_HOME="$JDK" ./gradlew jar -q || exit 1
fi

for p in "/Applications/Wine Stable.app/Contents/Resources/wine/bin" "/Applications/Wine Devel.app/Contents/Resources/wine/bin" "/Applications/Wine Staging.app/Contents/Resources/wine/bin"; do
    [ -d "$p" ] && PATH="$p:$PATH"
done
export PATH
mkdir -p "$ROOT/.run" && cd "$ROOT/.run" || exit 1
exec java --enable-native-access=ALL-UNNAMED -cp "$JOGAMP:$JAR" editor.MainFrame "$@"
