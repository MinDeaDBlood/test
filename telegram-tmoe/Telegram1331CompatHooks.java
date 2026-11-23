package cc.ioctl.tmoe.hook.core;

import android.app.AlertDialog;
import android.content.Context;
import android.view.View;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import cc.ioctl.tmoe.util.HostInfo;
import cc.ioctl.tmoe.util.Initiator;
import cc.ioctl.tmoe.util.Utils;
import cc.ioctl.tmoe.util.config.ConfigManager;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

/**
 * Telegram 13.3.1 standalone R8 compatibility layer.
 *
 * The original TMoe hooks used Telegram source names directly. The official
 * 13.3.1 standalone APK minifies most host classes/methods, so this layer hooks
 * verified R8 targets while keeping the original TMoe preference keys.
 */
public final class Telegram1331CompatHooks implements Initializable {

    public static final Telegram1331CompatHooks INSTANCE = new Telegram1331CompatHooks();
    private static volatile boolean sInitialized;

    private Telegram1331CompatHooks() {}

    public static boolean isTarget() {
        return "org.telegram.messenger.web".equals(HostInfo.getPackageName());
    }

    private static boolean enabled(String key) {
        try {
            return ConfigManager.getDefaultConfig().getBooleanOrDefault(key + ".enabled", false);
        } catch (Throwable e) {
            Utils.loge(e);
            return false;
        }
    }

    public static void onPreferenceChanged(String key, boolean value) {
        if (!isTarget()) return;
        if ("EnableDebugMode".equals(key) && value) {
            applyDebugFlags();
        }
        if ("DisableInstantCamera".equals(key)) {
            applyInstantCameraPreference(value);
        }
    }

    @Override
    public boolean initialize() {
        if (!isTarget() || sInitialized) return true;
        try {
            installMessageObjectHooks();
            installUserAndConfigHooks();
            installExactLastSeen();
            installInstantCamera();
            installChatGreetings();
            installSendCommandGuard();
            installStoriesHooks();
            installSponsoredAndChatHooks();
            installStickerHooks();
            installGhostMode();
            installAntiRecall();
            installSpeedBoost();
            installDownloadsUiHooks();
            installMessageCellHooks();
            if (enabled("EnableDebugMode")) applyDebugFlags();
            sInitialized = true;
            Utils.logd("TMoe: Telegram 13.3.1 compatibility hooks installed");
            return true;
        } catch (Throwable e) {
            Utils.loge(e);
            return false;
        }
    }

    private static Class<?> host(String sourceName) throws ClassNotFoundException {
        return Initiator.loadClass(sourceName);
    }

    private static Method exact(Class<?> c, String name, Class<?>... p) throws NoSuchMethodException {
        Method m = c.getDeclaredMethod(name, p);
        m.setAccessible(true);
        return m;
    }

    private static void hook(Method m, XC_MethodHook cb) {
        m.setAccessible(true);
        XposedBridge.hookMethod(m, cb);
    }

