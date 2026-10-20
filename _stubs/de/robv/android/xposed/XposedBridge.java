package de.robv.android.xposed;

import java.util.Set;

/** Compile-only stub. Signatures verified against the Vector v2.2 framework dex on the phone. */
public final class XposedBridge {
    private XposedBridge() {}

    public static Set<XC_MethodHook.Unhook> hookAllMethods(Class<?> hookClass, String methodName, XC_MethodHook callback) {
        return null;
    }

    public static void log(String text) {}

    public static void log(Throwable t) {}
}
