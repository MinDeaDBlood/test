package cc.ioctl.tmoe.startup;

import android.app.Application;
import android.widget.Toast;

import java.lang.reflect.Method;

import cc.ioctl.tmoe.hook.core.SettingEntryHook;
import cc.ioctl.tmoe.util.HostInfo;
import cc.ioctl.tmoe.util.Initiator;
import cc.ioctl.tmoe.util.Utils;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Telegram 13.x bootstrap that does not depend on TMoe's legacy startup chain.
 *
 * Telegram 13.3.1 release APK R8-renames ApplicationLoader to an obfuscated
 * superclass. The manifest still points at ApplicationLoaderImpl, so resolve
 * the actual onCreate implementation by walking that class hierarchy instead
 * of depending on the old org.telegram.messenger.ApplicationLoader name.
 */
public final class Telegram13Bootstrap {

    private static volatile boolean sInstalled = false;
    private static volatile boolean sRan = false;

    private Telegram13Bootstrap() {}

    private static Method resolveApplicationOnCreate(ClassLoader classLoader) throws Throwable {
        Class<?> appClass = null;

        // Older/non-obfuscated Telegram builds.
        try {
            appClass = classLoader.loadClass("org.telegram.messenger.ApplicationLoader");
        } catch (ClassNotFoundException ignored) {
        }

        // Telegram 13.3.1 official standalone APK:
        // ApplicationLoaderImpl -> R8-obfuscated Application subclass (s0) -> Application.
        if (appClass == null) {
            appClass = classLoader.loadClass("org.telegram.messenger.ApplicationLoaderImpl");
        }

        for (Class<?> c = appClass; c != null && c != Application.class; c = c.getSuperclass()) {
            try {
                Method method = c.getDeclaredMethod("onCreate");
                if (method.getParameterTypes().length == 0 && method.getReturnType() == void.class) {
                    method.setAccessible(true);
                    return method;
                }
            } catch (NoSuchMethodException ignored) {
            }
        }

        throw new NoSuchMethodException(
                "Unable to resolve Telegram Application.onCreate from " + appClass.getName());
    }

    public static boolean install(XC_LoadPackage.LoadPackageParam lpparam) {
        if (sInstalled) return true;
        try {
            final Method onCreate = resolveApplicationOnCreate(lpparam.classLoader);
            XposedBridge.log(
                    "TMoe: Telegram13Bootstrap resolved "
                            + onCreate.getDeclaringClass().getName()
                            + "#onCreate");

            XposedBridge.hookMethod(onCreate, new XC_MethodHook(10000) {
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
                    String preError = null;
                    String postError = null;
                    String settingsError = null;

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
                        preError = e.getClass().getSimpleName() + ": " + String.valueOf(e.getMessage());
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
                        settingsError = e.getClass().getSimpleName() + ": " + String.valueOf(e.getMessage());
                        Utils.loge(e);
                    }

                    final String settingsStatus = SettingEntryHook.getDiagnosticStatus();
                    final String msg =
                            "TMoe bootstrap\n"
                            + "hook: " + onCreate.getDeclaringClass().getName() + "#onCreate\n"
                            + "loader: OK\n"
                            + "native: " + (nativeOk ? "OK" : "FAIL") + "\n"
                            + "pre: " + (preOk ? "OK" : "FAIL") + "\n"
                            + "post: " + (postOk ? "OK" : "FAIL") + "\n"
                            + "settings: " + (settingsInitOk ? settingsStatus : "INIT FAIL")
                            + (nativeError == null ? "" : "\nnative err: " + nativeError)
                            + (preError == null ? "" : "\npre err: " + preError)
                            + (postError == null ? "" : "\npost err: " + postError)
                            + (settingsError == null ? "" : "\nsettings err: " + settingsError);

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
