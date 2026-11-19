#!/bin/sh
# Starts Pokemon DS Map Studio on macOS (double-click in Finder, or run in a terminal).
# Needs Java 11 or newer. lib/jogamp-fat-2.6.0.jar comes first on the classpath: its 3D library fixes
# the crash on recent macOS versions. For .nsbmd export, put g3dcvtr.exe into converter/ and install Wine.
cd "$(dirname "$0")" || exit 1
APP_DIR=$(pwd)

# /usr/bin/java always exists on macOS, but only works when a Java is installed.
if ! java -version >/dev/null 2>&1; then
    osascript -e 'display alert "Pokemon DS Map Studio needs Java" message "Install Java 11 or newer (e.g. from adoptium.net), then start it again."' >/dev/null 2>&1
    echo "Java not found: install Java 11 or newer (e.g. from adoptium.net)." >&2
    exit 1
fi
JAVA=java
JH=$(/usr/libexec/java_home 2>/dev/null) && [ -x "$JH/bin/java" ] && JAVA="$JH/bin/java"

for p in "/Applications/Wine Stable.app/Contents/Resources/wine/bin" "/Applications/Wine Devel.app/Contents/Resources/wine/bin" "/Applications/Wine Staging.app/Contents/Resources/wine/bin" /opt/homebrew/bin /usr/local/bin; do
    [ -d "$p" ] && PATH="$PATH:$p"
done
export PATH

# --enable-native-access=ALL-UNNAMED exists since Java 22; it silences JOGL's native access warnings.
MAJOR=$("$JAVA" -version 2>&1 | sed -n 's/.*version "\([0-9]*\).*/\1/p' | head -1)
OPTS=""
[ "${MAJOR:-0}" -ge 22 ] && OPTS="--enable-native-access=ALL-UNNAMED"

exec "$JAVA" $OPTS -cp "$APP_DIR/lib/jogamp-fat-2.6.0.jar:$APP_DIR/Pokemon DS Map Studio.jar" editor.MainFrame "$@"
