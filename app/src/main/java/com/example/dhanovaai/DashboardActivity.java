package com.example.dhanovaai;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.navigation.NavigationView;

public class DashboardActivity extends AppCompatActivity {

    TextView tvWelcome, tvFamilyName, tvTotalBalance, tvIncome, tvExpenses, tvSavings;
    DrawerLayout drawerLayout;
    NavigationView navView;
    SharedPreferences prefs;

    // Which dashboard is showing, and the last numbers fetched from the server
    boolean showingFamilyView = true;
    SummaryResponse lastSummary;

    // True once role + permissions have come back from the server, so we never
    // show a wrong "not permitted" popup just because the data hasn't loaded yet
    boolean permissionsReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        tvWelcome = findViewById(R.id.tv_welcome);
        tvFamilyName = findViewById(R.id.tv_family_name);
        tvTotalBalance = findViewById(R.id.tv_total_balance);
        tvIncome = findViewById(R.id.tv_income);
        tvExpenses = findViewById(R.id.tv_expenses);
        tvSavings = findViewById(R.id.tv_savings);

        prefs = getSharedPreferences("dhanova_prefs", MODE_PRIVATE);

        tvWelcome.setText("Welcome, " + prefs.getString("user_name", "User") + "!");
        tvFamilyName.setText(prefs.getString("current_family_name", "No family yet"));

        // Placeholder numbers until loadSummary() returns
        tvTotalBalance.setText("₹0");
        tvIncome.setText("₹0");
        tvExpenses.setText("₹0");
        tvSavings.setText("₹0");

        setupDrawer();
        setupQuickActionButtons();
        setupDashboardToggle();
        // Data + permissions are loaded in onResume(), which always runs right after onCreate()
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Runs every time the screen comes back (after adding a transaction, after the
        // admin role was transferred, after permissions were changed, ...)
        loadSummary();
        PermissionsManager.load(this, () -> permissionsReady = true);
    }

    // ---------------------------------------------------------------
    // Family / My Finances toggle
    // ---------------------------------------------------------------

