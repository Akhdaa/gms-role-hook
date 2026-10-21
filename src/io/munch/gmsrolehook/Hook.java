package io.munch.gmsrolehook;

import android.content.Context;
import android.os.Binder;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

/**
 * Runs inside system_server (scope "android"); nothing is injected into Google Play services.
 *
 * GMS 26.40.x calls RoleManager.getRoleHolders(), but the updated GMS cannot be granted
 * GET_ROLE_HOLDERS, so RoleService.getRoleHoldersAsUser throws SecurityException and sign-in fails.
 * Skip that single permission check when the caller is GMS and the required permissions are the
 * role-holder ones. Every other caller and every other permission is checked as usual.
 */
public class Hook implements IXposedHookLoadPackage {
    private static final String TAG = "RoleHolderHook: ";
    private static final String GMS = "com.google.android.gms";
    private static final String ROLE_STUB = "com.android.role.RoleService$Stub";
    private static final String GET_PERM = "android.permission.GET_ROLE_HOLDERS";
    private static final String OBSERVE_PERM = "android.permission.OBSERVE_ROLE_HOLDERS";

    private static volatile int gmsAppId = -1;
    private static volatile int logged;

    @Override
    public void handleLoadPackage(LoadPackageParam lp) {
        if (!"android".equals(lp.packageName)) return;
        try {
            Class<?> stub = Class.forName(ROLE_STUB, false, lp.classLoader);
            int n = XposedBridge.hookAllMethods(stub, "enforceCallingOrSelfAnyPermissions", new Allow()).size();
            XposedBridge.log(TAG + "hooked " + n + " method(s) in " + lp.processName);
        } catch (Throwable t) {
            XposedBridge.log(TAG + "not hooked: " + t);
        }
    }

    private static final class Allow extends XC_MethodHook {
        @Override
        protected void beforeHookedMethod(MethodHookParam p) {
            try {
                if (!wantsRoleHolders(p.args)) return;
                if (Binder.getCallingUid() % 100000 != gmsAppId()) return;
                if (logged++ < 3) XposedBridge.log(TAG + "allowed GMS: " + describe(p.args));
                p.setResult(null);
            } catch (Throwable ignored) {
                // any failure: leave the original permission check untouched
            }
        }
    }

    private static boolean wantsRoleHolders(Object[] args) {
        if (args == null) return false;
        for (Object a : args) {
            if (a instanceof String[]) {
                for (String perm : (String[]) a) {
                    if (GET_PERM.equals(perm) || OBSERVE_PERM.equals(perm)) return true;
                }
            }
        }
        return false;
    }

    private static String describe(Object[] args) {
        for (Object a : args) {
            if (a instanceof String) return (String) a;
        }
        return "?";
    }

    private static int gmsAppId() throws Exception {
        int id = gmsAppId;
        if (id >= 0) return id;
        long token = Binder.clearCallingIdentity();
        try {
            Class<?> at = Class.forName("android.app.ActivityThread");
            Object thread = at.getMethod("currentActivityThread").invoke(null);
            Context ctx = (Context) at.getMethod("getSystemContext").invoke(thread);
            id = ctx.getPackageManager().getPackageUid(GMS, 0) % 100000;
            gmsAppId = id;
            return id;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }
}
