package cc.ioctl.tmoe.util;

import android.content.pm.ApplicationInfo;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Minimal DEX class-table scanner used to survive Telegram R8 class renaming.
 *
 * It intentionally parses only string/type/class tables. No dex method code is
 * executed while discovering host classes.
 */
public final class HostDexClassScanner {

    private HostDexClassScanner() {}

    public static List<String> directSubclassesOf(Class<?> superClass) {
        List<String> out = new ArrayList<>();
        if (superClass == null || HostInfo.getApplication() == null) {
            return out;
        }
        String descriptor = "L" + superClass.getName().replace('.', '/') + ";";
        try {
            ApplicationInfo ai = HostInfo.getApplication().getApplicationInfo();
            scanApkForSubclasses(ai.sourceDir, descriptor, out);
            if (ai.splitSourceDirs != null) {
                for (String path : ai.splitSourceDirs) {
                    scanApkForSubclasses(path, descriptor, out);
                }
            }
        } catch (Throwable e) {
            Utils.loge(e);
        }
        return out;
    }

    /**
     * Returns host class names whose Java name starts with {@code packagePrefix}.
     *
     * LSPatch rewrites ApplicationInfo.sourceDir to its extracted origin.apk
     * before TMoe is initialized, so this scans Telegram itself rather than the
     * tiny LSPatch stub APK.
     */
    public static List<String> allClassesWithPrefix(String packagePrefix) {
        List<String> out = new ArrayList<>();
        if (packagePrefix == null || HostInfo.getApplication() == null) {
            return out;
        }
        String descriptorPrefix = "L" + packagePrefix.replace('.', '/');
        try {
            ApplicationInfo ai = HostInfo.getApplication().getApplicationInfo();
            scanApkForClasses(ai.sourceDir, descriptorPrefix, out);
            if (ai.splitSourceDirs != null) {
                for (String path : ai.splitSourceDirs) {
                    scanApkForClasses(path, descriptorPrefix, out);
                }
            }
        } catch (Throwable e) {
            Utils.loge(e);
        }
        return out;
    }

    private static void scanApkForSubclasses(String path, String superDescriptor, List<String> out) {
        if (path == null) return;
        try (ZipFile zip = new ZipFile(path)) {
            Enumeration<? extends ZipEntry> en = zip.entries();
            while (en.hasMoreElements()) {
                ZipEntry entry = en.nextElement();
                String name = entry.getName();
                if (!name.matches("classes\\d*\\.dex")) continue;
                try (InputStream in = zip.getInputStream(entry)) {
                    out.addAll(subclassesOf(readAll(in), superDescriptor));
                }
            }
        } catch (Throwable e) {
            Utils.loge(e);
        }
    }

    private static void scanApkForClasses(String path, String descriptorPrefix, List<String> out) {
        if (path == null) return;
        try (ZipFile zip = new ZipFile(path)) {
            Enumeration<? extends ZipEntry> en = zip.entries();
            while (en.hasMoreElements()) {
                ZipEntry entry = en.nextElement();
                String name = entry.getName();
                if (!name.matches("classes\\d*\\.dex")) continue;
                try (InputStream in = zip.getInputStream(entry)) {
                    out.addAll(classNamesWithPrefix(readAll(in), descriptorPrefix));
                }
            }
        } catch (Throwable e) {
            Utils.loge(e);
        }
    }

    static List<String> subclassesOf(byte[] dex, String superDescriptor) {
        List<String> out = new ArrayList<>();
        DexTables tables = parseTables(dex);
        if (tables == null) return out;
        ByteBuffer b = tables.buffer;

        int superType = -1;
        for (int i = 0; i < tables.typeIdsSize; i++) {
            int stringIdx = b.getInt(tables.typeIdsOff + 4 * i);
            if (superDescriptor.equals(readString(b, tables.stringIdsOff, stringIdx))) {
                superType = i;
                break;
            }
        }
        if (superType < 0) return out;

        for (int i = 0; i < tables.classDefsSize; i++) {
            int def = tables.classDefsOff + 32 * i;
            if (b.getInt(def + 8) != superType) continue;
            int classType = b.getInt(def);
            String desc = readTypeDescriptor(b, tables, classType);
            addDescriptor(out, desc);
        }
        return out;
    }

    static List<String> classNamesWithPrefix(byte[] dex, String descriptorPrefix) {
        List<String> out = new ArrayList<>();
        DexTables tables = parseTables(dex);
        if (tables == null) return out;
        ByteBuffer b = tables.buffer;
        for (int i = 0; i < tables.classDefsSize; i++) {
            int def = tables.classDefsOff + 32 * i;
            int classType = b.getInt(def);
            String desc = readTypeDescriptor(b, tables, classType);
            if (desc != null && desc.startsWith(descriptorPrefix)) {
                addDescriptor(out, desc);
            }
        }
        return out;
    }

    private static DexTables parseTables(byte[] dex) {
        if (dex == null || dex.length < 112) return null;
        ByteBuffer b = ByteBuffer.wrap(dex).order(ByteOrder.LITTLE_ENDIAN);
        if (b.getInt(0) != 0x0A786564) return null; // "dex\n"
        return new DexTables(
                b,
                b.getInt(56), b.getInt(60),
                b.getInt(64), b.getInt(68),
                b.getInt(96), b.getInt(100)
        );
    }

    private static String readTypeDescriptor(ByteBuffer b, DexTables tables, int typeIdx) {
        if (typeIdx < 0 || typeIdx >= tables.typeIdsSize) return null;
        int stringIdx = b.getInt(tables.typeIdsOff + 4 * typeIdx);
        return readString(b, tables.stringIdsOff, stringIdx);
    }

    private static void addDescriptor(List<String> out, String desc) {
        if (desc != null && desc.length() > 2 && desc.charAt(0) == 'L' && desc.endsWith(";")) {
            out.add(desc.substring(1, desc.length() - 1).replace('/', '.'));
        }
    }

    private static String readString(ByteBuffer b, int stringIdsOff, int idx) {
        int p = b.getInt(stringIdsOff + 4 * idx);
        int v;
        do {
            v = b.get(p++) & 0xff;
        } while ((v & 0x80) != 0);
        int start = p;
        while (b.get(p) != 0) p++;
        byte[] raw = new byte[p - start];
        for (int i = 0; i < raw.length; i++) raw[i] = b.get(start + i);
        return new String(raw, StandardCharsets.UTF_8);
    }

    private static byte[] readAll(InputStream in) throws java.io.IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(1 << 20);
        byte[] buf = new byte[1 << 16];
        int n;
        while ((n = in.read(buf)) > 0) {
            bos.write(buf, 0, n);
        }
        return bos.toByteArray();
    }

    private static final class DexTables {
        final ByteBuffer buffer;
        final int stringIdsSize;
        final int stringIdsOff;
        final int typeIdsSize;
        final int typeIdsOff;
        final int classDefsSize;
        final int classDefsOff;

        DexTables(ByteBuffer buffer, int stringIdsSize, int stringIdsOff,
                  int typeIdsSize, int typeIdsOff, int classDefsSize, int classDefsOff) {
            this.buffer = buffer;
            this.stringIdsSize = stringIdsSize;
            this.stringIdsOff = stringIdsOff;
            this.typeIdsSize = typeIdsSize;
            this.typeIdsOff = typeIdsOff;
            this.classDefsSize = classDefsSize;
            this.classDefsOff = classDefsOff;
        }
    }
}
