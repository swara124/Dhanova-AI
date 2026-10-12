package com.example.dhanovaai;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class WelcomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        findViewById(R.id.btn_create_family).setOnClickListener(v ->
                startActivity(new Intent(WelcomeActivity.this, CreateFamilyActivity.class)));

        findViewById(R.id.btn_join_family).setOnClickListener(v ->
                startActivity(new Intent(WelcomeActivity.this, InvitationsActivity.class)));
    }
}