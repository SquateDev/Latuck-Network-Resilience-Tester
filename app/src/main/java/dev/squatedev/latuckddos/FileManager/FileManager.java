/*________________________________________________*//*


               Author: @Squatedev
               age: 19
               name: null


/*________________________________________________*/

package dev.squatedev.latuckddos.FileManager;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class FileManager {
    private static FileManager instance;
    private Context context;
    private String tag = "LatuckDDos";

    private FileManager(Context context){
        if(context!=null) this.context = context;
    }

    public static synchronized FileManager getInstance(Context ctx){
        if(instance==null) instance = new FileManager(ctx);
        return instance;
    }

    private File getProxyFile(String fileName) {
        File folder = new File(Environment.getExternalStorageDirectory(), "LatuckDDOS");
        if (!folder.exists()) {
            folder.mkdirs();
        }
        return new File(folder, fileName + ".json");
    }

    public void addProxy(String fileName, String ip, String port) {
        try {
            File file = getProxyFile(fileName);
            JSONObject root;
            JSONArray proxies;

            if (file.exists()) {
                FileInputStream fis = new FileInputStream(file);
                BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();
                root = new JSONObject(sb.toString());
                proxies = root.getJSONArray("proxies");
            } else {
                root = new JSONObject();
                proxies = new JSONArray();
                root.put("proxies", proxies);
            }

            boolean exists = false;
            for (int i = 0; i < proxies.length(); i++) {
                JSONObject p = proxies.getJSONObject(i);
                if (p.getString("ip").equals(ip) && p.getString("port").equals(port)) {
                    exists = true;
                    break;
                }
            }

            if (!exists) {
                JSONObject newProxy = new JSONObject();
                newProxy.put("ip", ip);
                newProxy.put("port", port);
                newProxy.put("type", "http");
                proxies.put(newProxy);

                FileOutputStream fos = new FileOutputStream(file);
                fos.write(root.toString(4).getBytes());
                fos.close();
            }

        } catch (Exception e) {
            Log.e(tag, "Error adding proxy: " + e.getMessage());
        }
    }

    public String loadProxies(String fileName) {
        try {
            File file = getProxyFile(fileName);
            if (!file.exists()) {
                JSONObject root = new JSONObject();
                root.put("proxies", new JSONArray());
                return root.toString();
            }

            FileInputStream fis = new FileInputStream(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            return sb.toString();

        } catch (Exception e) {
            Log.e(tag, "Error loading proxies: " + e.getMessage());
            JSONObject root = new JSONObject();
            try {
                root.put("proxies", new JSONArray());
            } catch (Exception ex) {}
            return root.toString();
        }
    }

    public List<String> getProxyList(String fileName) {
        List<String> list = new ArrayList<>();
        try {
            String json = loadProxies(fileName);
            JSONObject root = new JSONObject(json);
            JSONArray proxies = root.getJSONArray("proxies");

            for (int i = 0; i < proxies.length(); i++) {
                JSONObject proxy = proxies.getJSONObject(i);
                String ip = proxy.getString("ip");
                String port = proxy.getString("port");
                list.add(ip + ":" + port);
            }
        } catch (Exception e) {
            Log.e(tag, "Error getting proxy list: " + e.getMessage());
        }
        return list;
    }

    public List<String> getJsonFiles() {
        List<String> files = new ArrayList<>();
        try {
            File folder = new File(Environment.getExternalStorageDirectory(), "LatuckDDOS");
            if (folder.exists() && folder.isDirectory()) {
                File[] listFiles = folder.listFiles();
                if (listFiles != null) {
                    for (File file : listFiles) {
                        if (file.isFile() && file.getName().endsWith(".json")) {
                            String name = file.getName();
                            files.add(name.substring(0, name.length() - 5));
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(tag, "Error getting json files: " + e.getMessage());
        }
        return files;
    }
}