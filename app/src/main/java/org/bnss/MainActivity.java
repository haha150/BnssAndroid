package org.bnss;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.apache.commons.io.FileUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

import static java.nio.charset.Charset.forName;

public class MainActivity extends AppCompatActivity {

    private HostnameVerifier hostnameVerifier = new HostnameVerifier() {
        @Override
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    };

    private Utils utils;
    private Spinner spinner;
    private List<String> spinnerArray;
    private String selectedRecipient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        spinner = findViewById(R.id.spinner2);
        spinnerArray =  new ArrayList<>();
        utils = Utils.getInstance(getApplicationContext());
        sendHttpsRequest();
        //sendHttpsRequest2();
        //performFileSearch();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, spinnerArray);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                selectedRecipient = spinnerArray.get(i);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        Button selectFile = findViewById(R.id.selectFileButton);
        selectFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                performFileSearch();
            }
        });

        Button sendFile = findViewById(R.id.sendButton);
        sendFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendHttpsRequest4();
            }
        });
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
                System.out.println(uri.toString());
            }
        }
    }

    public void sendHttpsRequest() {
        try {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .permitAll().build();
            StrictMode.setThreadPolicy(policy);

            URL url = new URL("https://10.68.90.142:8443/rest/users");
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
                spinnerArray.add(u.getUsername());
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

            URL url = new URL("https://10.68.90.142:8443/rest/file/add");
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setSSLSocketFactory(utils.getSslContext().getSocketFactory());
            conn.setHostnameVerifier(hostnameVerifier);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/text");
            conn.setRequestMethod("PUT");
            Gson gson = new Gson();
            Data2 d = new Data2();
            d.setFrom("Ali Symeri");
            d.setRecipient(selectedRecipient);
            byte[] arr = utils.generateSymmetricKey().getEncoded();
            System.out.println(Base64.encodeToString(arr, Base64.DEFAULT));
            d.setFile(utils.generateSymmetricKey().getEncoded());
            d.setHash(arr);
            d.setKey(utils.generateSymmetricKey().getEncoded());
            d.setName("file name");
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

    public void sendHttpsRequest3() {
        try {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .permitAll().build();
            StrictMode.setThreadPolicy(policy);

            URL url = new URL("https://10.68.90.142:8443/rest/filess/ab");
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setSSLSocketFactory(utils.getSslContext().getSocketFactory());
            conn.setHostnameVerifier(hostnameVerifier);

            conn.setRequestMethod("GET");
            File f = new File(getFilesDir(),"test.der");
            InputStream inputStream = conn.getInputStream();
            FileOutputStream outputStream = new FileOutputStream(f);

            int bytesRead;
            byte[] buffer = new byte[4096];
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.close();
            inputStream.close();

            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            InputStream caInput = new FileInputStream(f);
            try {
                X509Certificate ca = (X509Certificate) cf.generateCertificate(caInput);
                System.out.println(ca);
            } catch (Exception e) {
                System.out.println("failed");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendHttpsRequest4() {
        try {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .permitAll().build();
            StrictMode.setThreadPolicy(policy);

            URL url = new URL("https://10.68.90.142:8443/rest/files/ChunHeng%20Jen");
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
            List<Data> datas = gson.fromJson(json, new TypeToken<List<Data>>(){}.getType());
            for(Data d : datas) {
                byte[] decodedKey = Base64.decode(d.getHash(),0);
                //SecretKey originalKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
                System.out.println(Base64.encodeToString(decodedKey, Base64.DEFAULT));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
