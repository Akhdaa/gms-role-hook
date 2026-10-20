package de.robv.android.xposed.callbacks;

import android.content.pm.ApplicationInfo;

/** Compile-only stub. Fields verified against the Vector v2.2 framework dex on the phone. */
public abstract class XC_LoadPackage {
    public static class LoadPackageParam {
        public String packageName;
        public String processName;
        public ClassLoader classLoader;
        public ApplicationInfo appInfo;
        public boolean isFirstApplication;
    }
}
