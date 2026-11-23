package cc.ioctl.tmoe.compat;

import android.app.Application;
import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cc.ioctl.tmoe.util.HostDexClassScanner;
import cc.ioctl.tmoe.util.HostInfo;
import cc.ioctl.tmoe.util.Initiator;
import cc.ioctl.tmoe.util.Utils;

/**
 * Runtime source-name -> R8 resolver for the official Telegram 13.3.1
 * standalone APK. Semantic anchors are preferred; exact names are verified
 * fallbacks for the user's target APK.
 */
public final class Telegram13ClassResolver {

    private static final Map<String, Class<?>> CACHE = new ConcurrentHashMap<>();

    private static final Map<String, String> TG_1331 = Map.ofEntries(
            Map.entry("org.telegram.messenger.ApplicationLoader", "org.telegram.messenger.s0"),
            Map.entry("org.telegram.messenger.AccountInstance", "org.telegram.messenger.a"),
            Map.entry("org.telegram.messenger.BuildVars", "org.telegram.messenger.m2"),
            Map.entry("org.telegram.messenger.ChatObject", "org.telegram.messenger.p3"),
            Map.entry("org.telegram.messenger.DownloadController", "org.telegram.messenger.v5"),
            Map.entry("org.telegram.messenger.FileLoadOperation", "org.telegram.messenger.d7"),
            Map.entry("org.telegram.messenger.FileUploadOperation", "org.telegram.messenger.s8"),
            Map.entry("org.telegram.messenger.LocaleController", "org.telegram.messenger.dd"),
            Map.entry("org.telegram.messenger.MediaDataController", "org.telegram.messenger.ri"),
            Map.entry("org.telegram.messenger.MessageObject", "org.telegram.messenger.fj"),
            Map.entry("org.telegram.messenger.MessagesController", "org.telegram.messenger.pp"),
            Map.entry("org.telegram.messenger.MessagesStorage", "org.telegram.messenger.as"),
            Map.entry("org.telegram.messenger.SharedConfig", "org.telegram.messenger.rz"),
            Map.entry("org.telegram.messenger.UserConfig", "org.telegram.messenger.a20"),
            Map.entry("org.telegram.messenger.UserObject", "org.telegram.messenger.d20"),
            Map.entry("org.telegram.ui.ActionBar.BaseFragment", "org.telegram.ui.ActionBar.j2"),
            Map.entry("org.telegram.ui.ActionBar.ActionBar", "org.telegram.ui.ActionBar.k"),
            Map.entry("org.telegram.ui.ActionBar.ActionBarMenuItem", "org.telegram.ui.ActionBar.q0"),
            Map.entry("org.telegram.ui.ActionBar.ActionBarMenuSubItem", "org.telegram.ui.ActionBar.z0"),
            Map.entry("org.telegram.ui.ActionBar.Theme", "org.telegram.ui.ActionBar.r6"),
            Map.entry("org.telegram.ui.ActionBar.ThemeColors", "org.telegram.ui.ActionBar.z4"),
            Map.entry("org.telegram.ui.ChatActivity", "org.telegram.ui.on"),
            Map.entry("org.telegram.ui.ChatEditActivity", "org.telegram.ui.ho"),
            Map.entry("org.telegram.ui.ChatUsersActivity", "org.telegram.ui.gr"),
            Map.entry("org.telegram.ui.DialogsActivity", "org.telegram.ui.qx"),
            Map.entry("org.telegram.ui.TopicsFragment", "org.telegram.ui.ib1"),
            Map.entry("org.telegram.ui.ChatPullingDownDrawable", "org.telegram.ui.mp"),
            Map.entry("org.telegram.ui.DownloadProgressIcon", "org.telegram.ui.sx"),
            Map.entry("org.telegram.ui.Cells.ChatMessageCell", "org.telegram.ui.Cells.q1"),
            Map.entry("org.telegram.ui.Components.ChatGreetingsView", "org.telegram.ui.Components.io"),
            Map.entry("org.telegram.ui.Adapters.MentionsAdapter", "ef.l1"),
            Map.entry("org.telegram.ui.Stories.StoriesController", "bh.b9"),
            Map.entry("org.telegram.tgnet.TLRPC$TL_account_updateStatus", "org.telegram.tgnet.tl.TL_account$updateStatus")
    );

