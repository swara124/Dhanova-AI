package com.example.dhanovaai;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class AddTransactionOptionsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_transaction_options);

        // Which dashboard opened this screen: "FAMILY" (default) or "PERSONAL"
        final String scope = getIntent().getStringExtra("scope") != null
                ? getIntent().getStringExtra("scope") : "FAMILY";

        // Option 1: Upload Bank Statement
        findViewById(R.id.btn_opt_statement).setOnClickListener(v ->
                Toast.makeText(this, "Bank statement upload coming soon",
                        Toast.LENGTH_SHORT).show());

        // Option 2: Receipt Scan (OCR)
        findViewById(R.id.btn_opt_scan).setOnClickListener(v ->
                Toast.makeText(this, "Receipt scan coming soon",
                        Toast.LENGTH_SHORT).show());

        // Option 3: Manual Entry -> opens activity_add_transaction.xml
        findViewById(R.id.btn_opt_manual).setOnClickListener(v -> {
            Intent intent = new Intent(AddTransactionOptionsActivity.this,
                    AddTransactionActivity.class);
            intent.putExtra("scope", scope);
            startActivity(intent);
        });
    }
}