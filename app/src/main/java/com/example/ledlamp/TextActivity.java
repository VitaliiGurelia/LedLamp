package com.example.ledlamp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class TextActivity extends BaseActivity {
    private static final String TAG = "TextActivity";

    EditText editRunningText;
    // Оголошуємо всі кнопки в одному рядку
    Button btnSendText, btnBack, btnHelp;

    private static final int LAMP_PORT = 8888;
    private String currentIp = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text);

        editRunningText = findViewById(R.id.editRunningText);
        btnSendText = findViewById(R.id.btnSendText);
        btnBack = findViewById(R.id.btnBack);
        btnHelp = findViewById(R.id.btnHelp); // Ініціалізація кнопки довідки

        SharedPreferences prefs = getSharedPreferences("LampSettings", MODE_PRIVATE);
        currentIp = prefs.getString("LAMP_IP", "192.168.0.105");

        // Кнопка ВІДПРАВИТИ
        btnSendText.setOnClickListener(v -> {
            vibrate();
            String text = editRunningText.getText().toString();
            if (!text.isEmpty()) {
                byte[] dataToSend = encodeTextToBytes(text);
                sendRawBytes(dataToSend);
                Toast.makeText(this, R.string.msg_text_sent, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.hint_enter_text, Toast.LENGTH_SHORT).show();
            }
        });

        // Кнопка ДОВІДКА
        if (btnHelp != null) {
            btnHelp.setOnClickListener(v -> {
                vibrate();
                startActivity(new Intent(TextActivity.this, HelpActivity.class));
            });
        }

        // Кнопка НАЗАД
        btnBack.setOnClickListener(v -> {
            vibrate();
            finish();
        });
    }

    private byte[] encodeTextToBytes(String input) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            baos.write("TXT ".getBytes());
            for (char c : input.toCharArray()) {
                int code = (int) c;
                // Наші спец-символи з fonts.h
                if (c == 'Æ') baos.write(128);
                else if (c == 'Ø') baos.write(129);
                else if (c == 'Å') baos.write(130);
                else if (c == 'æ') baos.write(131);
                else if (c == 'ø') baos.write(132);
                else if (c == 'å') baos.write(133);
                else if (c == 'Є') baos.write(134);
                else if (c == 'І') baos.write(135);
                else if (c == 'Ї') baos.write(136);
                else if (c == 'є') baos.write(137);
                else if (c == 'і') baos.write(138);
                else if (c == 'ї') baos.write(139);
                else if (c == 'Ґ') baos.write(140);
                else if (c == 'ґ') baos.write(141);

                    // Кирилиця CP1251
                else if (code >= 1040 && code <= 1103) baos.write(code - 1040 + 192);
                else if (code == 1025) baos.write(168);
                else if (code == 1105) baos.write(184);

                else baos.write(code);
            }
        } catch (IOException e) {
            Log.e(TAG, "Error encoding text to bytes", e);
        }
        return baos.toByteArray();
    }

    private void sendRawBytes(byte[] data) {
        if (currentIp.isEmpty()) {
            Toast.makeText(this, R.string.msg_lamp_not_found, Toast.LENGTH_SHORT).show();
            return;
        }
        new Thread(() -> {
            try {
                DatagramSocket socket = new DatagramSocket();
                InetAddress address = InetAddress.getByName(currentIp);
                DatagramPacket packet = new DatagramPacket(data, data.length, address, LAMP_PORT);
                socket.send(packet);
                socket.close();
            } catch (Exception e) {
                Log.e(TAG, "Error sending raw bytes", e);
            }
        }).start();
    }

    private void vibrate() {
        SharedPreferences prefs = getSharedPreferences("LampAppPrefs", MODE_PRIVATE);
        if (prefs.getBoolean("vibration", true)) {
            Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (v != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) v.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE));
                else v.vibrate(20);
            }
        }
    }
}