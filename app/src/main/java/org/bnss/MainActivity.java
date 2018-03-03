package org.bnss;

import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.google.android.gms.security.ProviderInstaller;

import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initCert();

        new Thread(new Runnable() {
            @Override
            public void run() {
                URL url = null;
                try {

                    /*String url2 = "https://130.229.148.189:8443/users";
                    RestTemplate restTemplate = new RestTemplate();
                    restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
                    String a = restTemplate.getForObject(url2, String.class);
                    System.out.println(a);*/


                    /*url = new URL("https://130.229.148.189:8443/users");
                    URLConnection urlConnection = url.openConnection();
                    InputStream in = urlConnection.getInputStream();
                    System.out.println(in);*/
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();


    }

    public void initCert() {
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            InputStream caInput = getResources().openRawResource(R.raw.server);
            X509Certificate ca;
            try {
                ca = (X509Certificate) cf.generateCertificate(caInput);
                System.out.println("ca=" + ca.getSubjectDN());

                // Create a KeyStore containing our trusted CAs
                String keyStoreType = KeyStore.getDefaultType();
                KeyStore keyStore = KeyStore.getInstance(keyStoreType);
                keyStore.load(null);
                keyStore.setCertificateEntry("ca", ca);

                KeyStore keyStore2 = KeyStore.getInstance("BKS");
                keyStore2.load(getResources().openRawResource(R.raw.test2), "password".toCharArray());

                KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                kmf.init(keyStore2, "david".toCharArray());
                KeyManager[] keyManagers = kmf.getKeyManagers();

                // Create a TrustManager that trusts the CAs in our KeyStore
                String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
                tmf.init(keyStore);

                // Create an SSLContext that uses our TrustManager
                SSLContext context = SSLContext.getInstance("TLS");
                context.init(keyManagers, tmf.getTrustManagers(), null);



                StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                        .permitAll().build();
                StrictMode.setThreadPolicy(policy);

                URL url = new URL("https://130.229.148.189:8443/");
                HttpsURLConnection urlConnection =
                        (HttpsURLConnection)url.openConnection();
                urlConnection.setSSLSocketFactory(context.getSocketFactory());
                //urlConnection.setSSLSocketFactory(noSSLv3Factory);
                urlConnection.setHostnameVerifier(hostnameVerifier);
                InputStream in = urlConnection.getInputStream();
                System.out.println(getStringFromInputStream(in));

                //copyInputStreamToOutputStream(in, System.out);
            } finally {
                caInput.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    HostnameVerifier hostnameVerifier = new HostnameVerifier() {
        @Override
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    };

    private String getStringFromInputStream(InputStream is) {

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
}
