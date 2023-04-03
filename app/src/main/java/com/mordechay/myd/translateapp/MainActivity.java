package com.mordechay.myd.translateapp;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.io.File;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int PICKFILE_RESULT_CODE = 1;
    private EditText edtFrom;
    private EditText edtTo;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        edtFrom = findViewById(R.id.edt_from);
        edtTo = findViewById(R.id.edt_to);
        findViewById(R.id.btn_select_file).setOnClickListener(this);
        findViewById(R.id.btn_translate).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {

    }

    public void pickFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("text/xml");
        ActivityResultLauncher<String> startForResult = registerForActivityResult(new ActivityResultContracts.GetContent(),
                new ActivityResultCallback<Uri>() {
                    @Override
                    public void onActivityResult(Uri uri) {
                        // Handle the returned Uri
                        File selectedFile = new File(uri.getPath());
                        parserXml(selectedFile);
                    }
                });
        startForResult.launch(intent.);
    }

    private void parserXml(File selectedFile) {

    }
}