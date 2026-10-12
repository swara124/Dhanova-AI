package com.example.dhanovaai;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InvitationsActivity extends AppCompatActivity {

    LinearLayout container;
    TextView tvNoInvitations;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invitations);

        container = findViewById(R.id.container_invitations);
        tvNoInvitations = findViewById(R.id.tv_no_invitations);

        loadInvitations();
    }

    private void loadInvitations() {
        RetrofitClient.getApiService(this).getInvitations().enqueue(new Callback<InvitationsResponse>() {
            @Override
            public void onResponse(Call<InvitationsResponse> call, Response<InvitationsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    renderInvitations(response.body().invitations);
                } else {
                    Toast.makeText(InvitationsActivity.this, "Could not load invitations", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<InvitationsResponse> call, Throwable t) {
                Toast.makeText(InvitationsActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void renderInvitations(List<Invitation> invitations) {
        container.removeAllViews();

        if (invitations == null || invitations.isEmpty()) {
            tvNoInvitations.setVisibility(View.VISIBLE);
            return;
        }
        tvNoInvitations.setVisibility(View.GONE);

        for (Invitation inv : invitations) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(24, 24, 24, 24);
            card.setBackgroundColor(Color.parseColor("#F1F8E9"));
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            cardParams.setMargins(0, 0, 0, 16);
            card.setLayoutParams(cardParams);

            TextView tvFamily = new TextView(this);
            tvFamily.setText(inv.family_name);
            tvFamily.setTextSize(16);
            tvFamily.setTypeface(null, android.graphics.Typeface.BOLD);

            TextView tvFrom = new TextView(this);
            tvFrom.setText("Invited by " + inv.invited_by_name);
            tvFrom.setTextColor(Color.parseColor("#777777"));

            LinearLayout buttonRow = new LinearLayout(this);
            buttonRow.setOrientation(LinearLayout.HORIZONTAL);
            buttonRow.setGravity(Gravity.END);
            buttonRow.setPadding(0, 16, 0, 0);

            Button btnAccept = new Button(this);
            btnAccept.setText("Accept");
            btnAccept.setOnClickListener(v -> respond(inv.invitation_id, "ACCEPT"));

            Button btnReject = new Button(this);
            btnReject.setText("Reject");
            btnReject.setOnClickListener(v -> respond(inv.invitation_id, "REJECT"));

            buttonRow.addView(btnAccept);
            buttonRow.addView(btnReject);

            card.addView(tvFamily);
            card.addView(tvFrom);
            card.addView(buttonRow);
            container.addView(card);
        }
    }

    private void respond(int invitationId, String action) {
        RespondRequest request = new RespondRequest(action);

        RetrofitClient.getApiService(this).respondInvitation(invitationId, request).enqueue(new Callback<SimpleMessageResponse>() {
            @Override
            public void onResponse(Call<SimpleMessageResponse> call, Response<SimpleMessageResponse> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(InvitationsActivity.this, "Invitation " + action.toLowerCase(), Toast.LENGTH_SHORT).show();
                    loadInvitations(); // refresh the list
                } else {
                    String err = response.body() != null && response.body().error != null
                            ? response.body().error : "Action failed";
                    Toast.makeText(InvitationsActivity.this, err, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<SimpleMessageResponse> call, Throwable t) {
                Toast.makeText(InvitationsActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}