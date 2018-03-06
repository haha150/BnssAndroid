package org.bnss;

import android.content.Context;
import android.os.Environment;
import android.os.StrictMode;
import android.util.Base64;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManagerFactory;

public class Utils {

    private static Utils utils;
    private Context context;
    private SSLContext sslContext;
    private PrivateKey privateKey;
    private KeyFactory kf;
    private String whoami;

    private Utils(Context c) {
        this.context = c;
        initCerts();
        //whoami = "ChunHeng Jen";
        whoami = "Ali Symeri";
        //whoami = "Farhad Zareafifi";
    }

    public static Utils getInstance(Context c) {
        if(utils == null) {
            utils = new Utils(c);
        }
        return utils;
    }

    public SSLContext getSslContext() {
        return sslContext;
    }

    private void initCerts() {
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            InputStream caInput = context.getResources().openRawResource(R.raw.signingca);
            try {
                X509Certificate ca = (X509Certificate) cf.generateCertificate(caInput);

                // Create a KeyStore containing our trusted CAs
                String keyStoreType = KeyStore.getDefaultType();
                KeyStore keyStore = KeyStore.getInstance(keyStoreType);
                keyStore.load(null);
                keyStore.setCertificateEntry("ca", ca);

                // Create a TrustManager that trusts the CAs in our KeyStore
                String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
                tmf.init(keyStore);

                KeyStore keyStore2 = KeyStore.getInstance("PKCS12");
                keyStore2.load(context.getResources().openRawResource(R.raw.employee1keystore), "password".toCharArray());

                KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                kmf.init(keyStore2, "david".toCharArray());
                KeyManager[] keyManagers = kmf.getKeyManagers();

                PKCS8EncodedKeySpec  spec = new PKCS8EncodedKeySpec(IOUtils.toByteArray(context.getResources().openRawResource(R.raw.employee1privatekey)));
                kf = KeyFactory.getInstance("RSA");
                privateKey = kf.generatePrivate(spec);

                // Create an SSLContext that uses our TrustManager
                sslContext = SSLContext.getInstance("TLS");
                sslContext.init(keyManagers, tmf.getTrustManagers(), null);
            } finally {
                caInput.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public byte[] encryptFile(byte[] file, Key key) {
        try {
            Cipher cipher = Cipher.getInstance(key.getAlgorithm());
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return cipher.doFinal(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public byte[] decryptFile(byte[] file, Key key) {
        try {
            Cipher cipher = Cipher.getInstance(key.getAlgorithm());
            cipher.init(Cipher.DECRYPT_MODE, key);
            return cipher.doFinal(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public byte[] encryptSymmetricKey(SecretKey key, Key recipient) {
        try {
            Cipher cipher = Cipher.getInstance(recipient.getAlgorithm());
            cipher.init(Cipher.WRAP_MODE, recipient);
            return cipher.wrap(key);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Key decryptSymmetricKey(byte[] key) {
        try {
            Cipher cipher = Cipher.getInstance(kf.getAlgorithm());
            cipher.init(Cipher.UNWRAP_MODE, privateKey);
            return cipher.unwrap(key, "AES", Cipher.SECRET_KEY);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public SecretKey generateSymmetricKey() {
        KeyGenerator keyGen = null;
        try {
            keyGen = KeyGenerator.getInstance("AES");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        keyGen.init(256);
        return keyGen.generateKey();
    }

    public byte[] encryptHash(byte[] hash) {
        try {
            Cipher cipher = Cipher.getInstance(kf.getAlgorithm());
            cipher.init(Cipher.ENCRYPT_MODE, privateKey);
            return cipher.doFinal(hash);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public byte[] decryptHash(byte[] hash, PublicKey pubKey) {
        try {
            Cipher cipher = Cipher.getInstance(pubKey.getAlgorithm());
            cipher.init(Cipher.DECRYPT_MODE, pubKey);
            return cipher.doFinal(hash);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public byte[] hashFile(byte[] file) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(file);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getStringFromInputStream(InputStream is) {

        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();

        String line;
        try {

            br = new BufferedReader(new InputStreamReader(is));
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return sb.toString();
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
            conn.setSSLSocketFactory(getSslContext().getSocketFactory());
            conn.setHostnameVerifier(hostnameVerifier);

            conn.setRequestMethod("GET");
            File f = new File(context.getFilesDir(),"pubkey.der");
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

    public void getUsers(List<String> spinnerArray) {
        try {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .permitAll().build();
            StrictMode.setThreadPolicy(policy);

            URL url = new URL("https://192.168.1.239:8443/rest/users");
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setSSLSocketFactory(getSslContext().getSocketFactory());
            conn.setHostnameVerifier(hostnameVerifier);

            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestMethod("GET");

            InputStream in = conn.getInputStream();
            String json = getStringFromInputStream(in);
            System.out.println(json);

            Gson gson = new Gson();
            List<User> users = gson.fromJson(json, new TypeToken<List<User>>(){}.getType());
            for(User u : users) {
                if(!u.getUsername().equals(whoami)) {
                    spinnerArray.add(u.getUsername());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendFile(String selectedRecipient, File file) {
        try {
            PublicKey pubKey = getPublicKey(selectedRecipient);
            SecretKey symKey = generateSymmetricKey();
            byte[] fileArr = null;
            byte[] hashedFile = null;
            try {
                fileArr = FileUtils.readFileToByteArray(file);
                hashedFile = hashFile(fileArr);
            } catch (IOException e) {
                e.printStackTrace();
            }
            byte[] encryptedFile = encryptFile(fileArr, symKey);
            byte[] encryptedSymKey = encryptSymmetricKey(symKey, pubKey);
            byte[] encryptedHash = encryptHash(hashedFile);

            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .permitAll().build();
            StrictMode.setThreadPolicy(policy);

            URL url = new URL("https://192.168.1.239:8443/rest/file/add");
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setSSLSocketFactory(getSslContext().getSocketFactory());
            conn.setHostnameVerifier(hostnameVerifier);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/text");
            conn.setRequestMethod("PUT");
            Gson gson = new Gson();
            SendData d = new SendData();
            d.setFrom(whoami);
            d.setRecipient(selectedRecipient);
            d.setFile(encryptedFile);
            d.setHash(encryptedHash);
            d.setKey(encryptedSymKey);
            d.setName(file.getName());
            OutputStream os = conn.getOutputStream();
            os.write(gson.toJson(d).getBytes());
            os.flush();
            InputStream in = conn.getInputStream();
            String text = getStringFromInputStream(in);
            makeToast("Successfully sent file");
        } catch (Exception e) {
            e.printStackTrace();
            makeToast("Failed to send file");
        }
    }

    public void getIncomingFiles(List<ReceiveData> files) {
        try {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .permitAll().build();
            StrictMode.setThreadPolicy(policy);

            String oldLink = "https://192.168.1.239:8443/rest/files/" + whoami;
            String link = oldLink.replaceAll(" ", "%20");
            URL url = new URL(link);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setSSLSocketFactory(getSslContext().getSocketFactory());
            conn.setHostnameVerifier(hostnameVerifier);

            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestMethod("GET");

            InputStream in = conn.getInputStream();
            String json = getStringFromInputStream(in);

            Gson gson = new Gson();
            List<ReceiveData> datas = gson.fromJson(json, new TypeToken<List<ReceiveData>>(){}.getType());
            files.addAll(datas);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean downloadFile(ReceiveData d) {
        try {
            byte[] decodedHash = Base64.decode(d.getHash(),0);
            byte[] decodedSymKey = Base64.decode(d.getKey(),0);
            byte[] decodedFile = Base64.decode(d.getFile(),0);
            PublicKey pubKey = getPublicKey(d.getFrom());
            byte[] decryptedHash = decryptHash(decodedHash, pubKey);
            Key decryptedSymKey = decryptSymmetricKey(decodedSymKey);
            byte[] decryptedFile = decryptFile(decodedFile, decryptedSymKey);
            byte[] hashedFile = hashFile(decryptedFile);
            if(!Arrays.equals(decryptedHash, hashedFile)) {
                makeToast("File hash is invalid, skipping save file");
                return false;
            }
            File file = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS), d.getName());
            FileUtils.writeByteArrayToFile(file, decryptedFile);
            makeToast("Saved file in downloads: " + file.getName());
            deleteFile(d.getId());
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void deleteFile(Long id) {
        try {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .permitAll().build();
            StrictMode.setThreadPolicy(policy);

            URL url = new URL("https://192.168.1.239:8443/rest/file/delete/" + id);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setSSLSocketFactory(getSslContext().getSocketFactory());
            conn.setHostnameVerifier(hostnameVerifier);

            conn.setRequestProperty("Accept", "application/text");
            conn.setRequestMethod("POST");

            InputStream in = conn.getInputStream();
            String json = getStringFromInputStream(in);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void makeToast(String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

    private HostnameVerifier hostnameVerifier = new HostnameVerifier() {
        @Override
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    };
}
