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
                    Rect disp = (Rect) a[3];

                    Object info = XposedHelpers.callMethod(p.thisObject, "getDisplayDeviceInfoLocked");
                    if (info == null) return;
                    if (XposedHelpers.getIntField(info, "type") != 1) return;

                    int w = XposedHelpers.getIntField(info, "width");
                    int h = XposedHelpers.getIntField(info, "height");

                    XposedBridge.log("Stretch43: orient=" + orientation + " layer=" + layer
                            + " disp=" + disp + " panel=" + w + "x" + h);

                    int[][] cands = { {w, h}, {h, w} };
                    for (int[] c : cands) {
                        boolean pillar = disp.left > 0 && disp.left + disp.right == c[0]
                                && disp.top == 0 && disp.bottom == c[1];
                        boolean letter = disp.top > 0 && disp.top + disp.bottom == c[1]
                                && disp.left == 0 && disp.right == c[0];
                        if (pillar || letter) {
                            a[3] = new Rect(0, 0, c[0], c[1]);
                            XposedBridge.log("Stretch43: stretched to " + c[0] + "x" + c[1]);
                            return;
                        }
                    }
                } catch (Throwable t) {
                    XposedBridge.log("Stretch43: " + t);
                }
            }
        });
    }
}
