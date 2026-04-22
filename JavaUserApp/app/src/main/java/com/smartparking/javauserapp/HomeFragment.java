package com.smartparking.javauserapp;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.smartparking.javauserapp.api.ApiClient;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {
    private String username;
    private String qrCodeStr;
    private TextView tvWelcome, tvBalance;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        if (getArguments() != null) {
            username = getArguments().getString("username", "User");
        }

        tvWelcome = view.findViewById(R.id.tvWelcome);
        tvBalance = view.findViewById(R.id.tvBalance);
        
        tvWelcome.setText("Chào mừng, " + username + "!");

        LinearLayout btnNavTopup = view.findViewById(R.id.btnNavTopup);
        LinearLayout btnScan = view.findViewById(R.id.btnScan);

        btnNavTopup.setOnClickListener(v -> showTopupDialog());
        btnScan.setOnClickListener(v -> showScanDialog());

        loadUserInfo();
        return view;
    }

    private void loadUserInfo() {
        ApiClient.getService().getUserInfo(username).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> data = response.body();
                    Object bal = data.get("balance");
                    if (bal != null) {
                        double dbal = (double) bal;
                        tvBalance.setText((int)dbal + " VND");
                    }
                    qrCodeStr = (String) data.get("qr_code");
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Lỗi tải thông tin", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showTopupDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Nạp Tiền GTVT");

        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("Nhập số tiền (VND)");
        builder.setView(input);

        builder.setPositiveButton("Nạp", (dialog, which) -> {
            String amountStr = input.getText().toString();
            if (!amountStr.isEmpty()) {
                int amount = Integer.parseInt(amountStr);
                performTopup(amount);
            }
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void performTopup(int amount) {
        Map<String, Object> body = new HashMap<>();
        body.put("username", username);
        body.put("amount", amount);

        ApiClient.getService().topup(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Nạp tiền thành công!", Toast.LENGTH_SHORT).show();
                    loadUserInfo();
                }
            }
            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });
    }

    private void showScanDialog() {
        if (qrCodeStr == null) {
            Toast.makeText(getContext(), "Đang tải mã QR, thử lại sau.", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Chọn Làn Quét");
        CharSequence[] options = {"Quét Mã Xe Vào (IN - 0đ)", "Quét Mã Xe Ra (OUT - Thanh toán)"};
        
        builder.setItems(options, (dialog, which) -> {
            String type = (which == 0) ? "in" : "out";
            showQRDialog(type);
        });
        builder.show();
    }

    private void showQRDialog(String type) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(type.equals("in") ? "Quét QR Vào Bãi" : "Quét QR Ra Bãi");
        
        String msg = type.equals("in") 
            ? "Mô phỏng Thanh toán QR Nhà xe\n\nPhí vào bãi: 0 VND\n\nNhấn Xác nhận để đẩy thông tin lên Hệ thống Camera." 
            : "Mô phỏng Thanh toán QR Nhà xe\n\nPhí ra bãi: 3,000đ - 5,000đ (tùy giờ)\n\nNhấn Xác nhận để ép Camera kiểm tra thông tin của bạn.";
            
        builder.setMessage(msg);
        builder.setPositiveButton("Xác Nhận Thanh Toán", (dialog, which) -> {
            performScan(type);
        });
        builder.setNegativeButton("Hủy bỏ", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void performScan(String type) {
        Map<String, Object> body = new HashMap<>();
        body.put("qr_code", qrCodeStr);
        body.put("type", type);
        body.put("station_id", "Main Gate");

        ApiClient.getService().scanQr(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String msg = (String) response.body().get("message");
                    Toast.makeText(getContext(), "Barie Mở: " + msg, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getContext(), "Từ chối! Không đủ tiền hoặc chưa vao bãi.", Toast.LENGTH_LONG).show();
                }
                loadUserInfo();
            }
            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });
    }
}
