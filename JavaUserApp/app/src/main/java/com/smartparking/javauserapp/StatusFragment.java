package com.smartparking.javauserapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.smartparking.javauserapp.api.ApiClient;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StatusFragment extends Fragment {

    private String username;
    private TextView tvActiveCarsStatus;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_status, container, false);
        tvActiveCarsStatus = view.findViewById(R.id.tvActiveCarsStatus);

        if (getArguments() != null) {
            username = getArguments().getString("username", "");
        }

        loadActiveSessions();
        return view;
    }

    private void loadActiveSessions() {
        if (username.isEmpty()) return;
        ApiClient.getService().getActiveSessions(username).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Map<String, Object>> sessions = (List<Map<String, Object>>) response.body().get("sessions");
                    if (sessions == null || sessions.isEmpty()) {
                        tvActiveCarsStatus.setText("Hiện tại không có xe nào của bạn trong bãi.");
                    } else {
                        StringBuilder sb = new StringBuilder();
                        for (Map<String, Object> session : sessions) {
                            String plate = (String) session.get("plate");
                            String time = (String) session.get("entry_time");
                            sb.append("🚗 Biển số: ").append(plate).append("\n");
                            sb.append("🕒 Giờ vào: ").append(time).append("\n\n");
                        }
                        tvActiveCarsStatus.setText(sb.toString());
                    }
                } else {
                    tvActiveCarsStatus.setText("Không thể lấy dữ liệu.");
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                tvActiveCarsStatus.setText("Lỗi kết nối máy chủ.");
            }
        });
    }
}
