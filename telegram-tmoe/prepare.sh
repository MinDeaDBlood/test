#!/usr/bin/env bash
set -euo pipefail

ROOT="$(pwd)"
TG="$ROOT/telegram"
TM="$ROOT/tmoe"

# Add Pine Xposed compatibility and TMoe sources/resources into Telegram's main module.
python3 - <<'PY'
from pathlib import Path
p = Path("telegram/TMessagesProj/build.gradle")
s = p.read_text()
plugin_needle = "apply plugin: 'com.android.library'\n"
if plugin_needle not in s:
    raise SystemExit("android library plugin marker not found")
if "apply plugin: 'org.jetbrains.kotlin.android'" not in s:
    s = s.replace(plugin_needle, plugin_needle + "apply plugin: 'org.jetbrains.kotlin.android'\n", 1)

repo_needle = """repositories {
    mavenCentral()
    google()
}"""
repo_repl = """repositories {
    mavenCentral()
    google()
    maven { url 'https://jitpack.io' }
}"""
if repo_needle not in s:
    raise SystemExit("repositories block not found")
s = s.replace(repo_needle, repo_repl, 1)

needle = "dependencies {\n"
insert = """dependencies {
    implementation 'top.canyie.pine:core:0.3.0'
    implementation 'top.canyie.pine:xposed:0.2.0'
    implementation 'com.tencent:mmkv-static:2.0.0'
    implementation 'org.lsposed.hiddenapibypass:hiddenapibypass:4.3'
    implementation 'org.smali:dexlib2:2.5.2'
    implementation 'com.google.guava:guava:33.3.1-android'
    implementation 'com.google.android.material:material:1.12.0'
    implementation 'androidx.appcompat:appcompat:1.7.0'
    implementation 'com.github.kyuubiran:EzXHelper:1.0.3'
    implementation 'com.google.code.findbugs:jsr305:3.0.2'
"""
if needle not in s:
    raise SystemExit("dependencies block not found")
s = s.replace(needle, insert, 1)
p.write_text(s)
PY

# Copy TMoe Java/Kotlin and resources directly into Telegram source tree.
mkdir -p "$TG/TMessagesProj/src/main/java/cc/ioctl"
rm -rf "$TG/TMessagesProj/src/main/java/cc/ioctl/tmoe"
cp -a "$TM/app/src/main/java/cc/ioctl/tmoe" "$TG/TMessagesProj/src/main/java/cc/ioctl/"

# Copy TMoe resources, excluding module manifest/assets/xposed metadata.
cp -a "$TM/app/src/main/res/." "$TG/TMessagesProj/src/main/res/"

# Embedded module shares Telegram's resource namespace.
grep -RIl "cc.ioctl.tmoe.R" "$TG/TMessagesProj/src/main/java/cc/ioctl/tmoe" | while read -r file; do
  sed -i 's/cc\.ioctl\.tmoe\.R/org.telegram.messenger.R/g' "$file"
done

cat > "$TG/TMessagesProj/src/main/java/cc/ioctl/tmoe/BuildConfig.java" <<'JAVA'
package cc.ioctl.tmoe;

public final class BuildConfig {
    public static final boolean DEBUG = false;
    public static final String VERSION_NAME = "embedded-13.2.2";
    public static final int VERSION_CODE = 7303;
    public static final String BUILD_UUID = "telegram-13.2.2-tmoe";
    public static final long BUILD_TIMESTAMP = 0L;
    private BuildConfig() {}
}
JAVA

# Generate the KSP-produced hook list statically from annotations.
python3 - <<'PY'
from pathlib import Path
root = Path("telegram/TMessagesProj/src/main/java/cc/ioctl/tmoe/hook/func")
entries = []
for p in sorted(root.glob("*")):
    if p.suffix not in {".kt", ".java"}:
        continue
    txt = p.read_text(errors="ignore")
    if "@FunctionHookEntry" not in txt:
        continue
    stem = p.stem
    if p.suffix == ".java":
        entries.append(f"        {stem}.INSTANCE")
    else:
        entries.append(f"        {stem}.INSTANCE")
out = Path("telegram/TMessagesProj/src/main/java/cc/ioctl/tmoe/gen/AnnotatedFunctionHookEntryList.java")
out.parent.mkdir(parents=True, exist_ok=True)
names = []
for p in sorted(root.glob("*")):
    if p.suffix not in {".kt", ".java"}:
        continue
    txt = p.read_text(errors="ignore")
    if "@FunctionHookEntry" in txt:
        names.append(p.stem)
imports = "\n".join(f"import cc.ioctl.tmoe.hook.func.{name};" for name in names)
body = ",\n".join(f"        {name}.INSTANCE" for name in names)
out.write_text(f"""package cc.ioctl.tmoe.gen;

import cc.ioctl.tmoe.hook.base.BaseDynamicHook;
{imports}

public final class AnnotatedFunctionHookEntryList {{
    private AnnotatedFunctionHookEntryList() {{}}
    public static BaseDynamicHook[] getAnnotatedFunctionHookEntryList() {{
        return new BaseDynamicHook[] {{
{body}
        }};
    }}
}}
""")
PY

