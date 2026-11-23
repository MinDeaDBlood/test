package cc.ioctl.tmoe.hook.core;

import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

import cc.ioctl.tmoe.R;
import cc.ioctl.tmoe.fragment.SettingsFragment;
import cc.ioctl.tmoe.lifecycle.Parasitics;
import cc.ioctl.tmoe.rtti.ProxyFragmentRttiHandler;
import cc.ioctl.tmoe.ui.LocaleController;
import cc.ioctl.tmoe.util.HostInfo;
import cc.ioctl.tmoe.util.Initiator;
import cc.ioctl.tmoe.util.Reflex;
import cc.ioctl.tmoe.util.Utils;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

public class SettingEntryHook implements Initializable, ProfileActivityRowHook.Callback {
    public static final SettingEntryHook INSTANCE = new SettingEntryHook();

    private SettingEntryHook() {
    }

    private static final String TMOE_SETTINGS_ROW = "TMOE_SETTINGS_ROW";
    private static final int TMOE_SETTINGS_ITEM_ID = 0x7F0E0001;
    private static final int SETTINGS_ACTIVITY_LANGUAGE_ITEM_ID = 10;
    private static final int SETTINGS_ACTIVITY_ICON_COLOR_TOP = 0xFFB07AF5;
    private static final int SETTINGS_ACTIVITY_ICON_COLOR_BOTTOM = 0xFF8C57E8;
    private static Method sSettingsCellFactoryMethod = null;
    private static boolean sSettingsActivityHooked = false;

    private boolean mInitialized = false;

    @Override
    public boolean initialize() {
        if (mInitialized) {
            return true;
        }
        ProfileActivityRowHook.addCallback(this);
        hookSettingsActivityEntry();
        mInitialized = true;
        return true;
    }

    private static void presentTMoeSettingsFragment(@NonNull Object parentFragment) {
        ViewGroup parentLayout = ProxyFragmentRttiHandler.staticGetParentLayout(parentFragment);
        if (parentLayout != null) {
            ProxyFragmentRttiHandler.staticPresentFragment(parentLayout, new SettingsFragment(), false);
        }
    }

    @Override
    public boolean onBindViewHolder(@NonNull String key, @NonNull Object holder, @NonNull Object adpater, @NonNull Object profileActivity) {
        if (!TMOE_SETTINGS_ROW.equals(key)) {
            return false;
        }
        FrameLayout textCell = (FrameLayout) Reflex.getInstanceObjectOrNull(holder, "itemView");
        if (textCell != null) {
            Parasitics.injectModuleResources(textCell.getContext().getResources());
            Parasitics.injectModuleResources(HostInfo.getApplication().getResources());
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
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        } else {
            Utils.loge(new IllegalStateException("textCell is null"));
        }
        return true;
    }

    @Override
    public int getItemViewType(@NonNull String key, @NonNull Object adapter, @NonNull Object profileActivity) {
        if (TMOE_SETTINGS_ROW.equals(key)) {
            return 4;
        }
        return -1;
    }

    @Override
    public boolean onItemClicked(@NonNull String key, @NonNull Object adapter, @NonNull Object profileActivity) {
        if (TMOE_SETTINGS_ROW.equals(key)) {
            presentTMoeSettingsFragment(profileActivity);
            return true;
        }
        return false;
    }

    @Override
    public void onInsertRow(@NonNull ProfileActivityRowHook.RowManipulator manipulator, @NonNull Object profileActivity) {
        int targetRow = manipulator.getRowIdForField("languageRow");
        if (targetRow <= 0) {
            return;
        }
        manipulator.insertRowAtPosition(TMOE_SETTINGS_ROW, targetRow);
    }

    private static void hookSettingsActivityEntry() {
        if (sSettingsActivityHooked) {
            return;
        }
        if (hookNamedSettingsActivity()) {
            sSettingsActivityHooked = true;
            return;
        }
        if (hookTelegram1331ObfuscatedSettings()) {
            sSettingsActivityHooked = true;
        }
    }

