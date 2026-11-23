package cc.ioctl.tmoe.fragment;

import android.app.Activity;
import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.util.StateSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import cc.ioctl.tmoe.R;
import cc.ioctl.tmoe.lifecycle.Parasitics;
import cc.ioctl.tmoe.ui.LocaleController;
import cc.ioctl.tmoe.ui.Theme;
import cc.ioctl.tmoe.ui.dsl.HierarchyDescription;
import cc.ioctl.tmoe.ui.dsl.TMsgListItem;
import cc.ioctl.tmoe.ui.wrapper.component.ThemedRecyclerListView;
import cc.ioctl.tmoe.util.Utils;

/**
 * Telegram-13-compatible host for TMoe settings.
 *
 * The original TMoe fragment depends on Telegram's readable BaseFragment classes.
 * Telegram 13.3.1 R8-renames that hierarchy, so this host keeps the functional
 * TMoe hierarchy but renders it using the current Telegram Theme table and the
 * same grouped-settings visual language: gray page background, white rounded
 * sections, Telegram action bar colors and Telegram switch colors.
 */
public final class NativeSettingsDialog {

    private static Dialog sCurrent;

    private NativeSettingsDialog() {}

    public static void show(Activity activity) {
        if (activity == null || activity.isFinishing()) return;
        try {
            if (sCurrent != null && sCurrent.isShowing()) {
                return;
            }
            Parasitics.injectModuleResources(activity.getResources());
            Theme.invalidate();

            final SettingsFragment settingsFragment = new SettingsFragment();
            final HierarchyDescription hierarchy = getHierarchy(settingsFragment);
            final List<TMsgListItem> items = hierarchy.collectItems(activity);
            if (items == null || items.isEmpty()) {
                Toast.makeText(activity, "TMoe: настройки не загружены", Toast.LENGTH_LONG).show();
                return;
            }

            final Dialog dialog = new Dialog(activity, android.R.style.Theme_Material_NoActionBar);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

            final int pageColor = Theme.getColor(Theme.key_windowBackgroundGray);
            final int cardColor = Theme.getColor(Theme.key_windowBackgroundWhite);
            final int actionBarColor = Theme.getColor(Theme.key_actionBarDefault);
            final int actionBarIconColor = Theme.getColor(Theme.key_actionBarDefaultIcon);
            final int actionBarTitleColor = Theme.getColor(Theme.key_actionBarDefaultTitle);

            LinearLayout root = new LinearLayout(activity);
            root.setOrientation(LinearLayout.VERTICAL);
            root.setBackgroundColor(pageColor);

            LinearLayout bar = new LinearLayout(activity);
            bar.setOrientation(LinearLayout.HORIZONTAL);
            bar.setGravity(Gravity.CENTER_VERTICAL);
            bar.setPadding(0, 0, dp(activity, 12), 0);
            bar.setBackground(makeActionBarBackground(activity, actionBarColor));

            int backRes = activity.getResources().getIdentifier(
                    "ic_ab_back", "drawable", activity.getPackageName());
            if (backRes != 0) {
                ImageView back = new ImageView(activity);
                back.setScaleType(ImageView.ScaleType.CENTER);
                back.setImageResource(backRes);
                back.setColorFilter(actionBarIconColor);
                back.setContentDescription("Назад");
                back.setOnClickListener(v -> dialog.dismiss());
                bar.addView(back, new LinearLayout.LayoutParams(dp(activity, 56), dp(activity, 56)));
            } else {
                TextView back = new TextView(activity);
                back.setText("‹");
                back.setTextSize(34);
                back.setGravity(Gravity.CENTER);
                back.setTextColor(actionBarIconColor);
                back.setContentDescription("Назад");
                back.setOnClickListener(v -> dialog.dismiss());
                bar.addView(back, new LinearLayout.LayoutParams(dp(activity, 56), dp(activity, 56)));
            }

            TextView title = new TextView(activity);
            title.setText(LocaleController.getString("TMoeSettings", R.string.TMoeSettings));
            title.setTextSize(20);
            title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            title.setTextColor(actionBarTitleColor);
            title.setGravity(Gravity.CENTER_VERTICAL);
            title.setSingleLine(true);
            bar.addView(title, new LinearLayout.LayoutParams(0, dp(activity, 56), 1f));

            root.addView(bar, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(activity, 56)));

            final ThemedRecyclerListView list = new ThemedRecyclerListView(activity);
            list.setItemAnimator(null);
            list.setLayoutAnimation(null);
            list.setLayoutManager(new LinearLayoutManager(activity, RecyclerView.VERTICAL, false));
            list.setVerticalScrollBarEnabled(false);
            list.setClipToPadding(false);
            list.setPadding(0, dp(activity, 6), 0, dp(activity, 16));
            list.setBackgroundColor(pageColor);

            final ArrayList<Class<?>> typeList = new ArrayList<>();
            for (TMsgListItem item : items) {
                if (!typeList.contains(item.getClass())) typeList.add(item.getClass());
            }
            final TMsgListItem[] delegates = new TMsgListItem[typeList.size()];
            for (TMsgListItem item : items) {
                int idx = typeList.indexOf(item.getClass());
                if (delegates[idx] == null) delegates[idx] = item;
            }

            final ThemedRecyclerListView.SelectionAdapter adapter = new ThemedRecyclerListView.SelectionAdapter() {
                @Override
                public boolean isEnabled(RecyclerView.ViewHolder holder) {
                    int p = holder.getAdapterPosition();
                    return p >= 0 && p < items.size() && items.get(p).isEnabled();
                }

                @Override
                public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                    TMsgListItem delegate = delegates[viewType];
                    return delegate.createViewHolder(activity, parent);
                }

                @Override
                public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
                    TMsgListItem item = items.get(position);
                    item.bindView(holder, position, activity);
                    applyTelegramSectionStyle(activity, holder.itemView, items, position,
                            item, cardColor, pageColor);
                }

