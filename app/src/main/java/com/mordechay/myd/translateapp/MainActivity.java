package com.mordechay.myd.translateapp;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int PICKFILE_RESULT_CODE = 1;
    private EditText edtFrom;
    private EditText edtTo;
    private ActivityResultLauncher<Intent> getSelectedFileActivityResult;
    private String skippedChar = "Cellular";
    String[][] arr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSelectedFileActivityResult = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // There are no request codes
                        Uri fileUri = null;
                        if (result.getData() != null) {
                            fileUri = result.getData().getData();
                            parserXml(fileUri);
                        }else {

                        }

                    }
                });

        edtFrom = findViewById(R.id.edt_from);
        edtTo = findViewById(R.id.edt_to);
        findViewById(R.id.btn_select_file).setOnClickListener(this);
        findViewById(R.id.btn_translate).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btn_select_file:
                pickFile();
                break;
            case R.id.btn_translate:
                saveXmlTranslate();
                break;
        }

    }

    public void pickFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("text/xml");

        getSelectedFileActivityResult.launch(intent);
    }

    private void parserXml(Uri selectedFile) {
        edtFrom.setText("מפענח קובץ");
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    InputStream inputStream = getContentResolver().openInputStream(selectedFile);
                    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                    Document doc = dBuilder.parse(inputStream);
                    doc.getDocumentElement().normalize();
                    NodeList nList = doc.getElementsByTagName("string");
                    arr = new String[nList.getLength()][];
                    String content = "";
                    StringBuilder allContent = new StringBuilder();
                    String[] arrName = new String[nList.getLength()];
                    for (int i = 0; i < nList.getLength(); i++) {
                        Node nNode = nList.item(i);
                        content = nNode.getTextContent().trim();
                        allContent.append(nNode.getTextContent().trim()).append("\n");
                        // חיפוש והוספת הערך של ה-attribut name למערך
                        if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                            Element element = (Element) nNode;
                            arrName[i] = element.getAttribute("name");
                        }

                        if (skippedChar != null && !skippedChar.isEmpty()) {
                            String[] splittedContent = content.split(Pattern.quote(skippedChar));
                            if (splittedContent.length > 1) {
                                ArrayList<String> splittedContentList = new ArrayList<>();
                                for (String s : splittedContent) {
                                    if (!s.isEmpty()) {
                                        splittedContentList.add(s);
                                    }
                                }
                                arr[i] = splittedContentList.toArray(new String[0]);
                            } else {
                                arr[i] = new String[]{content};
                            }
                        } else {
                            arr[i] = new String[]{content};
                        }
                    }

                        String finalContent = allContent.toString();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            edtFrom.setText(finalContent);
                            if(BuildConfig.DEBUG) {
                                for (int i = 0; i < arrName.length; i++) {
                                    StringBuilder strBody = new StringBuilder("name=  " + arrName[i] + "  value=  ");
                                    for (int b = 0; b < arr[i].length; b++) {
                                       strBody.append(arr[i][b]).append("\n");
                                    }
                                    Log.d(this.getClass().getName(), strBody.toString());
                                }
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }


    private void saveXmlTranslate() {

    }
}