package com.example.ledlamp;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class PaintingActivity extends BaseActivity {
    private static final String TAG = "PaintingActivity";

    MatrixView matrixView;
    ImageButton btnPencil, btnEraser, btnClear;
    Button btnBack;
    View viewSpectrum, viewColorPreview;
    ImageView imgCursor; // Курсор
    SeekBar seekBarBrightness;

    private int selectedColor = Color.RED;

    private float currentHue = 0f;
    private float currentBrightness = 1f;

    private boolean isEraser = false;
    private static final int LAMP_PORT = 8888;
    private boolean swapColors = false;
    private long lastSendTime = 0;
    
    private boolean isListening = true;

    // Ресурси кольорів
    private int colorToolBtn;
    private int colorToolIcon;
    private int colorAccent;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_painting);

        matrixView = findViewById(R.id.matrixView);
        btnPencil = findViewById(R.id.btnToolPencil);
        btnEraser = findViewById(R.id.btnToolEraser);
        btnClear = findViewById(R.id.btnClear);
        btnBack = findViewById(R.id.btnBack);
        viewSpectrum = findViewById(R.id.viewSpectrum);
        viewColorPreview = findViewById(R.id.viewColorPreview);
        imgCursor = findViewById(R.id.imgCursor);
        seekBarBrightness = findViewById(R.id.seekBarBrushBrightness);

        SharedPreferences appPrefs = getSharedPreferences("LampAppPrefs", MODE_PRIVATE);
        swapColors = appPrefs.getBoolean("swap_colors", false);

        initThemeColors();

        sendUdpCommand("EFF 88");
        requestMatrixImage();
        startMatrixListener();

        setupUI();
        setupListeners();

        new android.os.Handler().postDelayed(() -> {
            updateCalculatedColor();
        }, 500);
    }

    private void initThemeColors() {
        TypedValue typedValue = new TypedValue();
        
        if (getTheme().resolveAttribute(R.attr.toolBtnColor, typedValue, true)) {
            colorToolBtn = typedValue.data;
        } else {
            colorToolBtn = Color.LTGRAY; 
        }

        if (getTheme().resolveAttribute(R.attr.toolIconColor, typedValue, true)) {
            colorToolIcon = typedValue.data;
        } else {
            colorToolIcon = Color.BLACK; 
        }

        if (getTheme().resolveAttribute(R.attr.accentColor, typedValue, true)) {
            colorAccent = typedValue.data;
        } else {
            colorAccent = Color.GREEN; 
        }
    }

    private void setupUI() {
        if (viewSpectrum == null) return;
        GradientDrawable rainbow = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[] { 0xFFFF0000, 0xFFFFFF00, 0xFF00FF00, 0xFF00FFFF, 0xFF0000FF, 0xFFFF00FF, 0xFFFF0000 }
        );
        rainbow.setCornerRadius(15f);
        viewSpectrum.setBackground(rainbow);
        
        updateToolsUI();
        updateCalculatedColor();
    }

    private void updateCalculatedColor() {
        if (viewColorPreview == null || imgCursor == null || matrixView == null || viewSpectrum == null) return;

        selectedColor = Color.HSVToColor(new float[]{currentHue, 1f, currentBrightness});

        viewColorPreview.getBackground().setTint(selectedColor);
        matrixView.setCurrentColor(selectedColor);

        // Оновлюємо позицію курсора відповідно до Hue (0..360)
        // Ширина viewSpectrum може змінюватись, тому треба брати актуальну ширину
        viewSpectrum.post(() -> {
            if (viewSpectrum == null || imgCursor == null) return;
            int width = viewSpectrum.getWidth();
            if (width > 0) {
                // Hue 0..360 -> x 0..width
                float x = (currentHue / 360f) * width;
                
                // Центруємо курсор
                float cursorX = x - (imgCursor.getWidth() / 2f);
                
                // Обмежуємо межі
                if (cursorX < 0) cursorX = 0;
                if (cursorX > width - imgCursor.getWidth()) cursorX = width - imgCursor.getWidth();

                imgCursor.setX(cursorX);
            }
        });

        if (!isEraser) {
            sendColorCommand(selectedColor);
        }
    }

    private void setupListeners() {
        if (btnPencil != null) {
            btnPencil.setOnClickListener(v -> {
                vibrate();
                isEraser = false;
                updateToolsUI();
                if (matrixView != null) matrixView.setCurrentColor(selectedColor);
                sendColorCommand(selectedColor);
            });
        }

        if (btnEraser != null) {
            btnEraser.setOnClickListener(v -> {
                vibrate();
                isEraser = true;
                updateToolsUI();
                if (matrixView != null) matrixView.setCurrentColor(Color.BLACK);
                sendUdpCommand("COL;0;0;0");
            });
        }

        if (btnClear != null) {
            btnClear.setOnClickListener(v -> {
                vibrate();
                if (matrixView != null) matrixView.clear();
                sendUdpCommand("CLR");
            });
        }

        // Ми тепер слухаємо дотики на viewSpectrum, але обробляємо їх
        // так само. Тільки тепер ми будемо оновлювати UI курсора в updateCalculatedColor
        if (viewSpectrum != null) {
            viewSpectrum.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
                    float x = event.getX();
                    float width = v.getWidth();
                    if (x < 0) x = 0;
                    if (x > width) x = width;

                    currentHue = (x / width) * 360f;

                    if (isEraser) {
                        isEraser = false;
                        updateToolsUI();
                    }

                    // Курсор буде оновлено всередині updateCalculatedColor
                    if (System.currentTimeMillis() - lastSendTime > 100) {
                        updateCalculatedColor();
                        lastSendTime = System.currentTimeMillis();
                    } else {
                        // Якщо ми пропускаємо відправку UDP, все одно оновимо UI (превью + курсор)
                        // Для плавності краще оновлювати курсор завжди
                        selectedColor = Color.HSVToColor(new float[]{currentHue, 1f, currentBrightness});
                        if (viewColorPreview != null)
                            viewColorPreview.getBackground().setTint(selectedColor);

                        // Локальне оновлення позиції (дубляж логіки для швидкості UI)
                        if (imgCursor != null) {
                            float cursorX = x - (imgCursor.getWidth() / 2f);
                            imgCursor.setX(cursorX);
                        }
                    }
                }
                if (event.getAction() == MotionEvent.ACTION_UP) updateCalculatedColor();
                return true;
            });
        }

        if (seekBarBrightness != null) {
            seekBarBrightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        currentBrightness = progress / 100f;
                        if (isEraser) {
                            isEraser = false;
                            updateToolsUI();
                        }
                        if (System.currentTimeMillis() - lastSendTime > 100) {
                            updateCalculatedColor();
                            lastSendTime = System.currentTimeMillis();
                        }
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    updateCalculatedColor();
                }
            });
        }

        if (matrixView != null) {
            matrixView.setOnPixelTouchListener((x, y, color) -> {
                long now = System.currentTimeMillis();
                if (now - lastSendTime > 25) {
                    sendUdpCommand("DRW;" + x + ";" + y);
                    lastSendTime = now;
                }
            });
        }

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                vibrate();
                finish();
            });
        }
    }

    private void requestMatrixImage() {
        sendUdpCommand("MTRX_GET");
    }

    private void startMatrixListener() {
        new Thread(() -> {
            try {
                DatagramSocket socket = new DatagramSocket();
                SharedPreferences settings = getSharedPreferences("LampSettings", MODE_PRIVATE);
                String ip = settings.getString("LAMP_IP", "");
                InetAddress address = InetAddress.getByName(ip);

                byte[] data = "MTRX_GET".getBytes();
                DatagramPacket packet = new DatagramPacket(data, data.length, address, LAMP_PORT);
                socket.send(packet);

                byte[] buf = new byte[1024];
                DatagramPacket recv = new DatagramPacket(buf, buf.length);
                socket.setSoTimeout(3000);

                socket.receive(recv);

                if (recv.getLength() >= 768) {
                    final byte[] matrixData = new byte[768];
                    System.arraycopy(recv.getData(), 0, matrixData, 0, 768);
                    runOnUiThread(() -> {
                        if (matrixView != null) matrixView.setFullImage(matrixData);
                    });
                }
                socket.close();
            } catch (Exception e) {
                Log.e(TAG, "Error in matrix listener", e);
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isListening = false;
        SharedPreferences prefs = getSharedPreferences("LampSettings", MODE_PRIVATE);
        int lastEffect = prefs.getInt("LAST_EFFECT_ID", 0);
        if (lastEffect != 88) {
            sendUdpCommand("EFF " + lastEffect);
        }
    }

    private void updateToolsUI() {
        int dangerColor = 0;
        
        TypedValue typedValue = new TypedValue();
        if (getTheme().resolveAttribute(R.attr.dangerColor, typedValue, true)) {
            dangerColor = typedValue.data;
        } else {
            dangerColor = Color.RED;
        }

        if (btnPencil != null && btnPencil.getBackground() != null) {
            btnPencil.getBackground().mutate().setTint(colorToolBtn);
        }
        
        if (btnEraser != null && btnEraser.getBackground() != null) {
            btnEraser.getBackground().mutate().setTint(colorToolBtn);
        }

        if (btnClear != null && btnClear.getBackground() != null) {
            btnClear.getBackground().mutate().setTint(colorToolBtn);
        }
        
        if (btnClear != null) btnClear.setColorFilter(dangerColor);

        if (isEraser) {
            if (btnEraser != null) btnEraser.setColorFilter(colorAccent);
            if (btnPencil != null) btnPencil.setColorFilter(colorToolIcon);
        } else {
            if (btnPencil != null) btnPencil.setColorFilter(colorAccent);
            if (btnEraser != null) btnEraser.setColorFilter(colorToolIcon);
        }
    }

    private void sendColorCommand(int color) {
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        if (swapColors) { int temp = r; r = g; g = temp; }
        sendUdpCommand("COL;" + r + ";" + b + ";" + g);
    }

    private void sendUdpCommand(String command) {
        SharedPreferences settings = getSharedPreferences("LampSettings", MODE_PRIVATE);
        String ip = settings.getString("LAMP_IP", "");
        if (ip.isEmpty()) return;

        new Thread(() -> {
            try {
                DatagramSocket socket = new DatagramSocket();
                byte[] data = command.getBytes();
                InetAddress address = InetAddress.getByName(ip);
                DatagramPacket packet = new DatagramPacket(data, data.length, address, LAMP_PORT);
                socket.send(packet);
                socket.close();
            } catch (Exception e) {
                Log.e(TAG, "Error sending UDP command", e);
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