    private static void installMessageObjectHooks() throws Exception {
        final Class<?> fj = host("org.telegram.messenger.MessageObject");

        hook(exact(fj, "w"), new XC_MethodHook(10000) {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                if (enabled("AntiAntiForward")) p.setResult(true);
            }
        });
        hook(exact(fj, "l4"), new XC_MethodHook(10000) {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                if (enabled("AntiAntiForward")) p.setResult(false);
            }
        });
        hook(exact(fj, "m4", host("org.telegram.tgnet.TLRPC$Message")), new XC_MethodHook(10000) {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                if (enabled("AntiAntiForward")) p.setResult(false);
            }
        });
        hook(exact(fj, "t2"), new XC_MethodHook(10000) {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                if (enabled("ProhibitSpoilers")) p.setResult(false);
            }
        });
        hook(exact(fj, "X3", host("org.telegram.tgnet.TLRPC$Document")), new XC_MethodHook(10000) {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                if (enabled("DisablePremiumStickerAnimation")) p.setResult(false);
            }
        });
        try {
            hook(exact(fj, "W3"), new XC_MethodHook(10000) {
                @Override protected void beforeHookedMethod(MethodHookParam p) {
                    if (enabled("DisablePremiumStickerAnimation")) p.setResult(false);
                }
            });
        } catch (NoSuchMethodException ignored) {}
    }

    private static void installUserAndConfigHooks() throws Exception {
        final Class<?> uc = host("org.telegram.messenger.UserConfig");
        hook(exact(uc, "q"), new XC_MethodHook(10000) {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                if (enabled("FakePremium")) p.setResult(true);
            }
        });
        hook(exact(uc, "o"), new XC_MethodHook(10000) {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                if (enabled("FakePremium")) p.setResult(true);
            }
        });
        hook(exact(uc, "f"), new XC_MethodHook(10000) {
            @Override protected void afterHookedMethod(MethodHookParam p) {
                if (!enabled("HidePhoneNumber")) return;
                Object user = p.getResult();
                if (user == null) return;
                try {
                    Field f = user.getClass().getField("phone");
                    f.set(user, "");
                } catch (Throwable e) {
                    Utils.loge(e);
                }
            }
        });

        final Class<?> shared = host("org.telegram.messenger.SharedConfig");
        hook(exact(shared, "e"), new XC_MethodHook(10000) {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                if (enabled("ForceBlurChatAvailable")) p.setResult(true);
            }
        });

        final Class<?> locale = host("org.telegram.messenger.LocaleController");
        hook(exact(locale, "S", int[].class, int.class), new XC_MethodHook(10000) {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                if (!enabled("ChannelDetailNumbers")) return;
                int[] rounded = (int[]) p.args[0];
                int number = (Integer) p.args[1];
                if (rounded != null && rounded.length > 0) rounded[0] = number;
                p.setResult(String.valueOf(number));
            }
        });
    }


    private static void installExactLastSeen() {
        try {
            final Class<?> locale = host("org.telegram.messenger.LocaleController");
            hook(exact(locale, "w", long.class, boolean[].class), new XC_MethodHook(12000) {
                @Override protected void beforeHookedMethod(MethodHookParam p) {
                    if (!enabled("ShowExactLastSeenTime")) return;
                    long seconds = (Long) p.args[0];
                    if (seconds <= 0) return;
                    long millis = seconds * 1000L;
                    Locale loc = Locale.getDefault();
                    SimpleDateFormat time = new SimpleDateFormat("HH:mm:ss", loc);
                    SimpleDateFormat dateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", loc);
                    String lastSeen = hostString("LastSeenFormatted", "last seen %s");
                    String yesterdayAt = hostString("YesterdayAtFormatted", "yesterday at %s");
                    String lastSeenDate = hostString("LastSeenDateFormatted", "last seen %s");
                    if (isSameDay(millis, System.currentTimeMillis())) {
                        p.setResult(String.format(loc, lastSeen, time.format(new Date(millis))));
                    } else if (isYesterday(millis)) {
                        boolean[] shorter = (boolean[]) p.args[1];
                        if (shorter != null && shorter.length > 0) {
                            shorter[0] = true;
                            p.setResult(String.format(loc, yesterdayAt, time.format(new Date(millis))));
                        } else {
                            p.setResult(String.format(loc, lastSeen,
                                    String.format(loc, yesterdayAt, time.format(new Date(millis)))));
                        }
                    } else {
                        p.setResult(String.format(loc, lastSeenDate, dateTime.format(new Date(millis))));
                    }
                }
            });
        } catch (Throwable e) {
            Utils.loge(e);
        }
    }

    private static Boolean sOriginalInappCamera;

    private static void applyInstantCameraPreference(boolean disabled) {
        try {
            Class<?> shared = host("org.telegram.messenger.SharedConfig");
            Field inapp = shared.getDeclaredField("f22639y0");
            inapp.setAccessible(true);
            if (sOriginalInappCamera == null) sOriginalInappCamera = inapp.getBoolean(null);
            inapp.setBoolean(null, disabled ? false : sOriginalInappCamera);
        } catch (Throwable e) {
            Utils.loge(e);
        }
    }

    private static void installInstantCamera() {
        try {
            applyInstantCameraPreference(enabled("DisableInstantCamera"));
            final Class<?> enter = host("org.telegram.ui.Components.ChatActivityEnterView");
            // isInVideoMode()
            hook(exact(enter, "R0"), new XC_MethodHook(15000) {
                @Override protected void beforeHookedMethod(MethodHookParam p) {
                    if (enabled("DisableInstantCamera")) p.setResult(false);
                }
            });
            // checkRoundVideo(): force both state and UI back to voice mode.
            hook(exact(enter, "N"), new XC_MethodHook(-15000) {
                @Override protected void afterHookedMethod(MethodHookParam p) {
                    if (!enabled("DisableInstantCamera")) return;
                    try {
                        Field hasRecordVideo = enter.getDeclaredField("U2");
                        hasRecordVideo.setAccessible(true);
                        hasRecordVideo.setBoolean(p.thisObject, false);
                        exact(enter, "N1", boolean.class, boolean.class)
                                .invoke(p.thisObject, false, false);
                    } catch (Throwable e) {
                        Utils.loge(e);
                    }
                }
            });
        } catch (Throwable e) {
            Utils.loge(e);
        }
    }

    private static void installChatGreetings() {
        try {
            final Class<?> greetings = host("org.telegram.ui.Components.ChatGreetingsView");
            for (Method m : greetings.getDeclaredMethods()) {
                if ("setSticker".equals(m.getName()) || "setListener".equals(m.getName())) {
                    hook(m, new XC_MethodHook(15000) {
                        @Override protected void beforeHookedMethod(MethodHookParam p) {
                            if (enabled("ProhibitChatGreetings")) p.setResult(null);
                        }
                    });
                }
            }
            hook(exact(greetings, "onMeasure", int.class, int.class), new XC_MethodHook(-15000) {
                @Override protected void afterHookedMethod(MethodHookParam p) {
                    if (enabled("ProhibitChatGreetings") && p.thisObject instanceof View) {
                        ((View) p.thisObject).setVisibility(View.GONE);
                    }
                }
            });
        } catch (Throwable e) {
            Utils.loge(e);
        }
    }

    private static void installSendCommandGuard() {
        try {
            final Class<?> enter = host("org.telegram.ui.Components.ChatActivityEnterView");
            final Class<?> messageObject = host("org.telegram.messenger.MessageObject");
            final Method setCommand = exact(enter, "C1",
                    messageObject, String.class, boolean.class, boolean.class);
            hook(setCommand, new XC_MethodHook(16000) {
                @Override protected void beforeHookedMethod(MethodHookParam p) {
                    if (!enabled("SendCommand")) return;
                    String command = (String) p.args[1];
                    boolean longPress = (Boolean) p.args[2];
                    if (longPress || command == null || !command.startsWith("/")) return;
                    Context context = p.thisObject instanceof View
                            ? ((View) p.thisObject).getContext() : HostInfo.getApplication();
                    if (context == null) return;
                    final Object receiver = p.thisObject;
                    final Object[] args = p.args.clone();
                    new AlertDialog.Builder(context)
                            .setMessage("Отправить команду " + command + "?")
                            .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                                try {
                                    XposedBridge.invokeOriginalMethod(setCommand, receiver, args);
                                } catch (Throwable e) {
                                    Utils.loge(e);
                                }
                            })
                            .setNegativeButton(android.R.string.cancel, null)
                            .show();
                    p.setResult(null);
                }
            });
        } catch (Throwable e) {
            Utils.loge(e);
        }
    }

    private static String hostString(String name, String fallback) {
        try {
            Context c = HostInfo.getApplication();
            if (c == null) return fallback;
            int id = c.getResources().getIdentifier(name, "string", c.getPackageName());
            return id == 0 ? fallback : c.getString(id);
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private static boolean isSameDay(long a, long b) {
        Calendar x = Calendar.getInstance();
        x.setTimeInMillis(a);
        int y = x.get(Calendar.YEAR);
        int d = x.get(Calendar.DAY_OF_YEAR);
        x.setTimeInMillis(b);
        return y == x.get(Calendar.YEAR) && d == x.get(Calendar.DAY_OF_YEAR);
    }

    private static boolean isYesterday(long value) {
        Calendar target = Calendar.getInstance();
        target.setTimeInMillis(value);
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);
        return target.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR)
                && target.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR);
    }

    private static void installStoriesHooks() throws Exception {
        final Class<?> stories = host("org.telegram.ui.Stories.StoriesController");
        hook(exact(stories, "I"), new XC_MethodHook(10000) {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                if (enabled("HideStories")) p.setResult(false);
            }
        });
        hook(exact(stories, "K", long.class), new XC_MethodHook(10000) {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                if (enabled("HideStories")) p.setResult(false);
            }
        });
        hook(exact(stories, "Y",
                host("org.telegram.tgnet.tl.TL_stories$PeerStories"),
                host("org.telegram.tgnet.tl.TL_stories$StoryItem"),
                boolean.class), new XC_MethodHook(10000) {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                if (enabled("HideSeenStories") || enabled("GhostMode")) p.setResult(false);
            }
        });
    }

    private static void installSponsoredAndChatHooks() throws Exception {
        final Class<?> mc = host("org.telegram.messenger.MessagesController");
        hook(exact(mc, "Q1", long.class), new XC_MethodHook(10000) {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                if (enabled("HideSponsoredMsg")) p.setResult(null);
            }
        });

        try {
            Class<?> pulling = Class.forName("org.telegram.ui.mp", false, Initiator.getHostClassLoader());
            hook(exact(pulling, "c", long.class, int.class, int.class, boolean.class, int[].class),
                    new XC_MethodHook(10000) {
                @Override protected void beforeHookedMethod(MethodHookParam p) {
                    if (enabled("ProhibitChannelSwitching")) p.setResult(null);
                }
            });
        } catch (Throwable e) {
            Utils.loge(e);
        }

        try {
            Class<?> chat = host("org.telegram.ui.ChatActivity");
            for (Method m : chat.getDeclaredMethods()) {
                if ("Eb".equals(m.getName()) && m.getReturnType() == void.class) {
                    hook(m, new XC_MethodHook(10000) {
                        @Override protected void beforeHookedMethod(MethodHookParam p) {
                            if (enabled("ProhibitEnableReactions") || enabled("DisableQuickReaction")) {
                                p.setResult(null);
                            }
                        }
                    });
                }
            }
        } catch (Throwable e) {
            Utils.loge(e);
        }
    }

    private static void installStickerHooks() throws Exception {
        Class<?> media = host("org.telegram.messenger.MediaDataController");
        hook(exact(media, "u0", int.class), new XC_MethodHook(10000) {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                if (enabled("HidePremiumStickerSetTab") && ((Integer) p.args[0]) == 7) {
                    p.setResult(new ArrayList<Object>());
                }
            }
        });
        try {
            hook(exact(media, "k0", int.class, boolean.class), new XC_MethodHook(10000) {
                @Override protected void beforeHookedMethod(MethodHookParam p) {
                    if (enabled("HidePremiumStickerSetTab") && ((Integer) p.args[0]) == 7) {
                        p.setResult(new ArrayList<Object>());
                    }
                }
            });
        } catch (NoSuchMethodException ignored) {}
    }

    private static void installGhostMode() throws Exception {
        Class<?> cm = host("org.telegram.tgnet.ConnectionsManager");
        final Set<String> blocked = new HashSet<String>();
        blocked.add("org.telegram.tgnet.TLRPC$TL_messages_readHistory");
        blocked.add("org.telegram.tgnet.TLRPC$TL_channels_readHistory");
        blocked.add("org.telegram.tgnet.TLRPC$TL_messages_readMessageContents");
        blocked.add("org.telegram.tgnet.TLRPC$TL_messages_readMentions");
        blocked.add("org.telegram.tgnet.TLRPC$TL_messages_readReactions");
        blocked.add("org.telegram.tgnet.TLRPC$TL_messages_setTyping");
        blocked.add("org.telegram.tgnet.tl.TL_account$updateStatus");
        blocked.add("org.telegram.tgnet.tl.TL_stories$TL_stories_readStories");

        for (Method m : cm.getDeclaredMethods()) {
            Class<?>[] pt = m.getParameterTypes();
            if (!"sendRequest".equals(m.getName()) || m.getReturnType() != int.class
                    || pt.length == 0 || !"org.telegram.tgnet.TLObject".equals(pt[0].getName())) continue;
            hook(m, new XC_MethodHook(20000) {
                @Override protected void beforeHookedMethod(MethodHookParam p) {
                    Object req = p.args[0];
                    if (req == null) return;
                    String n = req.getClass().getName();

                    if (enabled("HideSponsoredMsg")
                            && (n.endsWith("$TL_messages_getSponsoredMessages")
                            || n.endsWith("$TL_contacts_getSponsoredPeers"))) {
                        p.setResult(0);
                        return;
                    }
                    if (enabled("FuckTrackingHook")
                            && (n.endsWith("$TL_messages_clickSponsoredMessage")
                            || n.endsWith("$TL_messages_viewSponsoredMessage")
                            || n.endsWith("$TL_messages_reportSponsoredMessage"))) {
                        p.setResult(0);
                        return;
                    }
                    if (enabled("ProhibitEnableReactions")
                            && (n.endsWith("$TL_messages_sendReaction")
                            || n.endsWith("$TL_messages_sendPaidReaction")
                            || n.endsWith("$TL_stories_sendReaction"))) {
                        p.setResult(0);
                        return;
                    }
                    if (enabled("GhostMode") && blocked.contains(n)) p.setResult(0);
                }
            });
        }
    }

    private static void installAntiRecall() throws Exception {
        Class<?> mc = host("org.telegram.messenger.MessagesController");
        hook(exact(mc, "U3", ArrayList.class, ArrayList.class, ArrayList.class,
                boolean.class, int.class), new XC_MethodHook(20000) {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                if (!enabled("AntiRecall") || !(p.args[0] instanceof ArrayList)) return;
                @SuppressWarnings("unchecked")
                ArrayList<Object> updates = (ArrayList<Object>) p.args[0];
                for (Iterator<Object> it = updates.iterator(); it.hasNext();) {
                    Object u = it.next();
                    if (u == null) continue;
                    String n = u.getClass().getName();
                    if ("org.telegram.tgnet.tl.TL_update$TL_updateDeleteMessages".equals(n)
                            || "org.telegram.tgnet.tl.TL_update$TL_updateDeleteChannelMessages".equals(n)
                            || "org.telegram.tgnet.tl.TL_update$TL_updateDeleteScheduledMessages".equals(n)
                            || "org.telegram.tgnet.tl.TL_update$TL_updateDeleteEphemeralMessages".equals(n)) {
                        it.remove();
                    }
                }
            }
        });
    }

    private static void installSpeedBoost() throws Exception {
        final Class<?> load = host("org.telegram.messenger.FileLoadOperation");
        hook(exact(load, "t"), new XC_MethodHook(-10000) {
            @Override protected void afterHookedMethod(MethodHookParam p) {
                if (!enabled("BoostSpeed")) return;
                try {
                    setInt(load, p.thisObject, "n", 1024 * 1024);
                    setInt(load, p.thisObject, "o", 12);
                    setInt(load, p.thisObject, "p", 12);
                    setInt(load, p.thisObject, "q", 2000);
                } catch (Throwable e) {
                    Utils.loge(e);
                }
            }
        });
    }

    private static void installDownloadsUiHooks() {
        try {
            final Class<?> dc = host("org.telegram.messenger.DownloadController");
            hook(exact(dc, "f"), new XC_MethodHook(15000) {
                @Override protected void beforeHookedMethod(MethodHookParam p) {
                    if (enabled("AlwaysShowDownloads")) p.setResult(true);
                }
            });
        } catch (Throwable e) {
            Utils.loge(e);
        }
        try {
            final Class<?> icon = Class.forName("org.telegram.ui.sx", false, Initiator.getHostClassLoader());
            hook(exact(icon, "a"), new XC_MethodHook(-10000) {
                @Override protected void afterHookedMethod(MethodHookParam p) {
                    if (!enabled("AlwaysShowDownloads")) return;
                    try {
                        Field progress = icon.getDeclaredField("e");
                        Field current = icon.getDeclaredField("f");
                        Field completed = icon.getDeclaredField("x");
                        progress.setAccessible(true);
                        current.setAccessible(true);
                        completed.setAccessible(true);
                        progress.setFloat(p.thisObject, 1f);
                        current.setFloat(p.thisObject, 1f);
                        completed.setBoolean(p.thisObject, true);
                    } catch (Throwable e) {
                        Utils.loge(e);
                    }
                }
            });
        } catch (Throwable e) {
            Utils.loge(e);
        }
    }

    private static void installMessageCellHooks() {
        try {
            final Class<?> cell = Class.forName("org.telegram.ui.Cells.q1", false, Initiator.getHostClassLoader());
            final Class<?> moClass = host("org.telegram.messenger.MessageObject");
            hook(exact(cell, "Z2", moClass), new XC_MethodHook(-10000) {
                @Override protected void afterHookedMethod(MethodHookParam p) {
                    if (!enabled("ShowMsgId")) return;
                    try {
                        Object mo = p.args[0];
                        Field ownerField = moClass.getDeclaredField("k");
                        ownerField.setAccessible(true);
                        Object owner = ownerField.get(mo);
                        if (owner == null) return;
                        int id = owner.getClass().getField("id").getInt(owner);
                        Field timeField = cell.getDeclaredField("E9");
                        timeField.setAccessible(true);
                        Object old = timeField.get(p.thisObject);
                        if (!(old instanceof CharSequence)) return;
                        String prefix = id + " ";
                        String oldText = old.toString();
                        if (!oldText.startsWith(prefix)) timeField.set(p.thisObject, prefix + oldText);
                        int extra = Math.max(18, prefix.length() * 8);
                        for (String fn : new String[]{"A9", "B9"}) {
                            try {
                                Field f = cell.getDeclaredField(fn);
                                f.setAccessible(true);
                                f.setInt(p.thisObject, f.getInt(p.thisObject) + extra);
                            } catch (Throwable ignored) {}
                        }
                    } catch (Throwable e) {
                        Utils.loge(e);
                    }
                }
            });
        } catch (Throwable e) {
            Utils.loge(e);
        }
    }

    private static void setInt(Class<?> owner, Object instance, String name, int value) throws Exception {
        Field f = owner.getDeclaredField(name);
        f.setAccessible(true);
        f.setInt(instance, value);
    }

    private static void applyDebugFlags() {
        try {
            Class<?> bv = host("org.telegram.messenger.BuildVars");
            Field debug = bv.getDeclaredField("a");
            Field logs = bv.getDeclaredField("b");
            debug.setAccessible(true);
            logs.setAccessible(true);
            debug.setBoolean(null, true);
            logs.setBoolean(null, true);
        } catch (Throwable e) {
            Utils.loge(e);
        }
    }
}
