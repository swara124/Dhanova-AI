package com.example.dhanovaai;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EnterPinActivity extends AppCompatActivity {

    EditText etPin;
    Button btnContinue;
    TextView tvUserEmail, tvUsePasswordInstead;
    String savedEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enter_pin);

        etPin = findViewById(R.id.et_pin);
        btnContinue = findViewById(R.id.btn_continue);
        tvUserEmail = findViewById(R.id.tv_pin_user_email);
        tvUsePasswordInstead = findViewById(R.id.tv_use_password_instead);

        SharedPreferences prefs = getSharedPreferences("dhanova_prefs", MODE_PRIVATE);
        savedEmail = prefs.getString("last_user_email", null);
        tvUserEmail.setText(savedEmail != null ? savedEmail : "");

        btnContinue.setOnClickListener(v -> attemptVerifyPin());

        tvUsePasswordInstead.setOnClickListener(v -> {
            startActivity(new Intent(EnterPinActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void attemptVerifyPin() {
        String code = etPin.getText().toString().trim();

        if (code.isEmpty()) {
            Toast.makeText(this, "Enter your secret code", Toast.LENGTH_SHORT).show();
            return;
        }

        btnContinue.setEnabled(false);

        PinRequest request = new PinRequest(savedEmail, code);

        RetrofitClient.getApiService(this).verifyPin(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                btnContinue.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    String token = response.body().token;

                    SharedPreferences prefs = getSharedPreferences("dhanova_prefs", MODE_PRIVATE);
                    prefs.edit()
                            .putString("auth_token", token)
                            .putInt("user_id", response.body().user.user_id)
                            .putString("user_name", response.body().user.name)
                            .apply();

                    SessionRouter.routeAfterAuth(EnterPinActivity.this);
                    finish();

                } else {
                    Toast.makeText(EnterPinActivity.this, "Incorrect secret code", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                btnContinue.setEnabled(true);
                Toast.makeText(EnterPinActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}