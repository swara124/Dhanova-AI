package com.example.dhanovaai;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FamilyMembersActivity extends AppCompatActivity {

    LinearLayout container;
    int familyId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_family_members);

        familyId = getIntent().getIntExtra("family_id", -1);
        container = findViewById(R.id.container_members);

        findViewById(R.id.btn_invite_member).setOnClickListener(v -> {
            Intent intent = new Intent(FamilyMembersActivity.this, AddMemberActivity.class);
            intent.putExtra("family_id", familyId);
            startActivity(intent);
        });

        loadMembers();
    }

    private void loadMembers() {
        if (familyId == -1) {
            Toast.makeText(this, "Missing family info", Toast.LENGTH_SHORT).show();
            return;
        }

        RetrofitClient.getApiService(this).getFamilyMembers(familyId).enqueue(new Callback<MembersResponse>() {
            @Override
            public void onResponse(Call<MembersResponse> call, Response<MembersResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    renderMembers(response.body().members);
                } else {
                    Toast.makeText(FamilyMembersActivity.this, "Could not load members", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<MembersResponse> call, Throwable t) {
                Toast.makeText(FamilyMembersActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void renderMembers(List<FamilyMember> members) {
        container.removeAllViews();

        for (FamilyMember member : members) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(24, 20, 24, 20);
            row.setBackgroundColor(Color.parseColor("#F5F5F5"));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, 12);
            row.setLayoutParams(params);

            TextView tvName = new TextView(this);
            tvName.setText(member.name + "  (" + member.role + ")");
            tvName.setTextSize(16);
            tvName.setTypeface(null, android.graphics.Typeface.BOLD);

            TextView tvEmail = new TextView(this);
            tvEmail.setText(member.email);
            tvEmail.setTextColor(Color.parseColor("#777777"));

            row.addView(tvName);
            row.addView(tvEmail);

            // Admin can tap a MEMBER row to manage their permissions
            if (!member.role.equals("ADMIN")) {
                Button btnPermissions = new Button(this);
                btnPermissions.setText("Manage Permissions");
                btnPermissions.setOnClickListener(v -> {
                    Intent intent = new Intent(FamilyMembersActivity.this, MemberPermissionsActivity.class);
                    intent.putExtra("family_id", familyId);
                    intent.putExtra("member_user_id", member.user_id);
                    intent.putExtra("member_name", member.name);
                    startActivity(intent);
                });
                row.addView(btnPermissions);
            }

            container.addView(row);
        }
    }
}