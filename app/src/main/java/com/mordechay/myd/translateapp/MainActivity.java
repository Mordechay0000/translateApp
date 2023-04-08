package com.mordechay.myd.translateapp;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.PrecomputedText;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String FILE_TYPE = "text/xml";
    private static final String REPLACEMENT_CHARACTER = "@@@";
    private Button btnSaveTranslate;
    private TextView edtFrom;
    private EditText edtTo;
    private ActivityResultLauncher<Intent> fileActivityResult;
    private static ArrayList<String> skippedChar;
    private String nameAttribute;
    private String content;
    private ProgressBar progressBar;
    private boolean isSave;
    //private static int[] locationSkippedChar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fileActivityResult = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // There are no request codes
                        Uri fileUri;
                        if (result.getData() != null) {
                            if (!isSave) {
                                fileUri = result.getData().getData();
                                parserXml(fileUri);
                            }else {
                                fileUri = result.getData().getData();
                                saveXmlTranslate(fileUri);
                            }
                        } else {
                            Toast.makeText(this, getText(R.string.no_selected_file_or_directory), Toast.LENGTH_SHORT).show();
                        }

                    }
                });

        skippedChar = new ArrayList<>(getSharedPreferences("save_data", 0).getStringSet("skipped_chars", new HashSet<>(Arrays.asList("%s", "%1$s", "%2$s", "%3$s", "%4$s", "%5$s", "%6$s", "%7$s", "%8$s", "%9$s", "%d", "%1$d", "%2$d", "%3$d", "%4$d", "%5$d", "%6$d", "%7$d", "%8$d", "%9$d"))));
        progressBar = findViewById(R.id.progress_bar);
        progressBar.setMax(100);
        progressBar.setProgress(0);
        progressBar.setVisibility(View.VISIBLE);
        edtFrom = findViewById(R.id.edt_from);
        edtTo = findViewById(R.id.edt_to);
        findViewById(R.id.btn_manger_skipped_chars).setOnClickListener(this);
        findViewById(R.id.btn_select_file).setOnClickListener(this);
        btnSaveTranslate = findViewById(R.id.btn_translate);
        btnSaveTranslate.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id ==R.id.btn_manger_skipped_chars) {

        } else if (id == R.id.btn_select_file) {
            pickFile(false);
        } else if (id == R.id.btn_translate) {
            pickFile(true);
        }

    }

    public void pickFile(boolean isSave) {
        this.isSave = isSave;
        Intent intent = new Intent(!isSave ? Intent.ACTION_GET_CONTENT : Intent.ACTION_CREATE_DOCUMENT);
        intent.setType(FILE_TYPE);
        if (!isSave) {
            fileActivityResult.launch(intent);
        } else {
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.putExtra(Intent.EXTRA_TITLE, "strings.xml");
            fileActivityResult.launch(intent);
        }
    }


    private static String replacementSkippedChars(String content, boolean isParsing) {
        //locationSkippedChar = findString(content, skippedChar);
        for (int i = 0; i < skippedChar.size(); i++) {
            content = content.replace(!isParsing ? skippedChar.get(i) : REPLACEMENT_CHARACTER + i, !isParsing ? REPLACEMENT_CHARACTER + i : skippedChar.get(i));
        }
        return content;
    }

    /*
    for save location skipped char's method
    @NonNull
    private static int[] findString(String str, String[] arrSearchStr) {
        int[] positions = new int[str.length()];
        int removedChars = 0;
        int i = 0;
        for (String searchChar: arrSearchStr) {
            int pos = str.indexOf(searchChar);
            while (pos != -1) {
                positions[i++] = pos;
                pos = str.indexOf(searchChar, pos + 1) - removedChars;
                removedChars += searchChar.length();
            }
        }
        return Arrays.copyOf(positions, i);
    }

    */


    private void saveXmlTranslate(Uri UriSave) {
        content = edtTo.getText().toString();
        content = replacementSkippedChars(content, true);
        String[] arrNameAttribute = nameAttribute.split("\n");
        String[] arrContent = content.toString().split("\n");
        createdTranslateXml(arrNameAttribute, arrContent, UriSave);
    }


    private void parserXml(Uri selectedFile) {
        edtFrom.setText(R.string.file_parser);
        new Thread(() -> {
            try {
                InputStream inputStream = getContentResolver().openInputStream(selectedFile);
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                Document doc = dBuilder.parse(inputStream);
                doc.getDocumentElement().normalize();
                NodeList nList = doc.getElementsByTagName("string");

                nameAttribute = "";
                content = "";
                for (int i = 0; i < nList.getLength(); i++) {
                    Node nNode = nList.item(i);
                    // חיפוש והוספת הערך של ה-attribut name למערך
                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element element = (Element) nNode;
                        nameAttribute += element.getAttribute("name") + "\n";
                    }

                    content += nNode.getTextContent().trim() + "\n";

                    int progress = (i * 100) / nList.getLength(); // חישוב התקדמות
                    runOnUiThread(() -> {
                        progressBar.setProgress(progress);
                    });
                }

                if (skippedChar != null && !skippedChar.get(0).isEmpty()) {
                    content = replacementSkippedChars(content, false);
                }

                String finalNameAttribute = nameAttribute;
                String strBody = null;
                if (BuildConfig.DEBUG) {
                    String[] arrName = finalNameAttribute.split("\n");
                    String[] arrContent = content.split("\n");

                    for (int i = 0; i < arrName.length; i++) {
                        strBody += "name = " + arrName[i] + "  value = " + arrContent[i] + "\n";
                        int progress = (i * 100) / nList.getLength(); // חישוב התקדמות
                        runOnUiThread(() -> progressBar.setProgress(progress));
                    }
                }
                String finalContent = content;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        edtFrom.setText(finalContent);
                        btnSaveTranslate.setEnabled(true);
                        edtTo.setEnabled(true);
                        if (BuildConfig.DEBUG) {
                            edtTo.setText(finalContent);
                        }
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, getText(R.string.error_parsing_file), Toast.LENGTH_SHORT).show()
                );
                e.printStackTrace();
            }
        }).start();
    }


    private void createdTranslateXml(String[] arrNameAttribute, String[] arrContent, Uri uriSave) {
        try {
            // יצירת  output stream לקובץ
            OutputStream outputStream = getContentResolver().openOutputStream(uriSave);

            // יצירת דוקומנט XML חדש
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.newDocument();

            // יצירת התגים הראשיים בקובץ
            Element rootElement = doc.createElement("resources");
            doc.appendChild(rootElement);

            // הוספת התגים לקובץ בהתאם למערכי הקלט
            for (int i = 0; i < arrNameAttribute.length; i++) {
                Element element = doc.createElement("string");
                element.setAttribute("name", arrNameAttribute[i]);
                element.appendChild(doc.createTextNode(arrContent[i]));
                rootElement.appendChild(element);
                int progress = (i * 100) / arrNameAttribute.length; // חישוב התקדמות
                progressBar.setProgress(progress);
            }

            // כתיבת התוכן לקובץ ושמירתו באחסון החיצוני של האפליקציה
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(outputStream);
            transformer.transform(source, result);
            edtFrom.setText(R.string.save_successful);
            Log.d("SaveToXml", "File saved successfully: " + uriSave.getPath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}