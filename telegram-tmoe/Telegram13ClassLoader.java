package cc.ioctl.tmoe.util;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Compatibility ClassLoader for Telegram's heavily R8-obfuscated telegram.org build.
 *
 * TMoe historically resolves Telegram implementation classes by their source names.
 * Telegram 13.3.1 (org.telegram.messenger.web) obfuscates most of those classes.
 * This loader preserves the old logical names for TMoe while returning the real
 * classes from the host ClassLoader.
 *
 * The map below is verified against the exact Telegram 13.3.1 APK used by this
 * integration. Callers still get the real host Class objects; no Telegram classes
 * are copied into the module.
 */
public final class Telegram13ClassLoader extends ClassLoader {

    private static final Map<String, String> ALIASES;
    private static volatile Telegram13ClassLoader sInstance;
    private final ClassLoader host;

    static {
        HashMap<String, String> m = new HashMap<>();

        // messenger core
        m.put("org.telegram.messenger.MessageObject", "org.telegram.messenger.fj");
        m.put("org.telegram.messenger.MessagesController", "org.telegram.messenger.pp");
        m.put("org.telegram.messenger.MessagesStorage", "org.telegram.messenger.as");
        m.put("org.telegram.messenger.NotificationsController", "org.telegram.messenger.ft");
        m.put("org.telegram.messenger.NotificationCenter", "org.telegram.messenger.ss");
        m.put("org.telegram.messenger.UserConfig", "org.telegram.messenger.a20");
        m.put("org.telegram.messenger.SharedConfig", "org.telegram.messenger.rz");
        m.put("org.telegram.messenger.LocaleController", "org.telegram.messenger.dd");
        m.put("org.telegram.messenger.MediaDataController", "org.telegram.messenger.ri");
        m.put("org.telegram.messenger.DownloadController", "org.telegram.messenger.v5");
        m.put("org.telegram.messenger.FileLoadOperation", "org.telegram.messenger.d7");
        m.put("org.telegram.messenger.FileUploadOperation", "org.telegram.messenger.o8");

        // UI / fragments
        m.put("org.telegram.ui.ActionBar.BaseFragment", "org.telegram.ui.ActionBar.j2");
        m.put("org.telegram.ui.ActionBar.Theme", "org.telegram.ui.ActionBar.z4");
        m.put("org.telegram.ui.ChatActivity", "org.telegram.ui.on");
        m.put("org.telegram.ui.DialogsActivity", "org.telegram.ui.qx");
        m.put("org.telegram.ui.SettingsActivity", "org.telegram.ui.y41");
        m.put("org.telegram.ui.Cells.ChatMessageCell", "org.telegram.ui.Cells.q1");

        // Stories moved into an R8 package in this release.
        m.put("org.telegram.ui.Stories.StoriesController", "bh.b9");
        m.put("org.telegram.messenger.StoriesController", "bh.b9");
        m.put("org.telegram.ui.stories.StoriesController", "bh.b9");

        ALIASES = Collections.unmodifiableMap(m);
    }

    private Telegram13ClassLoader(@NonNull ClassLoader host) {
        super(host);
        this.host = host;
    }

    @NonNull
    public static Telegram13ClassLoader getOrCreate(@NonNull ClassLoader host) {
        Telegram13ClassLoader current = sInstance;
        if (current == null || current.host != host) {
            synchronized (Telegram13ClassLoader.class) {
                current = sInstance;
                if (current == null || current.host != host) {
                    current = new Telegram13ClassLoader(host);
                    sInstance = current;
                }
            }
        }
        return current;
    }

    public static String resolveAlias(String requestedName) {
        String alias = ALIASES.get(requestedName);
        return alias != null ? alias : requestedName;
    }

    public static Map<String, String> getAliases() {
        return ALIASES;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        String actual = resolveAlias(name);
        try {
            return host.loadClass(actual);
        } catch (ClassNotFoundException first) {
            // Fall back to the original name. This keeps the wrapper safe on forks
            // where readable source names are retained.
            if (!actual.equals(name)) {
                return host.loadClass(name);
            }
            throw first;
        }
    }

    @Override
    public String toString() {
        return "Telegram13ClassLoader{" + host + "}";
    }
}
