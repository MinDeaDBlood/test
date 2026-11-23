package cc.ioctl.tmoe.compat;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import cc.ioctl.tmoe.util.Initiator;
import cc.ioctl.tmoe.util.Utils;

/**
 * Structural host-class resolver for Telegram 13.x standalone builds.
 *
 * Telegram's standalone APK is minified with R8, so names such as BaseFragment,
 * ChatActivity, MessageObject and most controller classes are not stable.
 * Public bridge methods on a few anchor classes are still readable, however.
 * Resolve from those relationships instead of hard-coding short R8 names.
 */
public final class Telegram13ClassResolver {

    private static volatile Class<?> sBaseFragment;
    private static volatile Class<?> sChatActivity;
    private static volatile Class<?> sMessageObject;
    private static final Map<String, Class<?>> sBaseGetterTypes = new LinkedHashMap<>();

    private Telegram13ClassResolver() {}

    public static void warmUp() {
        try {
            baseFragment();
            chatActivity();
            messageObject();
            for (String getter : new String[]{
                    "getAccountInstance",
                    "getContactsController",
                    "getDownloadController",
                    "getFileLoader",
                    "getMediaDataController",
                    "getMessagesController",
                    "getMessagesStorage",
                    "getNotificationCenter",
                    "getNotificationsController",
                    "getSecretChatHelper",
                    "getSendMessagesHelper",
                    "getUserConfig"
            }) {
                fromBaseFragmentGetter(getter);
            }
            Utils.logd("TMoe: Telegram13ClassResolver " + diagnosticSummary());
        } catch (Throwable e) {
            Utils.loge(e);
        }
    }

    public static Class<?> baseFragment() {
        Class<?> cached = sBaseFragment;
        if (cached != null) return cached;

        Class<?> readable = Initiator.load("org.telegram.ui.ActionBar.BaseFragment");
        if (readable != null) {
            sBaseFragment = readable;
            return readable;
        }

        Class<?> profile = Initiator.load("org.telegram.ui.ProfileActivity");
        if (profile != null) {
            Class<?> parent = profile.getSuperclass();
            if (parent != null && parent != Object.class) {
                sBaseFragment = parent;
                return parent;
            }
        }
        return null;
    }

    public static Class<?> chatActivity() {
        Class<?> cached = sChatActivity;
        if (cached != null) return cached;

        Class<?> readable = Initiator.load("org.telegram.ui.ChatActivity");
        if (readable != null) {
            sChatActivity = readable;
            return readable;
        }

        Class<?> base = baseFragment();
        Class<?> enter = Initiator.load("org.telegram.ui.Components.ChatActivityEnterView");
        if (base == null || enter == null) return null;

        // Telegram 13.3.1 keeps two covariant bridge versions of
        // getParentFragment(): one returns BaseFragment, the other ChatActivity.
        for (Method method : enter.getDeclaredMethods()) {
            if (!"getParentFragment".equals(method.getName())
                    || method.getParameterTypes().length != 0) {
                continue;
            }
            Class<?> rt = method.getReturnType();
            if (rt != base && base.isAssignableFrom(rt)) {
                sChatActivity = rt;
                return rt;
            }
        }
        return null;
    }

    public static Class<?> messageObject() {
        Class<?> cached = sMessageObject;
        if (cached != null) return cached;

        Class<?> readable = Initiator.load("org.telegram.messenger.MessageObject");
        if (readable != null) {
            sMessageObject = readable;
            return readable;
        }

        Class<?> enter = Initiator.load("org.telegram.ui.Components.ChatActivityEnterView");
        if (enter == null) return null;
        Class<?> found = null;
        for (Method method : enter.getDeclaredMethods()) {
            String name = method.getName();
            if (!("getEditingMessageObject".equals(name)
                    || "getReplyingMessageObject".equals(name)
                    || "getThreadMessage".equals(name))) {
                continue;
            }
            if (method.getParameterTypes().length != 0) continue;
            Class<?> rt = method.getReturnType();
            if (rt == void.class || rt.isPrimitive()) continue;
            if (found == null) {
                found = rt;
            } else if (found != rt) {
                Utils.logw("TMoe: inconsistent MessageObject anchor types");
                return null;
            }
        }
        sMessageObject = found;
        return found;
    }

    public static Class<?> fromBaseFragmentGetter(String getterName) {
        synchronized (sBaseGetterTypes) {
            if (sBaseGetterTypes.containsKey(getterName)) {
                return sBaseGetterTypes.get(getterName);
            }
        }
        Class<?> base = baseFragment();
        Class<?> result = null;
        if (base != null) {
            try {
                Method method = base.getDeclaredMethod(getterName);
                method.setAccessible(true);
                result = method.getReturnType();
            } catch (Throwable ignored) {
                // Fall through to null. The caller can use a semantic resolver.
            }
        }
        synchronized (sBaseGetterTypes) {
            sBaseGetterTypes.put(getterName, result);
        }
        return result;
    }

    public static Class<?> messagesController() {
        Class<?> readable = Initiator.load("org.telegram.messenger.MessagesController");
        return readable != null ? readable : fromBaseFragmentGetter("getMessagesController");
    }

    public static Class<?> userConfig() {
        Class<?> readable = Initiator.load("org.telegram.messenger.UserConfig");
        return readable != null ? readable : fromBaseFragmentGetter("getUserConfig");
    }

    public static Class<?> downloadController() {
        Class<?> readable = Initiator.load("org.telegram.messenger.DownloadController");
        return readable != null ? readable : fromBaseFragmentGetter("getDownloadController");
    }

    public static Class<?> mediaDataController() {
        Class<?> readable = Initiator.load("org.telegram.messenger.MediaDataController");
        return readable != null ? readable : fromBaseFragmentGetter("getMediaDataController");
    }

    public static Class<?> messagesStorage() {
        Class<?> readable = Initiator.load("org.telegram.messenger.MessagesStorage");
        return readable != null ? readable : fromBaseFragmentGetter("getMessagesStorage");
    }

    public static Class<?> sendMessagesHelper() {
        Class<?> readable = Initiator.load("org.telegram.messenger.SendMessagesHelper");
        return readable != null ? readable : fromBaseFragmentGetter("getSendMessagesHelper");
    }

    public static String diagnosticSummary() {
        return "BaseFragment=" + name(baseFragment())
                + ", ChatActivity=" + name(chatActivity())
                + ", MessageObject=" + name(messageObject())
                + ", MessagesController=" + name(messagesController())
                + ", UserConfig=" + name(userConfig())
                + ", DownloadController=" + name(downloadController())
                + ", MediaDataController=" + name(mediaDataController())
                + ", MessagesStorage=" + name(messagesStorage());
    }

    private static String name(Class<?> c) {
        return c == null ? "?" : c.getName();
    }
}
