package cc.ioctl.tmoe.hook.core;

import android.app.Activity;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cc.ioctl.tmoe.R;
import cc.ioctl.tmoe.fragment.NativeSettingsDialog;
import cc.ioctl.tmoe.lifecycle.Parasitics;
import cc.ioctl.tmoe.ui.LocaleController;
import cc.ioctl.tmoe.util.HostDexClassScanner;
import cc.ioctl.tmoe.util.HostInfo;
import cc.ioctl.tmoe.util.Initiator;
import cc.ioctl.tmoe.util.Reflex;
import cc.ioctl.tmoe.util.Utils;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

/**
 * TMoe entry in Telegram settings.
 *
 * Telegram 13 release builds R8-rename SettingsActivity/UItem on every build.
 * For those builds we locate the settings fragment by its method shape, starting
 * from ProfileActivity's readable BaseFragment superclass, then clone Telegram's
 * own UItem objects so the TMoe section keeps the native visual style.
 */
public class SettingEntryHook implements Initializable, ProfileActivityRowHook.Callback {

    public static final SettingEntryHook INSTANCE = new SettingEntryHook();

    private static final String TMOE_SETTINGS_ROW = "TMOE_SETTINGS_ROW";
    private static final int TMOE_SETTINGS_ITEM_ID = 0x7F0E0001;
    private static final int LANGUAGE_ITEM_ID = 10;
    private static final int PREMIUM_ITEM_ID = 11;
    private static final int STARS_ITEM_ID = 12;
    private static final int HELP_FIRST_ITEM_ID = 17;

    private static final Map<Object, Boolean> OUR_ITEMS =
            Collections.synchronizedMap(new IdentityHashMap<>());

    private boolean mInitialized = false;

    private SettingEntryHook() {}

    @Override
    public boolean initialize() {
        if (mInitialized) return true;

        // Keep the legacy ProfileActivity integration for older Telegram builds.
        try {
            ProfileActivityRowHook.addCallback(this);
        } catch (Throwable e) {
            Utils.loge(e);
        }

        boolean hooked = false;
        try {
            hooked = hookNamedSettingsActivity();
        } catch (Throwable e) {
            Utils.loge(e);
        }
        if (!hooked) {
            try {
                hooked = hookR8SettingsActivity();
            } catch (Throwable e) {
                Utils.loge(e);
            }
        }

        if (!hooked) {
            Utils.logw("TMoe: SettingsActivity hook was not resolved");
        }
        mInitialized = true;
        return true;
    }

    // ---------------------------------------------------------------------------------------------
    // Legacy ProfileActivity row

    @Override
    public boolean onBindViewHolder(@NonNull String key, @NonNull Object holder,
                                    @NonNull Object adapter, @NonNull Object profileActivity) {
        if (!TMOE_SETTINGS_ROW.equals(key)) return false;
        FrameLayout textCell = (FrameLayout) Reflex.getInstanceObjectOrNull(holder, "itemView");
        if (textCell == null) return false;

        Parasitics.injectModuleResources(textCell.getContext().getResources());
        String text = LocaleController.getString("TMoeSettings", R.string.TMoeSettings);
        int iconResId = R.drawable.ic_setting_hex_outline_24;
        try {
            try {
                Reflex.invokeVirtual(textCell, "setTextAndIcon", text, iconResId, true,
                        CharSequence.class, int.class, boolean.class, void.class);
            } catch (NoSuchMethodException e) {
                Reflex.invokeVirtual(textCell, "setTextAndIcon", text, iconResId, true,
                        String.class, int.class, boolean.class, void.class);
            }
        } catch (Throwable e) {
            Utils.loge(e);
        }
        return true;
    }

    @Override
    public int getItemViewType(@NonNull String key, @NonNull Object adapter, @NonNull Object profileActivity) {
        return TMOE_SETTINGS_ROW.equals(key) ? 4 : -1;
    }

