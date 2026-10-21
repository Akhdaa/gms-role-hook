package io.xxru.gmsrolehook;

import android.app.role.RoleManager;

import java.util.ArrayList;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

/**
 * GMS 26.40 calls RoleManager.getRoleHolders() during OAuth token fetch, but the ROM's
 * system-image GMS never requested GET_ROLE_HOLDERS, so an updated GMS cannot be granted it.
 * system_server then throws SecurityException and GMS reports AUTH_SECURITY_ERROR.
 * Swallow that SecurityException inside GMS and return "no holders" instead.
 */
public class Hook implements IXposedHookLoadPackage {
    private static final String GMS = "com.google.android.gms";
    private static final String TAG = "GmsRoleHook: ";

    @Override
    public void handleLoadPackage(LoadPackageParam lp) {
        if (!GMS.equals(lp.packageName)) return;

        hook("getRoleHolders", new ListCb());
        hook("getRoleHoldersAsUser", new ListCb());
        hook("addOnRoleHoldersChangedListenerAsUser", new VoidCb());
        XposedBridge.log(TAG + "ready in " + lp.processName);
    }

    private static void hook(String method, XC_MethodHook cb) {
        try {
            int n = XposedBridge.hookAllMethods(RoleManager.class, method, cb).size();
            if (n == 0) XposedBridge.log(TAG + "no method named " + method);
        } catch (Throwable t) {
            XposedBridge.log(TAG + "hook failed " + method + ": " + t);
        }
    }

    /** getRoleHolders*: List<String> -> empty list on SecurityException. */
    private static final class ListCb extends XC_MethodHook {
        @Override
        protected void afterHookedMethod(MethodHookParam p) {
            if (p.getThrowable() instanceof SecurityException) {
                XposedBridge.log(TAG + "swallowed: " + p.getThrowable().getMessage());
                p.setResult(new ArrayList<String>());
            }
        }
    }

    /** addOnRoleHoldersChangedListener*: void -> no-op on SecurityException. */
    private static final class VoidCb extends XC_MethodHook {
        @Override
        protected void afterHookedMethod(MethodHookParam p) {
            if (p.getThrowable() instanceof SecurityException) {
                XposedBridge.log(TAG + "swallowed (listener): " + p.getThrowable().getMessage());
                p.setResult(null);
            }
        }
    }
}
