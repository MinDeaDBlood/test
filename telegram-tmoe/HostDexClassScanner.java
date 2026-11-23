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
 * It reads only class definitions and returns direct subclasses of a known host class.
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
            scanApk(ai.sourceDir, descriptor, out);
            if (ai.splitSourceDirs != null) {
                for (String path : ai.splitSourceDirs) {
                    scanApk(path, descriptor, out);
                }
            }
        } catch (Throwable e) {
            Utils.loge(e);
        }
        return out;
    }

    private static void scanApk(String path, String superDescriptor, List<String> out) {
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

    static List<String> subclassesOf(byte[] dex, String superDescriptor) {
        List<String> out = new ArrayList<>();
        if (dex == null || dex.length < 112) return out;
        ByteBuffer b = ByteBuffer.wrap(dex).order(ByteOrder.LITTLE_ENDIAN);
        if (b.getInt(0) != 0x0A786564) return out; // "dex\n"

        int stringIdsSize = b.getInt(56);
        int stringIdsOff = b.getInt(60);
        int typeIdsSize = b.getInt(64);
        int typeIdsOff = b.getInt(68);
        int classDefsSize = b.getInt(96);
        int classDefsOff = b.getInt(100);

        int superType = -1;
        for (int i = 0; i < typeIdsSize; i++) {
            int stringIdx = b.getInt(typeIdsOff + 4 * i);
            if (superDescriptor.equals(readString(b, stringIdsOff, stringIdx))) {
                superType = i;
                break;
            }
        }
        if (superType < 0) return out;

        for (int i = 0; i < classDefsSize; i++) {
            int def = classDefsOff + 32 * i;
            if (b.getInt(def + 8) != superType) continue;
            int classType = b.getInt(def);
            int stringIdx = b.getInt(typeIdsOff + 4 * classType);
            String desc = readString(b, stringIdsOff, stringIdx);
            if (desc.length() > 2 && desc.charAt(0) == 'L' && desc.endsWith(";")) {
                out.add(desc.substring(1, desc.length() - 1).replace('/', '.'));
            }
        }
        return out;
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
}
