package org.bnss;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

public class MainActivity extends AppCompatActivity {

    private HostnameVerifier hostnameVerifier = new HostnameVerifier() {
        @Override
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    };
    private Utils utils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        utils = Utils.getInstance(getApplicationContext());
        sendHttpsRequest();
        //sendHttpsRequest2();
        //performFileSearch();
    }

    public void performFileSearch() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, 42);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (requestCode == 42 && resultCode == Activity.RESULT_OK) {
            Uri uri = null;
            if (resultData != null) {
                uri = resultData.getData();
                Log.i("FILE SELECT", "Uri: " + uri.toString());
            }
        }
    }

    public void sendHttpsRequest() {
        try {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .permitAll().build();
            StrictMode.setThreadPolicy(policy);

            URL url = new URL("https://192.168.1.169:8443/rest/users");
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setSSLSocketFactory(utils.getSslContext().getSocketFactory());
            conn.setHostnameVerifier(hostnameVerifier);

            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestMethod("GET");

            InputStream in = conn.getInputStream();
            String json = utils.getStringFromInputStream(in);
            System.out.println(json);
            Gson gson = new Gson();
            List<User> users = gson.fromJson(json, new TypeToken<List<User>>(){}.getType());
            for(User u : users) {
                System.out.println(u.getUsername());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendHttpsRequest2() {
        try {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .permitAll().build();
            StrictMode.setThreadPolicy(policy);

            URL url = new URL("https://192.168.1.169:8443/rest/file/add");
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setSSLSocketFactory(utils.getSslContext().getSocketFactory());
            conn.setHostnameVerifier(hostnameVerifier);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/text");
            conn.setRequestMethod("PUT");
            Gson gson = new Gson();
            Data d = new Data();
            d.setFrom("me");
            d.setRecipient("you");
            d.setFile(new byte[]{1});
            d.setHash(new byte[]{2});
            d.setKey(new byte[]{3});
            d.setName("file");
            OutputStream os = conn.getOutputStream();
            os.write(gson.toJson(d).getBytes());
            os.flush();
            InputStream in = conn.getInputStream();
            String json = utils.getStringFromInputStream(in);
            System.out.println(json);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
