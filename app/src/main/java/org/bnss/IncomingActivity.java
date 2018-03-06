package org.bnss;

import android.os.Environment;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.security.Key;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

public class IncomingActivity extends AppCompatActivity {

    private TextView from;
    private Spinner spinner;
    private List<String> spinnerArray;
    private String selectedFile;
    private Utils utils;
    private List<ReceiveData> files;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incoming);
        files = new ArrayList<>();
        utils = Utils.getInstance(getApplicationContext());
        utils.getIncomingFiles(files);
        from = findViewById(R.id.fromText);

        spinner = findViewById(R.id.spinner3);
        spinnerArray =  new ArrayList<>();
        for(ReceiveData d : files) {
            spinnerArray.add(d.getName());
        }

        adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, spinnerArray);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                selectedFile = spinnerArray.get(i);
                for(ReceiveData d : files) {
                    if(d.getName().equals(selectedFile)) {
                        from.setText("From: " + d.getFrom());
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        Button download = findViewById(R.id.downloadButton);
        download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                for(ReceiveData d : files) {
                    if(d.getName().equals(selectedFile)) {
                        if(utils.downloadFile(d)) {
                            spinnerArray.remove(selectedFile);
                            spinner.setAdapter(adapter);
                            from.setText("From: ");
                            break;
                        }

                    }
                }
            }
        });
    }


}
