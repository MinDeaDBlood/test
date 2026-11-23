#!/usr/bin/env python3
from pathlib import Path

p = Path("tmoe/app/src/main/java/cc/ioctl/tmoe/hook/core/SettingEntryHook.java")
s = p.read_text()

s = s.replace(
    "import java.lang.reflect.Method;\n",
    "import java.lang.reflect.Field;\nimport java.lang.reflect.Method;\nimport java.lang.reflect.Modifier;\n"
)
s = s.replace(
    "import java.util.ArrayList;\n",
    "import java.util.ArrayList;\nimport java.util.Collections;\nimport java.util.Set;\nimport java.util.WeakHashMap;\n"
)

s = s.replace(
    "    private static boolean sSettingsActivityHooked = false;\n",
    """    private static boolean sSettingsActivityHooked = false;
    // Telegram 13.3.1 standalone APK is R8-obfuscated. The public SettingsActivity,
    // its lambda callbacks and SettingCell.Factory are renamed in that exact build.
    private static boolean sObfuscatedSettingsActivityHooked = false;
    private static final Set<Object> sObfuscatedSettingsItems =
            Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));
"""
)

s = s.replace(
    "        hookSettingsActivityEntry();\n",
    "        hookSettingsActivityEntry();\n        hookObfuscatedSettingsActivityEntry();\n"
)

marker = "    private static Object createSettingsActivityItem() {\n"
if marker not in s:
    raise SystemExit("createSettingsActivityItem marker not found")

block = r'''    /**
     * Telegram 13.3.1 standalone (versionCode 73469) is R8-obfuscated.
     *
     * In that APK the relevant classes are:
     *   SettingsActivity                  -> org.telegram.ui.y41
     *   fillItems method-reference lambda -> org.telegram.ui.b5
     *   click/long-click lambda           -> org.telegram.ui.p41
     *   SettingCell.Factory               -> org.telegram.ui.u41
     *
     * We hook by erased method signatures instead of obfuscated method names.
     * The generated UItem object is tracked by identity, so no obfuscated field
     * name (such as UItem.id) is required for click dispatch.
     */
    private static void hookObfuscatedSettingsActivityEntry() {
        if (sObfuscatedSettingsActivityHooked) {
            return;
        }
        try {
            Class<?> kSettingsActivity = Initiator.load("org.telegram.ui.y41");
            Class<?> kFillCallback = Initiator.load("org.telegram.ui.b5");
            Class<?> kClickCallback = Initiator.load("org.telegram.ui.p41");
            Class<?> kSettingCellFactory = Initiator.load("org.telegram.ui.u41");
            if (kSettingsActivity == null || kFillCallback == null
                    || kClickCallback == null || kSettingCellFactory == null) {
                return;
            }

            Method fillCallback = null;
            for (Method m : kFillCallback.getDeclaredMethods()) {
                Class<?>[] pt = m.getParameterTypes();
                if (m.getReturnType() == void.class && pt.length == 2
                        && pt[0] == Object.class && pt[1] == Object.class) {
                    fillCallback = m;
                    break;
                }
            }

            Method clickCallback = null;
            for (Method m : kClickCallback.getDeclaredMethods()) {
                Class<?>[] pt = m.getParameterTypes();
                if (m.getReturnType() == void.class && pt.length == 5
                        && pt[0] == Object.class && pt[1] == Object.class
                        && Number.class.isAssignableFrom(pt[2])) {
                    clickCallback = m;
                    break;
                }
            }

            Method factoryMethod = null;
            for (Method m : kSettingCellFactory.getDeclaredMethods()) {
                Class<?>[] pt = m.getParameterTypes();
                if (Modifier.isStatic(m.getModifiers()) && pt.length == 7
                        && pt[0] == int.class && pt[1] == int.class
                        && pt[2] == int.class && pt[3] == int.class
                        && CharSequence.class.isAssignableFrom(pt[4])
                        && CharSequence.class.isAssignableFrom(pt[5])
                        && CharSequence.class.isAssignableFrom(pt[6])
                        && m.getReturnType() != void.class) {
                    factoryMethod = m;
                    break;
                }
            }

            if (fillCallback == null || clickCallback == null || factoryMethod == null) {
                Utils.loge("TMoe: unable to resolve obfuscated Telegram 13.3.1 settings callbacks");
                return;
            }
            fillCallback.setAccessible(true);
            clickCallback.setAccessible(true);
            factoryMethod.setAccessible(true);

            final Method resolvedFactoryMethod = factoryMethod;
            XposedBridge.hookMethod(fillCallback, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    try {
                        Object owner = findCapturedInstance(param.thisObject, kSettingsActivity);
                        if (owner == null || !(param.args[0] instanceof ArrayList)) {
                            return;
                        }
                        @SuppressWarnings("unchecked")
                        ArrayList<Object> items = (ArrayList<Object>) param.args[0];

                        synchronized (sObfuscatedSettingsItems) {
                            for (Object existing : items) {
                                if (sObfuscatedSettingsItems.contains(existing)) {
                                    return;
                                }
                            }
                        }

                        Object item = createObfuscatedSettingsActivityItem(resolvedFactoryMethod);
                        if (item != null) {
                            // Keep the host ordering intact and add TMoe to the same settings list.
                            items.add(item);
                            sObfuscatedSettingsItems.add(item);
                        }
                    } catch (Throwable e) {
                        Utils.loge(e);
                    }
                }
            });

            XposedBridge.hookMethod(clickCallback, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    try {
                        Object owner = findCapturedInstance(param.thisObject, kSettingsActivity);
                        Object item = param.args.length > 0 ? param.args[0] : null;
                        if (owner != null && item != null && sObfuscatedSettingsItems.contains(item)) {
                            presentTMoeSettingsFragment(owner);
                            param.setResult(null);
                        }
                    } catch (Throwable e) {
                        Utils.loge(e);
                    }
                }
            });

            sObfuscatedSettingsActivityHooked = true;
        } catch (Throwable e) {
            Utils.loge(e);
        }
    }

    private static Object findCapturedInstance(Object wrapper, Class<?> expectedType) {
        if (wrapper == null || expectedType == null) {
            return null;
        }
        try {
            for (Field f : wrapper.getClass().getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers())) {
                    continue;
                }
                f.setAccessible(true);
                Object value = f.get(wrapper);
                if (expectedType.isInstance(value)) {
                    return value;
                }
            }
        } catch (Throwable e) {
            Utils.loge(e);
        }
        return null;
    }

    private static Object createObfuscatedSettingsActivityItem(Method factoryMethod) {
        try {
            Parasitics.injectModuleResources(HostInfo.getApplication().getResources());
            String text = LocaleController.getString("TMoeSettings", R.string.TMoeSettings);
            return factoryMethod.invoke(
                    null,
                    TMOE_SETTINGS_ITEM_ID,
                    SETTINGS_ACTIVITY_ICON_COLOR_TOP,
                    SETTINGS_ACTIVITY_ICON_COLOR_BOTTOM,
                    R.drawable.ic_setting_hex_outline_24,
                    text,
                    null,
                    null
            );
        } catch (Throwable e) {
            Utils.loge(e);
            return null;
        }
    }

'''

s = s.replace(marker, block + marker, 1)
p.write_text(s)
print("patched", p)
