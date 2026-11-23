package cc.ioctl.tmoe.fragment;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
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
 * TMoe settings hosted in a plain Android Dialog.
 * It intentionally does not depend on Telegram BaseFragment / ActionBarLayout names.
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

            LinearLayout root = new LinearLayout(activity);
            root.setOrientation(LinearLayout.VERTICAL);
            root.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

            LinearLayout bar = new LinearLayout(activity);
            bar.setOrientation(LinearLayout.HORIZONTAL);
            bar.setGravity(Gravity.CENTER_VERTICAL);
            bar.setPadding(dp(activity, 8), dp(activity, 8), dp(activity, 12), dp(activity, 8));
            bar.setBackgroundColor(Theme.getColor(Theme.key_actionBarDefault));

            TextView back = new TextView(activity);
            back.setText("‹");
            back.setTextSize(34);
            back.setGravity(Gravity.CENTER);
            back.setTextColor(Theme.getColor(Theme.key_actionBarDefaultIcon));
            back.setContentDescription("Назад");
            back.setOnClickListener(v -> dialog.dismiss());
            bar.addView(back, new LinearLayout.LayoutParams(dp(activity, 48), dp(activity, 48)));

            TextView title = new TextView(activity);
            title.setText(LocaleController.getString("TMoeSettings", R.string.TMoeSettings));
            title.setTextSize(20);
            title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            title.setTextColor(Theme.getColor(Theme.key_actionBarDefaultTitle));
            title.setGravity(Gravity.CENTER_VERTICAL);
            bar.addView(title, new LinearLayout.LayoutParams(0, dp(activity, 48), 1f));
            root.addView(bar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            final ThemedRecyclerListView list = new ThemedRecyclerListView(activity);
            list.setItemAnimator(null);
            list.setLayoutAnimation(null);
            list.setLayoutManager(new LinearLayoutManager(activity, RecyclerView.VERTICAL, false));
            list.setVerticalScrollBarEnabled(false);

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
                    RecyclerView.ViewHolder vh = delegate.createViewHolder(activity, parent);
                    if (!delegate.isVoidBackground()) {
                        vh.itemView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    }
                    return vh;
                }

                @Override
                public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
                    items.get(position).bindView(holder, position, activity);
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
            listFrame.addView(list, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            root.addView(listFrame, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

            dialog.setContentView(root);
            Window w = dialog.getWindow();
            if (w != null) {
                w.setBackgroundDrawable(new ColorDrawable(Theme.getColor(Theme.key_windowBackgroundGray)));
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
