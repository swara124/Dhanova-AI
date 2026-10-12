package com.example.dhanovaai;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Holds the current user's role + permissions for the current family.
 * Methods used by the app: load(), has(), isAdmin(), clear().
 */
public class PermissionsManager {

    private static boolean admin = false;
    private static final Set<String> permissions = new HashSet<>();

    /** Fetches role + permissions from the server, then runs onDone (always, even on failure). */
    public static void load(Context context, Runnable onDone) {
        Context appContext = context.getApplicationContext();
        SharedPreferences prefs = appContext.getSharedPreferences("dhanova_prefs", Context.MODE_PRIVATE);
        int familyId = prefs.getInt("current_family_id", -1);
        int userId = prefs.getInt("user_id", -1);

        if (familyId == -1 || userId == -1) {
            clear();
            if (onDone != null) onDone.run();
            return;
        }

        RetrofitClient.getApiService(appContext)
                .getMemberPermissions(familyId, userId)
                .enqueue(new Callback<PermissionsResponse>() {
                    @Override
                    public void onResponse(Call<PermissionsResponse> call,
                                           Response<PermissionsResponse> response) {
                        permissions.clear();
                        if (response.isSuccessful() && response.body() != null) {
                            admin = "ADMIN".equals(response.body().role);
                            if (response.body().permissions != null) {
                                permissions.addAll(response.body().permissions);
                            }
                        } else {
                            // Fall back to the role saved at login
                            admin = "ADMIN".equals(prefs.getString("current_family_role", ""));
                        }
                        if (onDone != null) onDone.run();
                    }

                    @Override
                    public void onFailure(Call<PermissionsResponse> call, Throwable t) {
                        permissions.clear();
                        admin = "ADMIN".equals(prefs.getString("current_family_role", ""));
                        if (onDone != null) onDone.run();
                    }
                });
    }

    public static boolean isAdmin() {
        return admin;
    }

    public static boolean has(String permission) {
        return admin || permissions.contains(permission);
    }

    public static void clear() {
        admin = false;
        permissions.clear();
    }
}