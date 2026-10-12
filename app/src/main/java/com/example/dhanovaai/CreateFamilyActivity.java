package com.example.dhanovaai;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateFamilyActivity extends AppCompatActivity {

    EditText etFamilyName;
    Button btnSubmit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_family);

        etFamilyName = findViewById(R.id.et_family_name);
        btnSubmit = findViewById(R.id.btn_create_family_submit);

        btnSubmit.setOnClickListener(v -> createFamily());
    }

    private void createFamily() {
        String name = etFamilyName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "Enter a family name", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSubmit.setEnabled(false);
        CreateFamilyRequest request = new CreateFamilyRequest(name, new ArrayList<>());

        RetrofitClient.getApiService(this).createFamily(request).enqueue(new Callback<CreateFamilyResponse>() {
            @Override
            public void onResponse(Call<CreateFamilyResponse> call, Response<CreateFamilyResponse> response) {
                btnSubmit.setEnabled(true);
                if (response.isSuccessful() && response.body() != null) {
                    int familyId = response.body().family_id;

                    SharedPreferences prefs = getSharedPreferences("dhanova_prefs", MODE_PRIVATE);
                    prefs.edit().putInt("current_family_id", familyId).apply();

                    Toast.makeText(CreateFamilyActivity.this, "Family created!", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(CreateFamilyActivity.this, FamilyMembersActivity.class);
                    intent.putExtra("family_id", familyId);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(CreateFamilyActivity.this, "Could not create family. You may already belong to one.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<CreateFamilyResponse> call, Throwable t) {
                btnSubmit.setEnabled(true);
                Toast.makeText(CreateFamilyActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}