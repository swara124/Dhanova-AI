package com.example.dhanovaai;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddMemberActivity extends AppCompatActivity {

    EditText etEmail;
    Button btnInvite;
    int familyId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_member);

        familyId = getIntent().getIntExtra("family_id", -1);

        etEmail = findViewById(R.id.et_invite_email);
        btnInvite = findViewById(R.id.btn_invite);

        btnInvite.setOnClickListener(v -> sendInvite());
    }

    private void sendInvite() {
        String email = etEmail.getText().toString().trim();
        if (email.isEmpty()) {
            Toast.makeText(this, "Enter an email address", Toast.LENGTH_SHORT).show();
            return;
        }
        if (familyId == -1) {
            Toast.makeText(this, "Missing family info — go back and try again", Toast.LENGTH_SHORT).show();
            return;
        }

        btnInvite.setEnabled(false);
        InviteRequest request = new InviteRequest(familyId, email);

        RetrofitClient.getApiService(this).inviteMember(request).enqueue(new Callback<SimpleMessageResponse>() {
            @Override
            public void onResponse(Call<SimpleMessageResponse> call, Response<SimpleMessageResponse> response) {
                btnInvite.setEnabled(true);
                if (response.isSuccessful()) {
                    Toast.makeText(AddMemberActivity.this, "Invitation sent!", Toast.LENGTH_SHORT).show();
                    etEmail.setText("");
                } else {
                    String err = response.body() != null && response.body().error != null
                            ? response.body().error : "Could not send invitation";
                    Toast.makeText(AddMemberActivity.this, err, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<SimpleMessageResponse> call, Throwable t) {
                btnInvite.setEnabled(true);
                Toast.makeText(AddMemberActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}