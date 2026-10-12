package com.example.dhanovaai;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Toast;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SessionRouter {

    public static void routeAfterAuth(Context context) {
        RetrofitClient.getApiService(context).getMyFamilies().enqueue(new Callback<MyFamiliesResponse>() {
            @Override
            public void onResponse(Call<MyFamiliesResponse> call, Response<MyFamiliesResponse> response) {
                Intent intent;

                if (response.isSuccessful() && response.body() != null
                        && response.body().families != null && !response.body().families.isEmpty()) {

                    FamilyInfo family = response.body().families.get(0); // 1 member = 1 family, so first is the only one

                    SharedPreferences prefs = context.getSharedPreferences("dhanova_prefs", Context.MODE_PRIVATE);
                    prefs.edit()
                            .putInt("current_family_id", family.family_id)
                            .putString("current_family_name", family.family_name)
                            .putString("current_family_role", family.role)
                            .apply();

                    intent = new Intent(context, DashboardActivity.class);
                } else {
                    // No family yet — send to Welcome to Create/Join
                    intent = new Intent(context, WelcomeActivity.class);
                }

                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }

            @Override
            public void onFailure(Call<MyFamiliesResponse> call, Throwable t) {
                Toast.makeText(context, "Could not check family status: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                // Fall back to Welcome screen rather than leaving the user stuck
                Intent intent = new Intent(context, WelcomeActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        });
    }
}