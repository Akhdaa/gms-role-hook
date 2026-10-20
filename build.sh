#!/usr/bin/env bash
# Build gms-role-hook.apk without Gradle: javac -> d8 -> aapt2 -> zipalign -> apksigner.
set -euo pipefail
cd "$(dirname "$0")"

SDK="${ANDROID_HOME:-$HOME/AppData/Local/Android/Sdk}"
BT="$SDK/build-tools/${BT_VER:-36.1.0}"
JAR=$(cygpath -m "$SDK/platforms/android-${API_LEVEL:-36}/android.jar")

rm -rf build && mkdir -p build/stubs build/classes build/dex

echo "== javac stubs (compile-only, not packaged)"
javac --release 8 -nowarn -cp "$JAR" -d build/stubs $(find _stubs -name '*.java')

echo "== javac module"
javac --release 8 -nowarn -cp "$JAR;build/stubs" -d build/classes $(find src -name '*.java')

echo "== d8"
"$BT/d8.bat" --release --min-api 26 --lib "$JAR" --output build/dex $(find build/classes -name '*.class')

echo "== aapt2"
"$BT/aapt2.exe" compile --dir res -o build/res.zip
"$BT/aapt2.exe" link -o build/base.apk -I "$JAR" --manifest AndroidManifest.xml -A assets build/res.zip

echo "== add classes.dex"
python -I - <<'EOF'
import zipfile
with zipfile.ZipFile('build/base.apk', 'a', zipfile.ZIP_DEFLATED) as z:
    z.write('build/dex/classes.dex', 'classes.dex')
EOF

echo "== zipalign + sign"
"$BT/zipalign.exe" -f -p 4 build/base.apk build/aligned.apk
KEYTOOL="$(command -v keytool || echo "${JAVA_HOME:-/c/Program Files/Java/jdk-25.0.2}/bin/keytool.exe")"
[ -f debug.jks ] || "$KEYTOOL" -genkeypair -keystore debug.jks -storepass android -keypass android \
    -alias key -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=xxru" >/dev/null 2>&1
"$BT/apksigner.bat" sign --ks debug.jks --ks-pass pass:android --key-pass pass:android \
    --out gms-role-hook.apk build/aligned.apk
"$BT/apksigner.bat" verify gms-role-hook.apk && echo "BUILD_OK gms-role-hook.apk"
