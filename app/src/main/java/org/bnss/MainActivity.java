package org.bnss;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.security.Key;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
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
    private Spinner spinner;
    private List<String> spinnerArray;
    private String selectedRecipient;
    private File file;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestRead();
        spinner = findViewById(R.id.spinner2);
        spinnerArray =  new ArrayList<>();
        utils = Utils.getInstance(getApplicationContext());
        getUsers();
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
                if(file == null) {
                    makeToast("Select file first");
                    return;
                }
                if(selectedRecipient == null) {
                    makeToast("Select recipient first");
                    return;
                }
                sendFile();
                file = null;
            }
        });

        getIncomingFiles();
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
            if (resultData != null) {
                Uri uri = resultData.getData();
                file = new File(FilePath.getPath(getApplicationContext() ,uri));
            }
        }
    }

    public void requestRead() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    1);
        }
    }

    public void getUsers() {
        try {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .permitAll().build();
            StrictMode.setThreadPolicy(policy);

            URL url = new URL("https://192.168.1.239:8443/rest/users");
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
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendFile() {
                try {
                    PublicKey pubKey = getPublicKey(selectedRecipient);
                    SecretKey symKey = utils.generateSymmetricKey();
                    byte[] fileArr = null;
                    byte[] hashedFile = null;
                    try {
                        fileArr = FileUtils.readFileToByteArray(file);
                        hashedFile = utils.hashFile(fileArr);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    byte[] encryptedFile = utils.encryptFile(fileArr, symKey);
                    byte[] encryptedSymKey = utils.encryptSymmetricKey(symKey, pubKey);
                    byte[] encryptedHash = utils.encryptHash(hashedFile);

                    StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                            .permitAll().build();
                    StrictMode.setThreadPolicy(policy);

                    URL url = new URL("https://192.168.1.239:8443/rest/file/add");
                    HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                    conn.setSSLSocketFactory(utils.getSslContext().getSocketFactory());
                    conn.setHostnameVerifier(hostnameVerifier);
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setRequestProperty("Accept", "application/text");
                    conn.setRequestMethod("PUT");
                    Gson gson = new Gson();
                    SendData d = new SendData();
                    d.setFrom(utils.getWhoami());
                    d.setRecipient(selectedRecipient);
                    d.setFile(encryptedFile);
                    d.setHash(encryptedHash);
                    d.setKey(encryptedSymKey);
                    d.setName(file.getName());
                    OutputStream os = conn.getOutputStream();
                    os.write(gson.toJson(d).getBytes());
                    os.flush();
                    InputStream in = conn.getInputStream();
                    String text = utils.getStringFromInputStream(in);
                    System.out.println(text);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            makeToast("Successfully sent file");
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            makeToast("Failed to send file");
                        }
                    });
                }

    }

    public PublicKey getPublicKey(String selectedRecipient) {
        try {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .permitAll().build();
            StrictMode.setThreadPolicy(policy);
            String oldLink = "https://192.168.1.239:8443/rest/certificate/" + selectedRecipient;
            String link = oldLink.replaceAll(" ", "%20");
            URL url = new URL(link);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setSSLSocketFactory(utils.getSslContext().getSocketFactory());
            conn.setHostnameVerifier(hostnameVerifier);

            conn.setRequestMethod("GET");
            File f = new File(getFilesDir(),"pubkey.der");
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
                X509Certificate recipient = (X509Certificate) cf.generateCertificate(caInput);
                return recipient.getPublicKey();
            } catch (Exception e) {
                System.out.println("failed");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void getIncomingFiles() {
        try {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .permitAll().build();
            StrictMode.setThreadPolicy(policy);

            String oldLink = "https://192.168.1.239:8443/rest/files/" + utils.getWhoami();
            String link = oldLink.replaceAll(" ", "%20");
            URL url = new URL(link);
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
            List<ReceiveData> datas = gson.fromJson(json, new TypeToken<List<ReceiveData>>(){}.getType());
            for(ReceiveData d : datas) {
                byte[] decodedHash = Base64.decode(d.getHash(),0);
                byte[] decodedSymKey = Base64.decode(d.getKey(),0);
                byte[] decodedFile = Base64.decode(d.getFile(),0);
                PublicKey pubKey = getPublicKey(d.getFrom());
                byte[] decryptedHash = utils.decryptHash(decodedHash, pubKey);
                Key decryptedSymKey = utils.decryptSymmetricKey(decodedSymKey);
                byte[] decryptedFile = utils.decryptFile(decodedFile, decryptedSymKey);
                byte[] hashedFile = utils.hashFile(decryptedFile);

                if(!Arrays.equals(decryptedHash, hashedFile)) {
                    makeToast("File hash is invalid, skipping save file");
                    return;
                }
                File file = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS), d.getName());
                FileUtils.writeByteArrayToFile(file, decryptedFile);
                makeToast("Saved file in downloads: " + file.getName());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void makeToast(String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }
}
