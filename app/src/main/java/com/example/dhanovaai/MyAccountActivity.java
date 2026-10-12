package com.example.dhanovaai;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

public class MyAccountActivity extends AppCompatActivity {

    private static final String[] CURRENCIES = {"INR", "USD", "EUR", "GBP"}
            ;

    SharedPreferences prefs;
    TextInputEditText etName, etEmail, etContribution;
    AutoCompleteTextView ddCurrency;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_account);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("My Account");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        prefs = getSharedPreferences("dhanova_prefs", MODE_PRIVATE);

        etName = findViewById(R.id.et_account_name);
        etEmail = findViewById(R.id.et_account_email);
        etContribution = findViewById(R.id.et_contribution_amount);
        ddCurrency = findViewById(R.id.dd_currency);

        showProfile();
        setupCurrencyDropdown();
        setupClicks();
    }

    private void showProfile() {
        String name = prefs.getString("user_name", "User");
        String email = prefs.getString("last_user_email", "");
        String family = prefs.getString("current_family_name", "No family yet");

        ((TextView) findViewById(R.id.tv_profile_name)).setText(name);
        ((TextView) findViewById(R.id.tv_profile_email)).setText(email);
        ((TextView) findViewById(R.id.tv_profile_family)).setText(family);
        ((TextView) findViewById(R.id.tv_avatar_initial)).setText(
                name.isEmpty() ? "U" : name.substring(0, 1).toUpperCase());

        etName.setText(name);
        etEmail.setText(email);
    }

    private void setupCurrencyDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_list_item_1, CURRENCIES);
        ddCurrency.setAdapter(adapter);
        ddCurrency.setText(prefs.getString("preferred_currency", "INR"), false);
    }

    private void setupClicks() {
        findViewById(R.id.btn_save_profile).setOnClickListener(v ->
                comingSoon("Saving profile changes"));

        findViewById(R.id.btn_acc_add_transaction).setOnClickListener(v ->
                startActivity(new Intent(this, AddTransactionOptionsActivity.class)));

        findViewById(R.id.btn_add_goal).setOnClickListener(v ->
                comingSoon("Personal goals"));

        findViewById(R.id.btn_set_budget).setOnClickListener(v ->
                comingSoon("Budget"));

        findViewById(R.id.btn_contribute).setOnClickListener(v -> {
            String amount = etContribution.getText() == null
                    ? "" : etContribution.getText().toString().trim();
            if (amount.isEmpty()) {
                Toast.makeText(this, "Enter a contribution amount", Toast.LENGTH_SHORT).show();
                return;
            }
            comingSoon("Family goal contribution");
        });

        findViewById(R.id.row_change_password).setOnClickListener(v ->
                comingSoon("Change password"));

        findViewById(R.id.row_change_secret).setOnClickListener(v ->
                comingSoon("Change secret code"));
    }

    private void comingSoon(String feature) {
        Toast.makeText(this, feature + " coming soon", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}