    private Telegram13ClassResolver() {}

    public static boolean isTelegram13Standalone() {
        return "org.telegram.messenger.web".equals(HostInfo.getPackageName());
    }

    public static Class<?> resolve(String sourceName) {
        if (sourceName == null || sourceName.isEmpty()) return null;
        Class<?> cached = CACHE.get(sourceName);
        if (cached != null) return cached;
        ClassLoader cl = Initiator.getHostClassLoader();
        if (cl == null) return null;
        try {
            Class<?> readable = tryClass(cl, sourceName);
            if (readable != null) return remember(sourceName, readable);

            Class<?> c = resolveSemantic(sourceName, cl);
            if (c == null) {
                String fallback = TG_1331.get(sourceName);
                if (fallback != null) c = tryClass(cl, fallback);
            }
            return c == null ? null : remember(sourceName, c);
        } catch (Throwable e) {
            Utils.loge(e);
            return null;
        }
    }

    public static Class<?> require(String sourceName) throws ClassNotFoundException {
        Class<?> c = resolve(sourceName);
        if (c == null) throw new ClassNotFoundException(sourceName);
        return c;
    }

    public static void warmUp() {
        String[] names = {
                "org.telegram.ui.ActionBar.BaseFragment",
                "org.telegram.ui.ChatActivity",
                "org.telegram.messenger.MessageObject",
                "org.telegram.messenger.MessagesController",
                "org.telegram.messenger.UserConfig",
                "org.telegram.messenger.DownloadController",
                "org.telegram.messenger.MediaDataController",
                "org.telegram.messenger.MessagesStorage",
                "org.telegram.ui.Stories.StoriesController"
        };
        for (String n : names) resolve(n);
    }

    private static Class<?> remember(String sourceName, Class<?> c) {
        CACHE.put(sourceName, c);
        Utils.logd("TMoe/TG13 class: " + sourceName + " -> " + c.getName());
        return c;
    }