    @Override
    public boolean onItemClicked(@NonNull String key, @NonNull Object adapter, @NonNull Object profileActivity) {
        if (!TMOE_SETTINGS_ROW.equals(key)) return false;
        Activity activity = findActivity(profileActivity);
        if (activity != null) {
            NativeSettingsDialog.show(activity);
            return true;
        }
        return false;
    }

    @Override
    public void onInsertRow(@NonNull ProfileActivityRowHook.RowManipulator manipulator,
                            @NonNull Object profileActivity) {
        int targetRow = manipulator.getRowIdForField("languageRow");
        if (targetRow > 0) {
            manipulator.insertRowAtPosition(TMOE_SETTINGS_ROW, targetRow);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Unobfuscated Telegram / forks

    private static boolean hookNamedSettingsActivity() {
        try {
            final Class<?> settings = Initiator.load("org.telegram.ui.SettingsActivity");
            final Class<?> factory = Initiator.load("org.telegram.ui.SettingsActivity$SettingCell$Factory");
            if (settings == null || factory == null) return false;

            Method fill = null;
            Method click = null;
            for (Method m : settings.getDeclaredMethods()) {
                Class<?>[] p = m.getParameterTypes();
                if ("fillItems".equals(m.getName()) && p.length == 2
                        && p[0] == ArrayList.class && m.getReturnType() == void.class) {
                    fill = m;
                } else if ("onClick".equals(m.getName()) && p.length == 5) {
                    click = m;
                }
            }
            if (fill == null || click == null) return false;

            Method of = null;
            for (Method m : factory.getDeclaredMethods()) {
                if (Modifier.isStatic(m.getModifiers())
                        && m.getParameterTypes().length == 7
                        && m.getReturnType() != void.class) {
                    of = m;
                    break;
                }
            }
            if (of == null) return false;
            of.setAccessible(true);
            fill.setAccessible(true);
            click.setAccessible(true);
            final Method finalOf = of;

            XposedBridge.hookMethod(fill, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    try {
                        @SuppressWarnings("unchecked")
                        ArrayList<Object> list = (ArrayList<Object>) param.args[0];
                        if (list == null || list.isEmpty()) return;

                        Object item = finalOf.invoke(null,
                                TMOE_SETTINGS_ITEM_ID,
                                0xFFB07AF5,
                                0xFF8C57E8,
                                R.drawable.ic_setting_hex_outline_24,
                                LocaleController.getString("TMoeSettings", R.string.TMoeSettings),
                                null,
                                null);
                        list.add(0, item);
                        OUR_ITEMS.put(item, Boolean.TRUE);
                    } catch (Throwable e) {
                        Utils.loge(e);
                    }
                }
            });

            XposedBridge.hookMethod(click, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    Object item = param.args.length > 0 ? param.args[0] : null;
                    if (!OUR_ITEMS.containsKey(item)) return;
                    Activity activity = findActivity(param.thisObject);
                    if (activity != null) {
                        param.setResult(null);
                        NativeSettingsDialog.show(activity);
                    }
                }
            });
            Utils.logd("TMoe: hooked readable SettingsActivity");
            return true;
        } catch (Throwable e) {
            Utils.loge(e);
            return false;
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Telegram 13.x R8 build

    private static final class Located {
        final Class<?> fragment;
        final Method fill;
        final Method click;
        final Class<?> itemClass;

        Located(Class<?> fragment, Method fill, Method click, Class<?> itemClass) {
            this.fragment = fragment;
            this.fill = fill;
            this.click = click;
            this.itemClass = itemClass;
        }
    }

    private static boolean hookR8SettingsActivity() {
        Class<?> profile = Initiator.load("org.telegram.ui.ProfileActivity");
        if (profile == null || profile.getSuperclass() == null) {
            Utils.logw("TMoe: ProfileActivity/BaseFragment not found");
            return false;
        }

        Class<?> baseFragment = profile.getSuperclass();
        Located loc = locateSettingsFragment(baseFragment);
        if (loc == null) {
            Utils.logw("TMoe: R8 SettingsActivity was not uniquely resolved");
            return false;
        }

        XposedBridge.hookMethod(loc.fill, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                try {
                    if (param.args.length < 2 || !(param.args[1] instanceof ArrayList)) return;
                    @SuppressWarnings("unchecked")
                    ArrayList<Object> list = (ArrayList<Object>) param.args[1];
                    injectNativeSection(list, loc.itemClass);
                } catch (Throwable e) {
                    Utils.loge(e);
                }
            }
        });

        XposedBridge.hookMethod(loc.click, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                try {
                    if (param.args.length < 2) return;
                    Object item = param.args[1];
                    if (!OUR_ITEMS.containsKey(item)) return;

                    Activity activity = findActivity(param.args[0]);
                    if (activity == null) {
                        Utils.logw("TMoe: Settings Activity context not found");
                        return;
                    }
                    param.setResult(null);
                    NativeSettingsDialog.show(activity);
                } catch (Throwable e) {
                    Utils.loge(e);
                }
            }
        });

        Utils.logd("TMoe: dynamically hooked Settings in " + loc.fragment.getName()
                + " (" + loc.fill.getName() + "/" + loc.click.getName() + ")");
        return true;
    }

    private static Located locateSettingsFragment(Class<?> baseFragment) {
        Located found = null;
        List<String> names = HostDexClassScanner.directSubclassesOf(baseFragment);
        for (String name : names) {
            Located candidate = checkSettingsShape(name);
            if (candidate == null) continue;
            if (found != null) {
                Utils.logw("TMoe: multiple Settings-like fragments: "
                        + found.fragment.getName() + " and " + candidate.fragment.getName());
                return null;
            }
            found = candidate;
        }
        return found;
    }

    private static Located checkSettingsShape(String className) {
        try {
            Class<?> c = Class.forName(className, false, HostInfo.getApplication().getClassLoader());
            Method fill = null;
            int fillCount = 0;
            List<Method> clickCandidates = new ArrayList<>();
            List<Method> longCandidates = new ArrayList<>();

            for (Method m : c.getDeclaredMethods()) {
                if (!Modifier.isStatic(m.getModifiers())) continue;
                Class<?>[] p = m.getParameterTypes();
                if (p.length == 0 || p[0] != c) continue;

                if (p.length == 2 && p[1] == ArrayList.class && m.getReturnType() == void.class) {
                    fill = m;
                    fillCount++;
                } else if (p.length == 2 && !p[1].isPrimitive()
                        && p[1] != ArrayList.class && m.getReturnType() == void.class) {
                    clickCandidates.add(m);
                } else if (p.length == 3 && !p[1].isPrimitive()
                        && p[2] == View.class && m.getReturnType() == boolean.class) {
                    longCandidates.add(m);
                }
            }

            if (fillCount != 1 || longCandidates.size() != 1) return null;
            Class<?> itemClass = longCandidates.get(0).getParameterTypes()[1];

            Method click = null;
            int matches = 0;
            for (Method m : clickCandidates) {
                Class<?>[] p = m.getParameterTypes();
                if (p[1] == itemClass) {
                    click = m;
                    matches++;
                }
            }
            if (matches != 1 || click == null) return null;

            fill.setAccessible(true);
            click.setAccessible(true);
            return new Located(c, fill, click, itemClass);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void injectNativeSection(ArrayList<Object> list, Class<?> itemClass) throws Throwable {
        if (list == null || list.isEmpty()) return;
        if (OUR_ITEMS.size() > 64) OUR_ITEMS.clear();

        Field idField = findIdField(list, itemClass);
        if (idField == null) {
            Utils.logw("TMoe: UItem id field not resolved");
            return;
        }

        if (findById(list, itemClass, idField, TMOE_SETTINGS_ITEM_ID) >= 0) return;

        int languageIndex = findById(list, itemClass, idField, LANGUAGE_ITEM_ID);
        int premiumIndex = findById(list, itemClass, idField, PREMIUM_ITEM_ID);
        int starsIndex = findById(list, itemClass, idField, STARS_ITEM_ID);
        if (languageIndex < 0 || premiumIndex < 0 || starsIndex < 0 || premiumIndex <= languageIndex) {
            Utils.logw("TMoe: Settings anchors Language/Premium/Stars not found");
            return;
        }

        Object language = list.get(languageIndex);
        Object shadowTemplate = languageIndex + 1 < list.size() ? list.get(languageIndex + 1) : null;
        Object headerTemplate = findHeaderTemplate(list, itemClass, idField, premiumIndex);
        if (shadowTemplate == null || headerTemplate == null) {
            Utils.logw("TMoe: native header/shadow templates not found");
            return;
        }

        Object header = cloneObject(headerTemplate);
        Object row = cloneObject(language);
        Object shadow = cloneObject(shadowTemplate);

        setFirstCharSequence(header, "TMoe", true);
        setId(row, idField, TMOE_SETTINGS_ITEM_ID);
        setRowText(row,
                LocaleController.getString("TMoeSettings", R.string.TMoeSettings),
                null,
                null);

        Field iconField = findResourceIdField(language, idField);
        if (iconField != null) {
            try {
                Parasitics.injectModuleResources(HostInfo.getApplication().getResources());
                iconField.setInt(row, R.drawable.ic_setting_hex_outline_24);
            } catch (Throwable e) {
                Utils.loge(e);
            }
        }

        int insertAt = Math.min(languageIndex + 2, list.size());
        list.add(insertAt, header);
        list.add(insertAt + 1, row);
        list.add(insertAt + 2, shadow);
        OUR_ITEMS.put(row, Boolean.TRUE);

        Utils.logd("TMoe: native Settings section inserted after Language");
    }

    private static Field findIdField(ArrayList<Object> list, Class<?> itemClass) {
        for (Field f : allFields(itemClass)) {
            if (Modifier.isStatic(f.getModifiers()) || f.getType() != int.class) continue;
            f.setAccessible(true);
            Set<Integer> values = new HashSet<>();
            try {
                for (Object item : list) {
                    if (item != null && itemClass.isInstance(item)) {
                        values.add(f.getInt(item));
                    }
                }
            } catch (Throwable ignored) {
                continue;
            }
            if (values.contains(LANGUAGE_ITEM_ID)
                    && values.contains(PREMIUM_ITEM_ID)
                    && values.contains(STARS_ITEM_ID)) {
                return f;
            }
        }
        return null;
    }

    private static Field findResourceIdField(Object item, Field idField) {
        if (item == null) return null;
        for (Field f : allFields(item.getClass())) {
            if (f == idField || Modifier.isStatic(f.getModifiers()) || f.getType() != int.class) continue;
            try {
                f.setAccessible(true);
                int value = f.getInt(item);
                if ((value & 0xFF000000) == 0x7F000000) {
                    return f;
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static Object findHeaderTemplate(ArrayList<Object> list, Class<?> itemClass,
                                             Field idField, int premiumIndex) throws Throwable {
        int helpIndex = findById(list, itemClass, idField, HELP_FIRST_ITEM_ID);
        if (helpIndex > premiumIndex) {
            for (int i = helpIndex - 1; i > premiumIndex; i--) {
                Object item = list.get(i);
                if (isHeaderLike(item, itemClass, idField)) return item;
            }
        }
        // Generic fallback: any id=0 item with exactly one non-null CharSequence.
        for (Object item : list) {
            if (isHeaderLike(item, itemClass, idField)) return item;
        }
        return null;
    }

    private static boolean isHeaderLike(Object item, Class<?> itemClass, Field idField) throws Throwable {
        if (item == null || !itemClass.isInstance(item) || idField.getInt(item) != 0) return false;
        int nonNull = 0;
        for (Field f : charSequenceFields(itemClass)) {
            if (f.get(item) != null) nonNull++;
        }
        return nonNull == 1;
    }

    private static int findById(ArrayList<Object> list, Class<?> itemClass, Field idField, int id) {
        try {
            for (int i = 0; i < list.size(); i++) {
                Object item = list.get(i);
                if (item != null && itemClass.isInstance(item) && idField.getInt(item) == id) {
                    return i;
                }
            }
        } catch (Throwable e) {
            Utils.loge(e);
        }
        return -1;
    }

    private static void setId(Object item, Field idField, int id) throws IllegalAccessException {
        idField.setInt(item, id);
    }

    private static void setRowText(Object item, CharSequence title,
                                   CharSequence subtitle, CharSequence value) throws Throwable {
        List<Field> fields = charSequenceFields(item.getClass());
        if (fields.size() > 0) fields.get(0).set(item, title);
        if (fields.size() > 1) fields.get(1).set(item, subtitle);
        if (fields.size() > 2) fields.get(2).set(item, value);
    }

    private static void setFirstCharSequence(Object item, CharSequence text, boolean clearRest) throws Throwable {
        List<Field> fields = charSequenceFields(item.getClass());
        if (fields.isEmpty()) return;
        fields.get(0).set(item, text);
        if (clearRest) {
            for (int i = 1; i < fields.size(); i++) fields.get(i).set(item, null);
        }
    }

    private static List<Field> charSequenceFields(Class<?> cls) {
        List<Field> out = new ArrayList<>();
        for (Field f : cls.getDeclaredFields()) {
            if (!Modifier.isStatic(f.getModifiers())
                    && CharSequence.class.isAssignableFrom(f.getType())) {
                f.setAccessible(true);
                out.add(f);
            }
        }
        return out;
    }

    private static List<Field> allFields(Class<?> cls) {
        List<Field> out = new ArrayList<>();
        for (Class<?> c = cls; c != null && c != Object.class; c = c.getSuperclass()) {
            Collections.addAll(out, c.getDeclaredFields());
        }
        return out;
    }

    private static Object cloneObject(Object template) throws Throwable {
        Class<?> cls = template.getClass();
        Object copy = newBlank(cls);
        for (Class<?> c = cls; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers())) continue;
                f.setAccessible(true);
                f.set(copy, f.get(template));
            }
        }
        return copy;
    }

    private static Object newBlank(Class<?> cls) throws Throwable {
        try {
            Constructor<?> ctor = cls.getDeclaredConstructor(int.class);
            ctor.setAccessible(true);
            return ctor.newInstance(0);
        } catch (Throwable ignored) {
            Field f = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
            f.setAccessible(true);
            Object unsafe = f.get(null);
            return unsafe.getClass().getMethod("allocateInstance", Class.class).invoke(unsafe, cls);
        }
    }

    private static Activity findActivity(Object fragment) {
        if (fragment == null) return null;

        for (Class<?> c = fragment.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                if (Modifier.isStatic(m.getModifiers())
                        || m.getParameterTypes().length != 0
                        || !Activity.class.isAssignableFrom(m.getReturnType())) {
                    continue;
                }
                try {
                    m.setAccessible(true);
                    Object value = m.invoke(fragment);
                    if (value instanceof Activity) return (Activity) value;
                } catch (Throwable ignored) {
                }
            }
        }

        for (Field f : allFields(fragment.getClass())) {
            if (Modifier.isStatic(f.getModifiers())
                    || !Activity.class.isAssignableFrom(f.getType())) continue;
            try {
                f.setAccessible(true);
                Object value = f.get(fragment);
                if (value instanceof Activity) return (Activity) value;
            } catch (Throwable ignored) {
            }
        }
        return null;
    }
}
