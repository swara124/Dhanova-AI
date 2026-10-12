package com.example.dhanovaai;

import android.app.Activity;

import androidx.appcompat.app.AlertDialog;

/**
 * One place that decides "allowed -> run the action" or "not allowed -> popup".
 * Use it for EVERY permission-protected tap so behaviour is identical everywhere.
 *
 *   PermissionGuard.require(this, "ADD_TRANSACTIONS", "add family transactions", () -> openAddScreen());
 */
public final class PermissionGuard {

    private PermissionGuard() {}

    /** Runs onAllowed if the current user holds the permission (admins always do), else shows the popup. */
    public static void require(Activity activity, String permission, String actionLabel, Runnable onAllowed) {
        if (PermissionsManager.has(permission)) {
            onAllowed.run();
        } else {
            showDenied(activity, actionLabel);
        }
    }

    /** For actions that only the real admin may do (e.g. transferring the admin role). */
    public static void requireAdmin(Activity activity, String actionLabel, Runnable onAllowed) {
        if (PermissionsManager.isAdmin()) {
            onAllowed.run();
        } else {
            showDenied(activity, actionLabel);
        }
    }

    public static void showDenied(Activity activity, String actionLabel) {
        if (activity.isFinishing()) return;
        new AlertDialog.Builder(activity)
                .setTitle("Not permitted")
                .setMessage("You are not permitted to " + actionLabel
                        + ".\n\nPlease ask your family admin to grant you this permission.")
                .setPositiveButton("OK", null)
                .show();
    }
}