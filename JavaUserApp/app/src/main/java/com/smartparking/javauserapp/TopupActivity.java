package com.smartparking.javauserapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.smartparking.javauserapp.api.ApiClient;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TopupActivity extends AppCompatActivity {

    private EditText etAmount;
    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_topup);

        username = getIntent().getStringExtra("username");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Nạp tiền vào ví");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        etAmount = findViewById(R.id.etAmount);
        Button btn50k = findViewById(R.id.btn50k);
        Button btn100k = findViewById(R.id.btn100k);
        Button btn200k = findViewById(R.id.btn200k);
        LinearLayout btnMBBank = findViewById(R.id.btnMBBank);
        Button btnConfirm = findViewById(R.id.btnConfirm);

        btn50k.setOnClickListener(v -> etAmount.setText("50000"));
        btn100k.setOnClickListener(v -> etAmount.setText("100000"));
        btn200k.setOnClickListener(v -> etAmount.setText("200000"));

        btnConfirm.setOnClickListener(v -> {
            String amountStr = etAmount.getText().toString();
            if (amountStr.isEmpty() || amountStr.equals("0")) {
                Toast.makeText(this, "Vui lòng nhập số tiền hợp lệ", Toast.LENGTH_SHORT).show();
                return;
            }

            int amount = Integer.parseInt(amountStr);
            openBankingApp(amount);
        });

        btnMBBank.setOnClickListener(v -> {
             String amountStr = etAmount.getText().toString();
             int amount = amountStr.isEmpty() ? 0 : Integer.parseInt(amountStr);
             openBankingApp(amount);
        });
    }

    private void openBankingApp(int amount) {
        // Định dạng VietQR: https://img.vietqr.io/image/<BANK_ID>-<ACCOUNT_NUMBER>-<TEMPLATE>.png?amount=<AMOUNT>&addInfo=<DESCRIPTION>&accountName=<NAME>
        // MB Bank: MB, STK: 0916191655
        String description = "Nap_Tien_Smart_Parking_" + username;
        String vietQrUrl = "https://img.vietqr.io/image/MB-0916191655-compact2.png?amount=" + amount + "&addInfo=" + description;

        // Mở trình duyệt để hiển thị mã QR hoặc tự động mở app ngân hàng nếu hỗ trợ scheme
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(vietQrUrl));
        startActivity(browserIntent);

        // Giả lập sau khi chuyển hướng, chúng ta gọi API để cộng tiền (trong thực tế sẽ cần hệ thống webhook xác nhận)
        performApiTopup(amount);
    }

    private void performApiTopup(int amount) {
        Map<String, Object> body = new HashMap<>();
        body.put("username", username);
        body.put("amount", amount);

        ApiClient.getService().topup(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(TopupActivity.this, "Hệ thống đang chờ xác nhận giao dịch...", Toast.LENGTH_LONG).show();
                    // Để demo, chúng ta cho thành công luôn sau 2 giây
                    etAmount.postDelayed(() -> {
                        Toast.makeText(TopupActivity.this, "Nạp tiền thành công!", Toast.LENGTH_SHORT).show();
                        finish();
                    }, 2000);
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(TopupActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
