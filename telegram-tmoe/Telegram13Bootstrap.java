package cc.ioctl.tmoe.startup;

import android.app.Application;
import android.widget.Toast;

import cc.ioctl.tmoe.hook.core.SettingEntryHook;
import cc.ioctl.tmoe.util.HostInfo;
import cc.ioctl.tmoe.util.Initiator;
import cc.ioctl.tmoe.util.Utils;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Telegram 13.x bootstrap that does not depend on TMoe's legacy startup chain.
 * It is intentionally verbose/visible for one diagnostic build.
 */
public final class Telegram13Bootstrap {

    private static volatile boolean sInstalled = false;
    private static volatile boolean sRan = false;

    private Telegram13Bootstrap() {}

    public static boolean install(XC_LoadPackage.LoadPackageParam lpparam) {
        if (sInstalled) return true;
        try {
            Class<?> appClass = lpparam.classLoader.loadClass("org.telegram.messenger.ApplicationLoader");
            XposedBridge.hookAllMethods(appClass, "onCreate", new XC_MethodHook(10000) {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (sRan || !(param.thisObject instanceof Application)) return;
                    Application app = (Application) param.thisObject;
                    try {
                        HostInfo.setHostApplication(app);
                        Initiator.initWithHostClassLoader(app.getClassLoader());
                    } catch (Throwable e) {
                        Utils.loge(e);
                    }
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (sRan || !(param.thisObject instanceof Application)) return;
                    sRan = true;
                    Application app = (Application) param.thisObject;

                    boolean nativeOk = false;
                    boolean preOk = false;
                    boolean postOk = false;
                    boolean settingsInitOk = false;
                    String nativeError = null;
                    String postError = null;

                    try {
                        NativeLoader.loadAllSharedLibraries(app);
                        nativeOk = NativeLoader.isNativeLibraryLoaded();
                    } catch (Throwable e) {
                        nativeError = e.getClass().getSimpleName() + ": " + String.valueOf(e.getMessage());
                        Utils.loge(e);
                    }

                    try {
                        StartupRoutine.execPreStartupInit(app, null, false);
                        preOk = true;
                    } catch (Throwable e) {
                        Utils.loge(e);
                    }

                    if (nativeOk && preOk) {
                        try {
                            StartupRoutine.execPostStartupInit();
                            postOk = true;
                        } catch (Throwable e) {
                            postError = e.getClass().getSimpleName() + ": " + String.valueOf(e.getMessage());
                            Utils.loge(e);
                        }
                    }

                    try {
                        SettingEntryHook.INSTANCE.initialize();
                        settingsInitOk = true;
                    } catch (Throwable e) {
                        Utils.loge(e);
                    }

                    final String settingsStatus = SettingEntryHook.getDiagnosticStatus();
                    final String msg =
                            "TMoe bootstrap\n"
                            + "loader: OK\n"
                            + "native: " + (nativeOk ? "OK" : "FAIL") + "\n"
                            + "post: " + (postOk ? "OK" : "FAIL") + "\n"
                            + "settings: " + (settingsInitOk ? settingsStatus : "INIT FAIL")
                            + (nativeError == null ? "" : "\nnative err: " + nativeError)
                            + (postError == null ? "" : "\npost err: " + postError);

                    try {
                        Toast.makeText(app, msg, Toast.LENGTH_LONG).show();
                    } catch (Throwable e) {
                        Utils.loge(e);
                    }
                }
            });
            sInstalled = true;
            XposedBridge.log("TMoe: Telegram13Bootstrap installed for " + lpparam.packageName);
            return true;
        } catch (Throwable e) {
            XposedBridge.log(e);
            return false;
        }
    }
}
