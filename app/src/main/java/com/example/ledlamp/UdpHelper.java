package com.example.ledlamp;

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

    // Інтерфейс, щоб передавати повідомлення назад у MainActivity
    public interface UdpListener {
        void onMessageReceived(String message);
    }

    private UdpListener listener;

    public void setListener(UdpListener listener) {
        this.listener = listener;
    }

    public void setIp(String ip) {
        this.currentIp = ip;
    }

    // Відправка команди (без відповіді)
    public void sendCommand(String command) {
        if (currentIp.isEmpty()) return;
        new Thread(() -> {
            try {
                DatagramSocket socket = new DatagramSocket();
                byte[] data = command.getBytes();
                InetAddress address = InetAddress.getByName(currentIp);
                DatagramPacket packet = new DatagramPacket(data, data.length, address, LAMP_PORT);
                socket.send(packet);
                socket.close();
            } catch (Exception e) {
                Log.e(TAG, "Error sending command: " + command, e);
            }
        }).start();
    }

    // Запуск слухача + Heartbeat (все в одному)
    public void startListening() {
        if (listenerThread != null && listenerThread.isAlive()) return;
        isListening = true;

        listenerThread = new Thread(() -> {
            DatagramSocket socket = null;
            try {
                socket = new DatagramSocket();
                socket.setSoTimeout(100);
                long lastPingTime = 0;
                InetAddress address = null;

                while (isListening) {
                    if (!currentIp.isEmpty() && address == null) {
                        try {
                            address = InetAddress.getByName(currentIp);
                        } catch (Exception e) {
                            Log.e(TAG, "Error resolving IP: " + currentIp, e);
                        }
                    }

                    // 1. ПІНГ
                    long now = System.currentTimeMillis();
                    if (now - lastPingTime > 2000) {
                        if (address != null) {
                            try {
                                byte[] data = "GET".getBytes();
                                DatagramPacket packet = new DatagramPacket(data, data.length, address, LAMP_PORT);
                                socket.send(packet);
                            } catch (Exception e) {
                                Log.e(TAG, "Error sending ping", e);
                            }
                        }
                        lastPingTime = now;
                    }

                    // 2. ПРИЙОМ
                    try {
                        byte[] buf = new byte[1024];
                        DatagramPacket packet = new DatagramPacket(buf, buf.length);
                        socket.receive(packet);
                        String msg = new String(packet.getData(), 0, packet.getLength());

                        // Передаємо повідомлення в Activity
                        if (listener != null) {
                            listener.onMessageReceived(msg);
                        }
                    } catch (SocketTimeoutException e) {
                        // Тиша - це ок
                    } catch (Exception e) {
                        Log.e(TAG, "Error receiving packet", e);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in listener thread", e);
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
