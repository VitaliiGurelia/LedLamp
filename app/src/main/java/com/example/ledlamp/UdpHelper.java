package com.example.ledlamp;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

public class UdpHelper {
    private static final String TAG = "UdpHelper";
    private static final int LAMP_PORT = 8888;
    private boolean isListening = false;
    private Thread listenerThread;
    private String currentIp = "";
    private InetAddress cachedAddress = null;

    public interface UdpListener {
        void onMessageReceived(String message);
        void onConnectionLost();
    }

    private UdpListener listener;

    public void setListener(UdpListener listener) {
        this.listener = listener;
    }

    public void setIp(String ip) {
        if (ip == null || ip.equals(this.currentIp)) return;
        this.currentIp = ip;
        this.cachedAddress = null; // Скидаємо кеш, щоб резолвити нову IP
    }

    public void sendCommand(String command) {
        if (currentIp.isEmpty()) return;
        new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket()) {
                byte[] data = command.getBytes();
                InetAddress address = InetAddress.getByName(currentIp);
                DatagramPacket packet = new DatagramPacket(data, data.length, address, LAMP_PORT);
                socket.send(packet);
            } catch (Exception e) {
                Log.e(TAG, "Error sending command: " + command, e);
            }
        }).start();
    }

    public void startListening() {
        if (listenerThread != null && listenerThread.isAlive()) return;
        isListening = true;

        listenerThread = new Thread(() -> {
            DatagramSocket socket = null;
            long lastResponseTime = System.currentTimeMillis();
            long lastPingTime = 0;

            try {
                socket = new DatagramSocket();
                socket.setSoTimeout(500);

                while (isListening) {
                    if (currentIp.isEmpty()) {
                        Thread.sleep(500);
                        continue;
                    }

                    if (cachedAddress == null) {
                        try {
                            cachedAddress = InetAddress.getByName(currentIp);
                        } catch (Exception e) {
                            Log.e(TAG, "IP error: " + currentIp);
                        }
                    }

                    long now = System.currentTimeMillis();

                    // 1. ПЕРІОДИЧНЕ ОПИТУВАННЯ (кожні 2 сек)
                    if (now - lastPingTime > 2000 && cachedAddress != null) {
                        byte[] data = "GET".getBytes();
                        socket.send(new DatagramPacket(data, data.length, cachedAddress, LAMP_PORT));
                        lastPingTime = now;
                    }

                    // 2. ПРИЙОМ ДАНИХ
                    try {
                        byte[] buf = new byte[1024];
                        DatagramPacket packet = new DatagramPacket(buf, buf.length);
                        socket.receive(packet);
                        String msg = new String(packet.getData(), 0, packet.getLength());
                        
                        lastResponseTime = System.currentTimeMillis();
                        if (listener != null) {
                            new Handler(Looper.getMainLooper()).post(() -> listener.onMessageReceived(msg));
                        }
                    } catch (SocketTimeoutException e) {
                        // Перевіряємо, чи не задовго лампа мовчить
                        if (System.currentTimeMillis() - lastResponseTime > 5000) {
                            if (listener != null) {
                                new Handler(Looper.getMainLooper()).post(() -> listener.onConnectionLost());
                            }
                            lastResponseTime = System.currentTimeMillis(); // Запобігаємо спаму помилками
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Listener error", e);
            } finally {
                if (socket != null) socket.close();
            }
        });
        listenerThread.start();
    }

    public void stopListening() {
        isListening = false;
    }
}
