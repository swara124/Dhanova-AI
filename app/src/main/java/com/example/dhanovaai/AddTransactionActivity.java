package com.example.dhanovaai;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddTransactionActivity extends AppCompatActivity {

    private static final String[] EXPENSE_CATEGORIES = {
            "Food & Groceries", "Transport", "Bills", "Shopping",
            "Health", "Education", "Entertainment", "Others"};

    private static final String[] INCOME_CATEGORIES = {
            "Salary", "Freelance", "Business", "Gift", "Others"};

    private static final String[] PAYMENT_METHODS = {"Cash", "UPI", "Card", "Bank"};

    MaterialButtonToggleGroup toggleType;
    ChipGroup chipCategory, chipPayment;
    android.widget.EditText etAmount;
    TextInputEditText etTitle, etDate, etNote;
    View btnSave;

    boolean isExpense = true;
    String selectedDate;
    int familyId;
    String scope = "FAMILY"; // "FAMILY" or "PERSONAL"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_transaction);

        SharedPreferences prefs = getSharedPreferences("dhanova_prefs", MODE_PRIVATE);
        familyId = prefs.getInt("current_family_id", -1);

        if ("PERSONAL".equals(getIntent().getStringExtra("scope"))) {
            scope = "PERSONAL";
        }

        // Only FAMILY transactions need the permission; personal ones are always your own data
        if ("FAMILY".equals(scope) && !PermissionsManager.has("ADD_TRANSACTIONS")) {
            Toast.makeText(this, "You don't have permission to add transactions",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        toggleType = findViewById(R.id.toggle_type);
        chipCategory = findViewById(R.id.chip_group_category);
        chipPayment = findViewById(R.id.chip_group_payment);
        etAmount = findViewById(R.id.et_amount);
        etTitle = findViewById(R.id.et_title);
        etDate = findViewById(R.id.et_date);
        etNote = findViewById(R.id.et_note);
        btnSave = findViewById(R.id.btn_save_transaction);

        SimpleDateFormat server = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        selectedDate = server.format(new Date());
        etDate.setText(formatForDisplay(new Date()));

        buildChips(chipCategory, EXPENSE_CATEGORIES);
        buildChips(chipPayment, PAYMENT_METHODS);

        toggleType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            isExpense = (checkedId == R.id.btn_type_expense);
            buildChips(chipCategory, isExpense ? EXPENSE_CATEGORIES : INCOME_CATEGORIES);
        });

        etDate.setOnClickListener(v -> showDatePicker());

        btnSave.setOnClickListener(v -> saveTransaction());
    }

    private void buildChips(ChipGroup group, String[] items) {
        group.removeAllViews();
        for (int i = 0; i < items.length; i++) {
            Chip chip = new Chip(this);
            chip.setText(items[i]);
            chip.setCheckable(true);
            chip.setClickable(true);
            chip.setCheckedIconVisible(false);
            chip.setChipBackgroundColorResource(R.color.chip_bg_selector);
            chip.setTextColor(getResources().getColorStateList(R.color.chip_text_selector, getTheme()));
            chip.setChipStrokeColorResource(android.R.color.transparent);
            chip.setId(View.generateViewId());
            group.addView(chip);
            if (i == 0) chip.setChecked(true);
        }
    }

    private String selectedText(ChipGroup group) {
        int id = group.getCheckedChipId();
        if (id == View.NO_ID) return "";
        Chip chip = group.findViewById(id);
        return chip != null ? chip.getText().toString() : "";
    }

    private void showDatePicker() {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select date")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build();

        picker.addOnPositiveButtonClickListener(selection -> {
            SimpleDateFormat server = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            server.setTimeZone(TimeZone.getTimeZone("UTC"));
            selectedDate = server.format(new Date(selection));

            SimpleDateFormat shown = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            shown.setTimeZone(TimeZone.getTimeZone("UTC"));
            etDate.setText(shown.format(new Date(selection)));
        });

        picker.show(getSupportFragmentManager(), "date_picker");
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private String formatForDisplay(Date date) {
        return new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date);
    }

    private void saveTransaction() {
        String amountText = etAmount.getText().toString().trim();
        String title = etTitle.getText() == null ? "" : etTitle.getText().toString().trim();

        if (amountText.isEmpty()) {
            etAmount.setError("Enter an amount");
            showError("Please enter the amount");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountText);
        } catch (NumberFormatException e) {
            etAmount.setError("Invalid amount");
            showError("Invalid amount");
            return;
        }
        if (amount <= 0) {
            etAmount.setError("Amount must be more than 0");
            showError("Amount must be more than 0");
            return;
        }
        if (title.isEmpty()) {
            etTitle.setError("Enter a title");
            showError("Please enter a title");
            return;
        }
        if (familyId == -1) {
            Toast.makeText(this, "No family found", Toast.LENGTH_SHORT).show();
            return;
        }

        AddTransactionRequest request = new AddTransactionRequest();
        request.scope = scope;
        request.type = isExpense ? "EXPENSE" : "INCOME";
        request.amount = amount;
        request.category = selectedText(chipCategory);
        request.title = title;
        request.date = selectedDate;
        request.payment_method = selectedText(chipPayment);
        request.note = etNote.getText() == null ? "" : etNote.getText().toString().trim();

        btnSave.setEnabled(false);
        Toast.makeText(this, "Saving...", Toast.LENGTH_SHORT).show();

        RetrofitClient.getApiService(this).addTransaction(familyId, request)
                .enqueue(new Callback<SimpleMessageResponse>() {
                    @Override
                    public void onResponse(Call<SimpleMessageResponse> call,
                                           Response<SimpleMessageResponse> response) {
                        btnSave.setEnabled(true);
                        if (response.isSuccessful()) {
                            Toast.makeText(AddTransactionActivity.this,
                                    "Transaction saved", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(AddTransactionActivity.this,
                                    "Could not save transaction (code " + response.code() + ")", Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<SimpleMessageResponse> call, Throwable t) {
                        btnSave.setEnabled(true);
                        Toast.makeText(AddTransactionActivity.this,
                                "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}