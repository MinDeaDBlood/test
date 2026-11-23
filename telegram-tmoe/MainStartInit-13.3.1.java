package cc.ioctl.tmoe.base;

import android.app.Application;

import cc.ioctl.tmoe.compat.Telegram13ClassResolver;
import cc.ioctl.tmoe.hook.func.DatabaseCorruptionWarning;
import cc.ioctl.tmoe.lifecycle.Parasitics;
import cc.ioctl.tmoe.rtti.ProxyFragmentRttiHandler;
import cc.ioctl.tmoe.rtti.deobf.ClassLocator;
import cc.ioctl.tmoe.util.HostInfo;
import cc.ioctl.tmoe.util.Initiator;
import cc.ioctl.tmoe.util.MultiProcess;
import cc.ioctl.tmoe.util.Utils;

public class MainStartInit {
    public static final MainStartInit INSTANCE = new MainStartInit();

    private MainStartInit() {}

    private boolean mPreInitialized = false;
    private boolean mPostInitialized = false;

    public void initForPreStartup() {
        if (mPreInitialized) return;
        DynamicHookInit.allowEarlyInit(DatabaseCorruptionWarning.INSTANCE);
        mPreInitialized = true;
    }

    public void initForPostStartup() {
        if (mPostInitialized) return;
        Application app = HostInfo.getApplication();
        if (MultiProcess.isMainProcess()) {
            // Telegram 13.x release builds R8-obfuscate BaseFragment and its fields.
            // Proxy-fragment support is optional now: failure here must not block all TMoe hooks.
            try {
                findHostBaseFragmentAndInitForProxy();
            } catch (Throwable e) {
                Utils.logw("TMoe: proxy fragment unavailable on this Telegram build, using native settings fallback");
                Utils.loge(e);
            }

            try {
                Parasitics.injectModuleResources(app.getApplicationContext().getResources());
            } catch (Throwable e) {
                Utils.loge(e);
            }
            try {
                Parasitics.initForStubActivity(app);
            } catch (Throwable e) {
                Utils.loge(e);
            }

            // Resolve the R8 host-class graph before legacy hooks initialize.
            // This is structural (ProfileActivity/ChatActivityEnterView/BaseFragment getters)
            // and therefore does not depend on short R8 names.
            Telegram13ClassResolver.warmUp();

            // Always load functional/core hooks even if the old fragment RTTI layer failed.
            DynamicHookInit.loadHooks();
        }
        mPostInitialized = true;
        initForClassLocator();
    }

    private static void findHostBaseFragmentAndInitForProxy() throws ReflectiveOperationException {
        Class<?> kBaseFragment = Initiator.load("org.telegram.ui.ActionBar.BaseFragment");

        // Official Telegram 13.x keeps ProfileActivity readable while its BaseFragment is renamed.
        if (kBaseFragment == null) {
            Class<?> profile = Initiator.load("org.telegram.ui.ProfileActivity");
            if (profile != null) {
                Class<?> parent = profile.getSuperclass();
                if (parent != null && parent != Object.class) {
                    kBaseFragment = parent;
                    Utils.logd("TMoe: BaseFragment resolved from ProfileActivity: " + parent.getName());
                }
            }
        }

        if (kBaseFragment == null) {
            throw new ClassNotFoundException("Telegram BaseFragment");
        }

        // This can still fail on builds where BaseFragment fields/methods were renamed.
        // NativeSettingsDialog does not depend on this layer.
        ProxyFragmentRttiHandler.initProxyFragmentClass(kBaseFragment);
    }

    private static void initForClassLocator() {
        Class<?> kTheme = ClassLocator.getThemeClass();
        if (kTheme == null) {
            Utils.loge("can not find class Theme");
            Utils.async(() -> {
                Class<?> k = ClassLocator.findThemeClass();
                if (k != null) {
                    Utils.logd("find class Theme: " + k.getName());
                } else {
                    Utils.loge("can not find class Theme");
                }
            });
        }
    }
}