                @Override
                public int getItemCount() {
                    return items.size();
                }

                @Override
                public int getItemViewType(int position) {
                    return typeList.indexOf(items.get(position).getClass());
                }
            };

            list.setAdapter(adapter);
            list.setOnItemClickListener((view, position, x, y) -> {
                try {
                    if (position < 0 || position >= items.size()) return;
                    items.get(position).onItemClick(view, position, x, y);
                    adapter.notifyItemChanged(position);
                } catch (Throwable e) {
                    Utils.loge(e);
                    Toast.makeText(activity, "TMoe: не удалось открыть этот пункт", Toast.LENGTH_SHORT).show();
                }
            });

            FrameLayout listFrame = new FrameLayout(activity);
            listFrame.setBackgroundColor(pageColor);
            listFrame.addView(list, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            root.addView(listFrame, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

            dialog.setContentView(root);
            Window w = dialog.getWindow();
            if (w != null) {
                w.setBackgroundDrawable(new ColorDrawable(pageColor));
                w.setStatusBarColor(actionBarColor);
                w.setNavigationBarColor(pageColor);
                w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            }
            dialog.setOnDismissListener(d -> {
                if (sCurrent == dialog) sCurrent = null;
            });
            sCurrent = dialog;
            dialog.show();
            if (w != null) {
                w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            }
        } catch (Throwable e) {
            Utils.loge(e);
            Toast.makeText(activity, "TMoe: ошибка открытия настроек", Toast.LENGTH_LONG).show();
        }
    }

    private static void applyTelegramSectionStyle(
            Activity activity,
            View view,
            List<TMsgListItem> items,
            int position,
            TMsgListItem item,
            int cardColor,
            int pageColor) {

        boolean header = isHeader(item);
        boolean shadow = isShadow(item);

        RecyclerView.LayoutParams lp;
        ViewGroup.LayoutParams raw = view.getLayoutParams();
        if (raw instanceof RecyclerView.LayoutParams) {
            lp = (RecyclerView.LayoutParams) raw;
        } else {
            lp = new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    raw != null ? raw.height : ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        if (header) {
            lp.leftMargin = dp(activity, 12);
            lp.rightMargin = dp(activity, 12);
            lp.topMargin = dp(activity, 6);
            lp.bottomMargin = 0;
            view.setLayoutParams(lp);
            view.setBackgroundColor(pageColor);
            return;
        }

        if (shadow) {
            lp.leftMargin = 0;
            lp.rightMargin = 0;
            lp.topMargin = 0;
            lp.bottomMargin = dp(activity, 2);
            view.setLayoutParams(lp);
            view.setBackgroundColor(pageColor);
            return;
        }

        if (item.isVoidBackground()) {
            view.setBackgroundColor(pageColor);
            return;
        }

        boolean firstInSection = position > 0 && isHeader(items.get(position - 1));
        boolean lastInSection = position + 1 < items.size() && isShadow(items.get(position + 1));

        lp.leftMargin = dp(activity, 12);
        lp.rightMargin = dp(activity, 12);
        lp.topMargin = 0;
        lp.bottomMargin = 0;
        view.setLayoutParams(lp);

        view.setBackground(makeSectionRowBackground(
                activity,
                cardColor,
                Theme.getColor(Theme.key_listSelector),
                firstInSection,
                lastInSection));
    }

    private static Drawable makeSectionRowBackground(
            Activity activity, int color, int selectorColor, boolean top, boolean bottom) {
        float r = dp(activity, 14);
        float[] radii = new float[]{
                top ? r : 0, top ? r : 0,
                top ? r : 0, top ? r : 0,
                bottom ? r : 0, bottom ? r : 0,
                bottom ? r : 0, bottom ? r : 0
        };

        GradientDrawable content = new GradientDrawable();
        content.setColor(color);
        content.setCornerRadii(radii);

        GradientDrawable mask = new GradientDrawable();
        mask.setColor(0xffffffff);
        mask.setCornerRadii(radii);

        int pressed = (selectorColor & 0x00ffffff) | 0x22000000;
        ColorStateList ripple = new ColorStateList(
                new int[][]{StateSet.WILD_CARD},
                new int[]{pressed});
        return new RippleDrawable(ripple, content, mask);
    }

    private static Drawable makeActionBarBackground(Activity activity, int color) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        float r = dp(activity, 16);
        bg.setCornerRadii(new float[]{0, 0, 0, 0, r, r, r, r});
        return bg;
    }

    private static boolean isHeader(TMsgListItem item) {
        String name = item.getClass().getSimpleName();
        return "Header".equals(name) || name.endsWith("Header");
    }

    private static boolean isShadow(TMsgListItem item) {
        String name = item.getClass().getSimpleName();
        return "ShadowSection".equals(name) || name.contains("ShadowSection");
    }

    private static HierarchyDescription getHierarchy(SettingsFragment fragment) throws Exception {
        Class<?> c = SettingsFragment.class;
        while (c != null) {
            for (Field f : c.getDeclaredFields()) {
                if (HierarchyDescription.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    Object v = f.get(fragment);
                    if (v instanceof HierarchyDescription) {
                        return (HierarchyDescription) v;
                    }
                }
            }
            c = c.getSuperclass();
        }
        throw new NoSuchFieldException("SettingsFragment hierarchy");
    }

    private static int dp(Activity a, int value) {
        return Math.round(value * a.getResources().getDisplayMetrics().density);
    }
}
