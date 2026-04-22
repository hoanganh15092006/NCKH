package com.smartparking.javauserapp;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;

public class QrActivity extends AppCompatActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr);

        String qrString = getIntent().getStringExtra("QR_CODE");
        
        ImageView ivQrCode = findViewById(R.id.ivQrCode);
        TextView tvQrString = findViewById(R.id.tvQrString);
        Button btnClose = findViewById(R.id.btnClose);
        
        btnClose.setOnClickListener(v -> finish());
        
        if (qrString != null) {
            tvQrString.setText(qrString);
            try {
                BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
                Bitmap bitmap = barcodeEncoder.encodeBitmap(qrString, BarcodeFormat.QR_CODE, 400, 400);
                ivQrCode.setImageBitmap(bitmap);
            } catch (WriterException e) {
                Toast.makeText(this, "Lỗi tạo QR", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
