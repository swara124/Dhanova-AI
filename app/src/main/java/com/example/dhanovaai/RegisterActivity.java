package com.example.dhanovaai;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    EditText etName, etEmail, etPassword, etSecretCode, etConfirmSecretCode;
    Button btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etName = findViewById(R.id.et_name);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        etSecretCode = findViewById(R.id.et_secret_code);
        etConfirmSecretCode = findViewById(R.id.et_confirm_secret_code);
        btnRegister = findViewById(R.id.btn_register);

        btnRegister.setOnClickListener(v -> attemptRegister());
    }

    private void attemptRegister() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String secretCode = etSecretCode.getText().toString().trim();
        String confirmSecretCode = etConfirmSecretCode.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || secretCode.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }
        if (secretCode.length() < 4) {
            Toast.makeText(this, "Secret code must be at least 4 digits", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!secretCode.equals(confirmSecretCode)) {
            Toast.makeText(this, "Secret codes do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        btnRegister.setEnabled(false);

        RegisterRequest request = new RegisterRequest(name, email, password, secretCode);

        RetrofitClient.getApiService(this).registerUser(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                btnRegister.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    String token = response.body().token;

                    SharedPreferences prefs = getSharedPreferences("dhanova_prefs", MODE_PRIVATE);
                    prefs.edit()
                            .putString("auth_token", token)
                            .putInt("user_id", response.body().user.user_id)
                            .putString("user_name", response.body().user.name)
                            .putString("last_user_email", response.body().user.email) // remembers device owner
                            .apply();

                    Toast.makeText(RegisterActivity.this, "Registered successfully!", Toast.LENGTH_SHORT).show();

                    SessionRouter.routeAfterAuth(RegisterActivity.this);
                    finish();

                } else {
                    Toast.makeText(RegisterActivity.this, "Registration failed. Try a different email.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                btnRegister.setEnabled(true);
                Toast.makeText(RegisterActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}