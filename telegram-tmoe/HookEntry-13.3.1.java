package cc.ioctl.tmoe.startup;

import com.github.kyuubiran.ezxhelper.init.EzXHelperInit;

import java.util.List;

import cc.ioctl.tmoe.R;
import cc.ioctl.tmoe.util.Telegram13ClassLoader;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookEntry implements IXposedHookLoadPackage, IXposedHookZygoteInit {

    private static final List<String> TELEGRAM_CLIENT_PACKAGE_NAME_LIST = List.of(
            "org.telegram.messenger",
            "org.telegram.messenger.beta",
            "org.telegram.plus",
            "nekox.messenger",
            "com.jasonkhew96.pigeongram",
            "app.nicegram",
            "xyz.nextalone.nagram",
            "xyz.nextalone.nnngram",
            "com.xtaolabs.pagergram",
            "org.telegram.messenger.web",
            "com.cool2645.nekolite",
            "com.iMe.android",
            "org.telegram.BifToGram",
            "ua.itaysonlab.messenger",
            "org.forkclient.messenger.beta",
            "org.aka.messenger",
            "ellipi.messenger",
            "me.luvletter.nekox",
            "org.nift4.catox",
            "icu.ketal.yunigram",
            "icu.ketal.yunigram.lspatch",
            "icu.ketal.yunigram.beta",
            "icu.ketal.yunigram.lspatch.beta",
            "org.forkgram.messenger",
            "org.forkgram.classic",
            "com.blxueya.gugugram",
            "com.radolyn.ayugram",
            "com.blxueya.gugugramx",
            "com.evildayz.code.telegraher",
            "com.exteragram.messenger"
    );

    private static String sModulePath = null;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (R.string.res_inject_success >>> 24 == 0x7f) {
            XposedBridge.log("package id must NOT be 0x7f, reject loading...");
            return;
        }

        String packageName = lpparam.packageName;
        Class<?> kXposedBridge = XposedBridge.class;
        if (!"de.robv.android.xposed.XposedBridge".equals(kXposedBridge.getName())) {
            String className = kXposedBridge.getName();
            String pkgName = className.substring(0, className.lastIndexOf('.'));
            HybridClassLoader.setObfuscatedXposedApiPackage(pkgName);
        }

        if (!TELEGRAM_CLIENT_PACKAGE_NAME_LIST.contains(packageName)) {
            return;
        }

        // Initialize EzXHelper before functional hooks are installed. For the
        // telegram.org R8 build replace its host loader with our compatibility
        // loader, so old TMoe source names resolve to the real 13.3.1 classes.
        EzXHelperInit.INSTANCE.initHandleLoadPackage(lpparam);
        if ("org.telegram.messenger.web".equals(packageName)) {
            Telegram13ClassLoader compat =
                    Telegram13ClassLoader.getOrCreate(lpparam.classLoader);
            EzXHelperInit.INSTANCE.setEzClassLoader(compat);
            XposedBridge.log("TMoe: installed Telegram 13 compatibility ClassLoader");
            Telegram13Bootstrap.install(lpparam);
        } else {
            StartupHook.INSTANCE.doInit(lpparam.classLoader);
        }

        EzXHelperInit.INSTANCE.setLogTag("TMoe");
    }

    @Override
    public void initZygote(StartupParam startupParam) {
        try {
            EzXHelperInit.INSTANCE.initZygote(startupParam);
        } finally {
            sModulePath = startupParam.modulePath;
            XposedBridge.log("TMoe: modulePath=" + sModulePath);
        }
    }

    public static String getModulePath() {
        String path = sModulePath;
        if (path == null) {
            throw new IllegalStateException("sModulePath is null");
        }
        return path;
    }
}