    private void setupDashboardToggle() {
        MaterialButtonToggleGroup toggle = findViewById(R.id.toggle_dashboard_view);

        // Start on Family, before attaching the listener so it doesn't fire early
        toggle.check(R.id.btn_view_family);

        toggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            showingFamilyView = (checkedId == R.id.btn_view_family);
            renderSummary();
        });
    }

    private void renderSummary() {
        if (lastSummary == null) return;

        if (showingFamilyView) {
            tvFamilyName.setText(prefs.getString("current_family_name", "No family yet"));
            tvTotalBalance.setText("₹" + lastSummary.family_balance);
            tvIncome.setText("₹" + lastSummary.family_income);
            tvExpenses.setText("₹" + lastSummary.family_expense);
            tvSavings.setText("—"); // family-level savings comes later with Goals/Budget
        } else {
            tvFamilyName.setText("Your personal finances");
            tvTotalBalance.setText("₹" + lastSummary.personal_balance);
            tvIncome.setText("₹" + lastSummary.personal_income);
            tvExpenses.setText("₹" + lastSummary.personal_expense);
            tvSavings.setText("₹" + lastSummary.personal_balance);
        }
    }

    private void loadSummary() {
        RetrofitClient.getApiService(this).getSummary().enqueue(new retrofit2.Callback<SummaryResponse>() {
            @Override
            public void onResponse(retrofit2.Call<SummaryResponse> call,
                                   retrofit2.Response<SummaryResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    lastSummary = response.body();
                    renderSummary();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<SummaryResponse> call, Throwable t) {
                // Silently keep the previous numbers if this fails
            }
        });
    }

    // ---------------------------------------------------------------
    // Permission rule — ONE place for the whole dashboard
    // ---------------------------------------------------------------

    /**
     * Every member sees the same dashboard as the admin; nothing is hidden.
     *  - My Finances view: everything is open (it's your own data).
     *  - Family view: the tap only works if you hold the permission (admin holds all),
     *    otherwise a "Not permitted" popup is shown.
     */
    private void guarded(String permission, String actionLabel, Runnable onAllowed) {
        if (!showingFamilyView) {
            onAllowed.run();
            return;
        }
        if (!permissionsReady) {
            Toast.makeText(this, "Still loading your permissions, try again in a moment",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        PermissionGuard.require(this, permission, actionLabel, onAllowed);
    }

    /**
     * The Family screen (member list, invite, manage permissions) is family management,
     * so it is guarded in BOTH views. A member needs at least one of the "Family" group
     * permissions (Manage Family / Manage Members / Assign Permissions); admins hold all.
     */
    private void openFamilyGuarded() {
        if (!permissionsReady) {
            Toast.makeText(this, "Still loading your permissions, try again in a moment",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        if (PermissionsManager.has("MANAGE_FAMILY")
                || PermissionsManager.has("MANAGE_MEMBERS")
                || PermissionsManager.has("ASSIGN_PERMISSIONS")) {
            openFamilyMembers();
        } else {
            PermissionGuard.showDenied(this, "manage the family");
        }
    }

    private void openAddTransaction() {
        Intent intent = new Intent(this, AddTransactionOptionsActivity.class);
        intent.putExtra("scope", showingFamilyView ? "FAMILY" : "PERSONAL");
        startActivity(intent);
    }

    private void openBudget() {
        Toast.makeText(this, "Budget screen coming soon", Toast.LENGTH_SHORT).show();
    }

    private void openGoals() {
        Toast.makeText(this, "Goals screen coming soon", Toast.LENGTH_SHORT).show();
    }

    // ---------------------------------------------------------------
    // Drawer + buttons
    // ---------------------------------------------------------------

    private void setupDrawer() {
        drawerLayout = findViewById(R.id.drawer_layout);
        navView = findViewById(R.id.nav_view);

        // Menu icon in my layout (replaces the Toolbar toggle)
        findViewById(R.id.iv_menu).setOnClickListener(v ->
                drawerLayout.openDrawer(GravityCompat.START));

        navView.setNavigationItemSelectedListener(this::onDrawerItemSelected);

        View header = navView.getHeaderView(0);
        ((TextView) header.findViewById(R.id.tv_nav_user_name))
                .setText(prefs.getString("user_name", "User"));
        ((TextView) header.findViewById(R.id.tv_nav_family))
                .setText(prefs.getString("current_family_name", "No family yet"));
    }

    private boolean onDrawerItemSelected(MenuItem item) {
        drawerLayout.closeDrawers();
        int id = item.getItemId();

        if (id == R.id.nav_dashboard) {
            // Already here — just close the drawer
        } else if (id == R.id.nav_transactions) {
            Toast.makeText(this, "Transactions screen coming soon", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_family_settings) {
            openFamilyGuarded();
        } else if (id == R.id.nav_goals) {
            guarded("MANAGE_GOALS", "manage family goals", this::openGoals);
        } else if (id == R.id.nav_budget) {
            guarded("MANAGE_BUDGETS", "manage the family budget", this::openBudget);
        } else if (id == R.id.nav_ai_assistant) {
            Toast.makeText(this, "AI Assistant coming soon", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_my_account) {
            Toast.makeText(this, "My Account screen coming soon", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_notifications) {
            Toast.makeText(this, "Notifications coming soon", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_logout) {
            logout();
        }
        return true;
    }

    private void setupQuickActionButtons() {
        findViewById(R.id.btn_family).setOnClickListener(v -> openFamilyGuarded());

        findViewById(R.id.btn_add_transaction).setOnClickListener(v ->
                guarded("ADD_TRANSACTIONS", "add family transactions", this::openAddTransaction));

        findViewById(R.id.btn_budget).setOnClickListener(v ->
                guarded("MANAGE_BUDGETS", "manage the family budget", this::openBudget));

        findViewById(R.id.btn_goals).setOnClickListener(v ->
                guarded("MANAGE_GOALS", "manage family goals", this::openGoals));
    }

    private void openFamilyMembers() {
        // Read fresh, so it is correct after creating/joining a family
        int familyId = prefs.getInt("current_family_id", -1);
        if (familyId == -1) {
            Toast.makeText(this, "No family found", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, FamilyMembersActivity.class);
        intent.putExtra("family_id", familyId);
        startActivity(intent);
    }

    private void logout() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove("auth_token");
        editor.remove("user_id");
        editor.remove("user_name");
        editor.remove("current_family_id");
        editor.remove("current_family_name");
        editor.remove("current_family_role");
        // Deliberately KEEP last_user_email so Splash still offers PIN entry next time
        editor.apply();

        PermissionsManager.clear();

        Intent intent = new Intent(this, SplashActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawers();
        } else {
            super.onBackPressed();
        }
    }
}