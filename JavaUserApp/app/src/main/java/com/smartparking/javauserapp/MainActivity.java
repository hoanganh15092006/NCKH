package com.smartparking.javauserapp;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity {
    private String username;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        username = getIntent().getStringExtra("username");
        if (username == null) username = "User";

        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                switchFragment(new HomeFragment());
                return true;
            } else if (id == R.id.nav_status) {
                switchFragment(new StatusFragment());
                return true;
            } else if (id == R.id.nav_notifications) {
                switchFragment(new NotificationFragment());
                return true;
            } else if (id == R.id.nav_profile) {
                switchFragment(new SettingsFragment());
                return true;
            }
            return false;
        });

        // Nút QR ở giữa
        FloatingActionButton fabQr = findViewById(R.id.fabQr);
        fabQr.setOnClickListener(v -> {
            // Lấy QR code từ HomeFragment hoặc bắn Intent QrActivity
            // Để đơn giản hơn, Trang HomeFragment có trách nhiệm gọi API lấy QR và lưu tạm.
            // Nên ta có thể truyền thẳng Intent từ đây nếu ta gọi api/user/info lần nữa, hoặc dùng tĩnh.
            // Tôi sẽ để nút này phát broadcast hoặc gọi method trong HomeFragment hiện tại.
            
            // Lấy QrActivity thông qua Extra
            Intent intent = new Intent(MainActivity.this, QrActivity.class);
            // Mã được thiết kế mặc định theo backend format f"QR_{username}"
            intent.putExtra("QR_CODE", "QR_" + username);
            startActivity(intent);
        });

        // Set default
        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_home);
        }
    }

    private void switchFragment(Fragment fragment) {
        Bundle bundle = new Bundle();
        bundle.putString("username", username);
        fragment.setArguments(bundle);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
    
    public String getUsername() {
        return username;
    }
}
