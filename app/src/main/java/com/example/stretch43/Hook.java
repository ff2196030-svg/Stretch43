package com.example.stretch43;

import android.graphics.Rect;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class Hook implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpp) throws Throwable {
        if (!"android".equals(lpp.packageName)) return;

        Class<?> dd = XposedHelpers.findClass(
                "com.android.server.display.DisplayDevice", lpp.classLoader);

        XposedBridge.hookAllMethods(dd, "setProjectionLocked", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam p) {
                try {
                    Object[] a = p.args;
                    if (a == null || a.length < 4) return;
                    if (!(a[1] instanceof Integer) || !(a[2] instanceof Rect) || !(a[3] instanceof Rect)) return;

                    int orientation = (Integer) a[1];
                    Rect layer = (Rect) a[2];

                    Object info = XposedHelpers.callMethod(p.thisObject, "getDisplayDeviceInfoLocked");
                    if (info == null) return;

                    int type = XposedHelpers.getIntField(info, "type");
                    if (type != 1) return;

                    int w = XposedHelpers.getIntField(info, "width");
                    int h = XposedHelpers.getIntField(info, "height");
                    boolean rotated = (orientation == 1 || orientation == 3);
                    int pw = rotated ? h : w;
                    int ph = rotated ? w : h;

                    if (layer.width() <= 0 || layer.height() <= 0) return;

                    if ((long) layer.width() * ph != (long) layer.height() * pw) {
                        a[3] = new Rect(0, 0, pw, ph);
                    }
                } catch (Throwable t) {
                    XposedBridge.log("Stretch43: " + t);
                }
            }
        });
    }
}
