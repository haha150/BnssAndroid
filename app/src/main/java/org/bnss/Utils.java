package org.bnss;

import android.content.Context;
import android.util.Base64;

import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
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
        whoami = "ChunHeng Jen";
        //whoami = "Ali Symeri";
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
                keyStore2.load(context.getResources().openRawResource(R.raw.employee3keystore), "password".toCharArray());

                KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                kmf.init(keyStore2, "david".toCharArray());
                KeyManager[] keyManagers = kmf.getKeyManagers();

                PKCS8EncodedKeySpec  spec = new PKCS8EncodedKeySpec(IOUtils.toByteArray(context.getResources().openRawResource(R.raw.employee3privatekey)));
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

    public String getWhoami() {
        return whoami;
    }
}
