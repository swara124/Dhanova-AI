package com.example.dhanovaai;

import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MemberPermissionsActivity extends AppCompatActivity {

    int familyId, memberUserId;

    CheckBox cbManageFamily, cbManageMembers, cbAssignPermissions,
            cbAddTransactions, cbEditTransactions, cbDeleteTransactions,
            cbManageBudgets, cbManageGoals;

    com.google.android.material.switchmaterial.SwitchMaterial switchMakeAdmin;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_member_permissions);

        familyId = getIntent().getIntExtra("family_id", -1);
        memberUserId = getIntent().getIntExtra("member_user_id", -1);
        String memberName = getIntent().getStringExtra("member_name");

        TextView tvTitle = findViewById(R.id.tv_permissions_title);
        tvTitle.setText("Permissions — " + memberName);

        cbManageFamily = findViewById(R.id.cb_manage_family);
        cbManageMembers = findViewById(R.id.cb_manage_members);
        cbAssignPermissions = findViewById(R.id.cb_assign_permissions);
        cbAddTransactions = findViewById(R.id.cb_add_transactions);
        cbEditTransactions = findViewById(R.id.cb_edit_transactions);
        cbDeleteTransactions = findViewById(R.id.cb_delete_transactions);
        cbManageBudgets = findViewById(R.id.cb_manage_budgets);
        cbManageGoals = findViewById(R.id.cb_manage_goals);

        switchMakeAdmin = findViewById(R.id.switch_make_admin);
        switchMakeAdmin.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) confirmTransfer(memberName);
        });

        loadCurrentPermissions();

        findViewById(R.id.btn_save_permissions).setOnClickListener(v -> savePermissions());
    }

    /** A family has exactly one admin, so this is a transfer: make the user aware before doing it. */
    private void confirmTransfer(String memberName) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Transfer admin role?")
                .setMessage(memberName + " will become the family admin and you will become a "
                        + "regular member. A family can only have one admin.")
                .setPositiveButton("Transfer", (d, w) -> promoteToAdmin())
                .setNegativeButton("Cancel", (d, w) -> switchMakeAdmin.setChecked(false))
                .setOnCancelListener(d -> switchMakeAdmin.setChecked(false))
                .show();
    }

    private void promoteToAdmin() {
        RoleRequest request = new RoleRequest("ADMIN");
        RetrofitClient.getApiService(this).setMemberRole(familyId, memberUserId, request)
                .enqueue(new retrofit2.Callback<SimpleMessageResponse>() {
                    @Override
                    public void onResponse(retrofit2.Call<SimpleMessageResponse> call, retrofit2.Response<SimpleMessageResponse> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(MemberPermissionsActivity.this, "Admin role transferred", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            switchMakeAdmin.setChecked(false);
                            Toast.makeText(MemberPermissionsActivity.this,
                                    response.code() == 403 ? "Only the family admin can transfer the admin role"
                                            : "Could not transfer admin role",
                                    Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(retrofit2.Call<SimpleMessageResponse> call, Throwable t) {
                        switchMakeAdmin.setChecked(false);
                        Toast.makeText(MemberPermissionsActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
    private void loadCurrentPermissions() {
        RetrofitClient.getApiService(this).getMemberPermissions(familyId, memberUserId)
                .enqueue(new Callback<PermissionsResponse>() {
                    @Override
                    public void onResponse(Call<PermissionsResponse> call, Response<PermissionsResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<String> current = response.body().permissions;
                            if (current.contains("MANAGE_FAMILY")) cbManageFamily.setChecked(true);
                            if (current.contains("MANAGE_MEMBERS")) cbManageMembers.setChecked(true);
                            if (current.contains("ASSIGN_PERMISSIONS")) cbAssignPermissions.setChecked(true);
                            if (current.contains("ADD_TRANSACTIONS")) cbAddTransactions.setChecked(true);
                            if (current.contains("EDIT_TRANSACTIONS")) cbEditTransactions.setChecked(true);
                            if (current.contains("DELETE_TRANSACTIONS")) cbDeleteTransactions.setChecked(true);
                            if (current.contains("MANAGE_BUDGETS")) cbManageBudgets.setChecked(true);
                            if (current.contains("MANAGE_GOALS")) cbManageGoals.setChecked(true);
                        }
                    }

                    @Override
                    public void onFailure(Call<PermissionsResponse> call, Throwable t) {
                        Toast.makeText(MemberPermissionsActivity.this, "Could not load current permissions", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void savePermissions() {
        List<String> selected = new ArrayList<>();
        if (cbManageFamily.isChecked()) selected.add("MANAGE_FAMILY");
        if (cbManageMembers.isChecked()) selected.add("MANAGE_MEMBERS");
        if (cbAssignPermissions.isChecked()) selected.add("ASSIGN_PERMISSIONS");
        if (cbAddTransactions.isChecked()) selected.add("ADD_TRANSACTIONS");
        if (cbEditTransactions.isChecked()) selected.add("EDIT_TRANSACTIONS");
        if (cbDeleteTransactions.isChecked()) selected.add("DELETE_TRANSACTIONS");
        if (cbManageBudgets.isChecked()) selected.add("MANAGE_BUDGETS");
        if (cbManageGoals.isChecked()) selected.add("MANAGE_GOALS");

        SetPermissionsRequest request = new SetPermissionsRequest(selected);

        RetrofitClient.getApiService(this).setMemberPermissions(familyId, memberUserId, request)
                .enqueue(new Callback<SimpleMessageResponse>() {
                    @Override
                    public void onResponse(Call<SimpleMessageResponse> call, Response<SimpleMessageResponse> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(MemberPermissionsActivity.this, "Permissions updated", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(MemberPermissionsActivity.this, "Could not update permissions", Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<SimpleMessageResponse> call, Throwable t) {
                        Toast.makeText(MemberPermissionsActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}