    private static boolean hookNamedSettingsActivity() {
        try {
            Class<?> kSettingsActivity = Initiator.load("org.telegram.ui.SettingsActivity");
            if (kSettingsActivity == null) {
                return false;
            }
            Class<?> kSettingCellFactory = Initiator.load("org.telegram.ui.SettingsActivity$SettingCell$Factory");
            if (kSettingCellFactory == null) {
                return false;
            }
            sSettingsCellFactoryMethod = kSettingCellFactory.getDeclaredMethod(
                    "of",
                    int.class, int.class, int.class, int.class, CharSequence.class, CharSequence.class, CharSequence.class
            );
            sSettingsCellFactoryMethod.setAccessible(true);

            Method fillItems = null;
            Method onClick = null;
            for (Method m : kSettingsActivity.getDeclaredMethods()) {
                if (fillItems == null && "fillItems".equals(m.getName())
                        && m.getParameterTypes().length == 2
                        && ArrayList.class.isAssignableFrom(m.getParameterTypes()[0])) {
                    fillItems = m;
                } else if (onClick == null && "onClick".equals(m.getName())
                        && m.getParameterTypes().length == 5) {
                    onClick = m;
                }
            }
            if (fillItems == null || onClick == null) {
                return false;
            }

            XposedBridge.hookMethod(fillItems, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!(param.args[0] instanceof ArrayList)) {
                        return;
                    }
                    @SuppressWarnings("unchecked")
                    ArrayList<Object> items = (ArrayList<Object>) param.args[0];
                    int insertAt = findSettingsActivityLanguageItemIndex(items);
                    if (insertAt < 0 || containsSettingsActivityItem(items, TMOE_SETTINGS_ITEM_ID)) {
                        return;
                    }
                    Object item = createSettingsActivityItem();
                    if (item != null) {
                        items.add(insertAt, item);
                    }
                }
            });

            XposedBridge.hookMethod(onClick, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    Object item = param.args[0];
                    if (getSettingsActivityItemId(item) == TMOE_SETTINGS_ITEM_ID) {
                        presentTMoeSettingsFragment(param.thisObject);
                        param.setResult(null);
                    }
                }
            });
            return true;
        } catch (Throwable e) {
            Utils.loge(e);
            return false;
        }
    }

    private static boolean hookTelegram1331ObfuscatedSettings() {
        try {
            final Class<?> kSettingsActivity = Initiator.load("org.telegram.ui.y41");
            final Class<?> kFillCallback = Initiator.load("org.telegram.ui.b5");
            final Class<?> kClickCallback = Initiator.load("org.telegram.ui.p41");
            final Class<?> kSettingCellFactory = Initiator.load("org.telegram.ui.u41");
            final Class<?> kUItem = Initiator.load("org.telegram.ui.Components.j51");
            if (kSettingsActivity == null || kFillCallback == null || kClickCallback == null
                    || kSettingCellFactory == null || kUItem == null) {
                Utils.loge("TMoe: Telegram 13.3.1 settings classes not found");
                return false;
            }

            final Method itemFactory = findStaticFactory(kSettingCellFactory, "i", kUItem, 7);
            final Method shadowFactory = findStaticFactory(kUItem, "C", kUItem, 1);
            final Method headerFactory = findStaticFactory(kUItem, "u", kUItem, 1);
            final Field itemId = kUItem.getDeclaredField("d");
            itemId.setAccessible(true);

            Method fillCallback = null;
            for (Method m : kFillCallback.getDeclaredMethods()) {
                Class<?>[] pt = m.getParameterTypes();
                if ("a".equals(m.getName())
                        && m.getReturnType() == void.class
                        && pt.length == 2
                        && pt[0] == Object.class
                        && pt[1] == Object.class) {
                    fillCallback = m;
                    break;
                }
            }
            if (fillCallback == null) {
                Utils.loge("TMoe: Telegram 13.3.1 fill callback not found");
                return false;
            }
            fillCallback.setAccessible(true);

            Method clickCallback = null;
            for (Method m : kClickCallback.getDeclaredMethods()) {
                Class<?>[] pt = m.getParameterTypes();
                if ("i".equals(m.getName())
                        && m.getReturnType() == void.class
                        && pt.length == 5
                        && pt[0] == Object.class
                        && pt[1] == Object.class
                        && Number.class.isAssignableFrom(pt[2])) {
                    clickCallback = m;
                    break;
                }
            }
            if (clickCallback == null) {
                Utils.loge("TMoe: Telegram 13.3.1 click callback not found");
                return false;
            }
            clickCallback.setAccessible(true);

            final Field discriminatorField = findFieldByType(kFillCallback, int.class);
            if (discriminatorField == null) {
                Utils.loge("TMoe: Telegram 13.3.1 fill discriminator field not found");
                return false;
            }
            discriminatorField.setAccessible(true);

            XposedBridge.hookMethod(fillCallback, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    try {
                        if (discriminatorField.getInt(param.thisObject) != 27) {
                            return;
                        }
                        Object owner = findCapturedInstance(param.thisObject, kSettingsActivity);
                        if (owner == null || !(param.args[0] instanceof ArrayList)) {
                            return;
                        }
                        @SuppressWarnings("unchecked")
                        ArrayList<Object> items = (ArrayList<Object>) param.args[0];

                        int languageIndex = findItemById(items, itemId, SETTINGS_ACTIVITY_LANGUAGE_ITEM_ID);
                        if (languageIndex < 0 || findItemById(items, itemId, TMOE_SETTINGS_ITEM_ID) >= 0) {
                            return;
                        }

                        Parasitics.injectModuleResources(HostInfo.getApplication().getResources());
                        String text = LocaleController.getString("TMoeSettings", R.string.TMoeSettings);

                        Object header = headerFactory.invoke(null, "TMoe");
                        Object item = itemFactory.invoke(
                                null,
                                TMOE_SETTINGS_ITEM_ID,
                                SETTINGS_ACTIVITY_ICON_COLOR_TOP,
                                SETTINGS_ACTIVITY_ICON_COLOR_BOTTOM,
                                R.drawable.ic_setting_hex_outline_24,
                                text,
                                null,
                                null
                        );
                        Object shadow = shadowFactory.invoke(null, new Object[]{null});

                        // Telegram 13.3.1 layout:
                        // Language (id 10) -> existing shadow -> Premium/Stars block.
                        // Insert our native section after the existing shadow.
                        int insertAt = Math.min(languageIndex + 2, items.size());
                        items.add(insertAt, header);
                        items.add(insertAt + 1, item);
                        items.add(insertAt + 2, shadow);
                    } catch (Throwable e) {
                        Utils.loge(e);
                    }
                }
            });

            XposedBridge.hookMethod(clickCallback, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    try {
                        Object item = param.args.length > 0 ? param.args[0] : null;
                        if (item == null || !kUItem.isInstance(item) || itemId.getInt(item) != TMOE_SETTINGS_ITEM_ID) {
                            return;
                        }
                        Object owner = findCapturedInstance(param.thisObject, kSettingsActivity);
                        if (owner != null) {
                            presentTMoeSettingsFragment(owner);
                            param.setResult(null);
                        }
                    } catch (Throwable e) {
                        Utils.loge(e);
                    }
                }
            });

            return true;
        } catch (Throwable e) {
            Utils.loge(e);
            return false;
        }
    }

    private static Field findFieldByType(Class<?> owner, Class<?> type) {
        for (Field f : owner.getDeclaredFields()) {
            if (!Modifier.isStatic(f.getModifiers()) && f.getType() == type) {
                return f;
            }
        }
        return null;
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

    private static Method findStaticFactory(Class<?> owner, String name, Class<?> returnType, int parameterCount)
            throws NoSuchMethodException {
        for (Method m : owner.getDeclaredMethods()) {
            if (name.equals(m.getName())
                    && Modifier.isStatic(m.getModifiers())
                    && m.getParameterTypes().length == parameterCount
                    && returnType.isAssignableFrom(m.getReturnType())) {
                m.setAccessible(true);
                return m;
            }
        }
        throw new NoSuchMethodException(owner.getName() + "#" + name + "/" + parameterCount);
    }

    private static int findItemById(ArrayList<?> items, Field itemId, int id) {
        for (int i = 0; i < items.size(); i++) {
            Object item = items.get(i);
            if (item == null || !itemId.getDeclaringClass().isInstance(item)) {
                continue;
            }
            try {
                if (itemId.getInt(item) == id) {
                    return i;
                }
            } catch (IllegalAccessException ignored) {
            }
        }
        return -1;
    }

    private static Object createSettingsActivityItem() {
        if (sSettingsCellFactoryMethod == null) {
            return null;
        }
        try {
            Parasitics.injectModuleResources(HostInfo.getApplication().getResources());
            String text = LocaleController.getString("TMoeSettings", R.string.TMoeSettings);
            return sSettingsCellFactoryMethod.invoke(
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

    private static int findSettingsActivityLanguageItemIndex(@NonNull ArrayList<?> items) {
        for (int i = 0; i < items.size(); i++) {
            if (getSettingsActivityItemId(items.get(i)) == SETTINGS_ACTIVITY_LANGUAGE_ITEM_ID) {
                return i;
            }
        }
        return -1;
    }

    private static boolean containsSettingsActivityItem(@NonNull ArrayList<?> items, int itemId) {
        for (Object item : items) {
            if (getSettingsActivityItemId(item) == itemId) {
                return true;
            }
        }
        return false;
    }

    private static int getSettingsActivityItemId(Object item) {
        Object id = item == null ? null : Reflex.getInstanceObjectOrNull(item, "id");
        return id instanceof Number ? ((Number) id).intValue() : Integer.MIN_VALUE;
    }
}