# Remove Xposed-module-only resource injection/stub machinery: resources are already merged in this APK.
python3 - <<'PY'
from pathlib import Path
p = Path("telegram/TMessagesProj/src/main/java/cc/ioctl/tmoe/lifecycle/Parasitics.java")
s = p.read_text()
start = s.index("    public static void injectModuleResources(Resources res) {")
end = s.index("\n    @RequiresApi(30)", start)
replacement = """    public static void injectModuleResources(Resources res) {
        // Embedded build: TMoe resources are packaged directly into Telegram.
    }
"""
s = s[:start] + replacement + s[end:]
p.write_text(s)
PY

# Make native loader usable inside the single APK. The TMoe native library is not needed for functional hooks.
cat > "$TG/TMessagesProj/src/main/java/cc/ioctl/tmoe/startup/NativeLoader.java" <<'JAVA'
package cc.ioctl.tmoe.startup;

import android.content.Context;
import com.tencent.mmkv.MMKV;
import java.io.File;

public final class NativeLoader {
    private static boolean loaded;

    private NativeLoader() {}

    public static synchronized void loadAllSharedLibraries(Context ctx) {
        if (loaded) return;
        File mmkvDir = new File(ctx.getFilesDir(), "tmoe_mmkv");
        if (!mmkvDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            mmkvDir.mkdirs();
        }
        MMKV.initialize(mmkvDir.getAbsolutePath());
        MMKV.mmkvWithID("global_config", MMKV.SINGLE_PROCESS_MODE);
        MMKV.mmkvWithID("global_cache", MMKV.SINGLE_PROCESS_MODE);
        loaded = true;
    }

    public static boolean isNativeLibraryLoaded() {
        return loaded;
    }
}
JAVA

# Stub native-only tgnet log controller controls so Settings remains functional without libtmoe.
python3 - <<'PY'
from pathlib import Path
p = Path("telegram/TMessagesProj/src/main/java/cc/ioctl/tmoe/hook/func/TgnetLogController.java")
s = p.read_text()
s = s.replace("public static native int getCurrentTgnetLogStatus();", "public static int getCurrentTgnetLogStatus() { return -1; }")
s = s.replace("public static native int setCurrentTgnetLogStatus(int status);", "public static int setCurrentTgnetLogStatus(int status) { return -1; }")
p.write_text(s)
PY

# Add an embedded startup helper using Pine's in-process Xposed bridge.
mkdir -p "$TG/TMessagesProj/src/main/java/cc/ioctl/tmoe/embedded"
cat > "$TG/TMessagesProj/src/main/java/cc/ioctl/tmoe/embedded/EmbeddedEntry.java" <<'JAVA'
package cc.ioctl.tmoe.embedded;

import android.app.Application;

import cc.ioctl.tmoe.startup.NativeLoader;
import cc.ioctl.tmoe.startup.StartupRoutine;
import top.canyie.pine.PineConfig;

public final class EmbeddedEntry {
    private static boolean initialized;

    private EmbeddedEntry() {}

    public static synchronized void preInit(Application app) {
        if (initialized) return;
        PineConfig.debug = false;
        PineConfig.debuggable = false;
        NativeLoader.loadAllSharedLibraries(app);
        StartupRoutine.execPreStartupInit(app, null, false);
    }

    public static synchronized void postInit() {
        if (initialized) return;
        StartupRoutine.execPostStartupInit();
        initialized = true;
    }
}
JAVA

# Wire TMoe into ApplicationLoader.onCreate.
python3 - <<'PY'
from pathlib import Path
p = Path("telegram/TMessagesProj/src/main/java/org/telegram/messenger/ApplicationLoader.java")
s = p.read_text()
needle = "        super.onCreate();\n"
replace = """        super.onCreate();

        cc.ioctl.tmoe.embedded.EmbeddedEntry.preInit(this);
"""
if needle not in s:
    raise SystemExit("super.onCreate marker not found")
s = s.replace(needle, replace, 1)

needle2 = "        ProxyRotationController.init();\n"
replace2 = """        ProxyRotationController.init();

        cc.ioctl.tmoe.embedded.EmbeddedEntry.postInit();
"""
if needle2 not in s:
    raise SystemExit("post marker not found")
s = s.replace(needle2, replace2, 1)
p.write_text(s)
PY

# Limit the final APK to the requested ABI.
python3 - <<'PY'
from pathlib import Path
import os
abi = os.environ["TARGET_ABI"]
p = Path("telegram/TMessagesProj_App/build.gradle")
s = p.read_text()
s = s.replace('abiFilters "armeabi-v7a", "arm64-v8a", "x86", "x86_64"', f'abiFilters "{abi}"')
p.write_text(s)
PY

# Keep TMoe classes from R8 because the module uses reflection heavily.
cat >> "$TG/TMessagesProj/proguard-rules.pro" <<'PRO'
-keep class cc.ioctl.tmoe.** { *; }
-keep class de.robv.android.xposed.** { *; }
-keep class top.canyie.pine.** { *; }
PRO
