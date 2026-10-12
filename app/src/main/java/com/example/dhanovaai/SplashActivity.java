package com.example.dhanovaai;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY_MS = 1800;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler().postDelayed(() -> {
            SharedPreferences prefs = getSharedPreferences("dhanova_prefs", MODE_PRIVATE);
            String lastUserEmail = prefs.getString("last_user_email", null);

            Intent intent;
            if (lastUserEmail != null) {
                // This device has a remembered user — ask for their PIN
                intent = new Intent(SplashActivity.this, EnterPinActivity.class);
            } else {
                // Never logged in on this device — go to normal Login (has Register link)
                intent = new Intent(SplashActivity.this, LoginActivity.class);
            }
            startActivity(intent);
            finish();
        }, SPLASH_DELAY_MS);
    }
}