    private static Class<?> resolveSemantic(String sourceName, ClassLoader cl) throws Exception {
        Class<?> base = getBaseFragment(cl);
        if ("org.telegram.ui.ActionBar.BaseFragment".equals(sourceName)) return base;

        if ("org.telegram.messenger.ApplicationLoader".equals(sourceName)) {
            Class<?> impl = tryClass(cl, "org.telegram.messenger.ApplicationLoaderImpl");
            if (impl != null) {
                for (Class<?> c = impl; c != null && c != Application.class; c = c.getSuperclass()) {
                    try {
                        Method m = c.getDeclaredMethod("onCreate");
                        if (!Modifier.isStatic(m.getModifiers()) && m.getReturnType() == void.class
                                && m.getParameterTypes().length == 0) return c;
                    } catch (NoSuchMethodException ignored) {}
                }
            }
        }

        if (base != null) {
            if ("org.telegram.ui.ActionBar.ActionBar".equals(sourceName)) return methodReturn(base, "getActionBar");
            if ("org.telegram.messenger.AccountInstance".equals(sourceName)) return methodReturn(base, "getAccountInstance");
            if ("org.telegram.messenger.DownloadController".equals(sourceName)) return methodReturn(base, "getDownloadController");
            if ("org.telegram.messenger.MediaDataController".equals(sourceName)) return methodReturn(base, "getMediaDataController");
            if ("org.telegram.messenger.MessagesController".equals(sourceName)) return methodReturn(base, "getMessagesController");
            if ("org.telegram.messenger.MessagesStorage".equals(sourceName)) return methodReturn(base, "getMessagesStorage");
            if ("org.telegram.messenger.UserConfig".equals(sourceName)) return methodReturn(base, "getUserConfig");
        }

        if ("org.telegram.ui.ActionBar.ActionBarMenuItem".equals(sourceName)) {
            for (String n : HostDexClassScanner.allClassesWithPrefix("org.telegram.ui.ActionBar.")) {
                Class<?> c = tryClass(cl, n);
                if (c == null || !FrameLayout.class.isAssignableFrom(c)) continue;
                for (Method m : c.getDeclaredMethods()) {
                    Class<?>[] p = m.getParameterTypes();
                    if (p.length == 3 && p[0] == int.class && p[1] == int.class
                            && CharSequence.class.isAssignableFrom(p[2])
                            && View.class.isAssignableFrom(m.getReturnType())) return c;
                }
            }
        }

        if ("org.telegram.ui.ActionBar.ActionBarMenuSubItem".equals(sourceName)) {
            Class<?> menu = resolve("org.telegram.ui.ActionBar.ActionBarMenuItem");
            if (menu != null) {
                for (Method m : menu.getDeclaredMethods()) {
                    Class<?>[] p = m.getParameterTypes();
                    if (p.length == 3 && p[0] == int.class && p[1] == int.class
                            && CharSequence.class.isAssignableFrom(p[2])
                            && View.class.isAssignableFrom(m.getReturnType())) return m.getReturnType();
                }
            }
        }

        if ("org.telegram.ui.Stories.StoriesController".equals(sourceName)) {
            Class<?> mc = resolve("org.telegram.messenger.MessagesController");
            if (mc != null) {
                for (Method m : mc.getDeclaredMethods()) {
                    if (m.getParameterTypes().length != 0) continue;
                    Class<?> r = m.getReturnType();
                    if (r != void.class && hasStoryReadShape(r)) return r;
                }
            }
        }

        if ("org.telegram.ui.ChatActivity".equals(sourceName) && base != null) {
            Class<?> enter = tryClass(cl, "org.telegram.ui.Components.ChatActivityEnterView");
            if (enter != null) {
                for (Method m : enter.getDeclaredMethods()) {
                    if (!"getParentFragment".equals(m.getName()) || m.getParameterTypes().length != 0) continue;
                    Class<?> rt = m.getReturnType();
                    if (rt != base && base.isAssignableFrom(rt)) return rt;
                }
            }
            Class<?> best = null;
            int bestScore = -1;
            for (String n : HostDexClassScanner.directSubclassesOf(base)) {
                Class<?> c = tryClass(cl, n);
                if (c == null) continue;
                int score = c.getDeclaredFields().length + c.getDeclaredMethods().length;
                if (score > bestScore) { best = c; bestScore = score; }
            }
            return best;
        }
        return null;
    }

    private static boolean hasStoryReadShape(Class<?> c) {
        if (c == null || c.isPrimitive()) return false;
        for (Method m : c.getDeclaredMethods()) {
            Class<?>[] p = m.getParameterTypes();
            if (m.getReturnType() == boolean.class && p.length == 3
                    && p[0].getName().contains("PeerStories")
                    && p[1].getName().contains("StoryItem") && p[2] == boolean.class) return true;
        }
        return false;
    }

    private static Class<?> getBaseFragment(ClassLoader cl) {
        Class<?> profile = tryClass(cl, "org.telegram.ui.ProfileActivity");
        if (profile == null) return null;
        Class<?> c = profile.getSuperclass();
        return c == Object.class ? null : c;
    }

    private static Class<?> methodReturn(Class<?> c, String name) {
        for (Class<?> x = c; x != null && x != Object.class; x = x.getSuperclass()) {
            for (Method m : x.getDeclaredMethods()) {
                if (name.equals(m.getName()) && m.getParameterTypes().length == 0) return m.getReturnType();
            }
        }
        return null;
    }

    private static Class<?> tryClass(ClassLoader cl, String name) {
        try { return Class.forName(name, false, cl); }
        catch (Throwable ignored) { return null; }
    }
}
