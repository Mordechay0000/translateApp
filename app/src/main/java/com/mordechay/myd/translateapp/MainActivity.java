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
import android.widget.Toast;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String FILE_TYPE = "text/xml";
    private static final String REPLACEMENT_CHARACTER = "@@@";
    private EditText edtFrom;
    private EditText edtTo;
    private ActivityResultLauncher<Intent> getSelectedFileActivityResult;
    private ActivityResultLauncher<Intent> getSaveFileActivityResult;
    private static final String[] skippedChar = new String[]{"@","*","$"};
    private StringBuilder nameAttribute;
    private StringBuilder content;
    //private static int[] locationSkippedChar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSelectedFileActivityResult = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // There are no request codes
                        Uri fileUri;
                        if (result.getData() != null) {
                            fileUri = result.getData().getData();
                            parserXml(fileUri);
                        }else {
                            Toast.makeText(this, getText(R.string.no_selected_file_or_directory), Toast.LENGTH_SHORT).show();
                        }

                    }
                });
        getSaveFileActivityResult = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // There are no request codes
                        Uri fileUri;
                        if (result.getData() != null) {
                            fileUri = result.getData().getData();
                            saveXmlTranslate(fileUri);
                        }else {
                            Toast.makeText(this, getText(R.string.no_selected_file_or_directory), Toast.LENGTH_SHORT).show();
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
        int id = view.getId();
        if (id == R.id.btn_select_file) {
            pickFile(false);
        } else if (id == R.id.btn_translate) {
            pickFile(true);
        }

    }

    public void pickFile(boolean isSave) {
        Intent intent = new Intent(!isSave ? Intent.ACTION_GET_CONTENT : Intent.ACTION_CREATE_DOCUMENT);
        intent.setType(FILE_TYPE);
        if (!isSave){
            getSelectedFileActivityResult.launch(intent);
        }else {
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.putExtra(Intent.EXTRA_TITLE, "strings.xml");
            getSaveFileActivityResult.launch(intent);
        }

    }



    private static String replacementSkippedChars(String content, boolean isParsing){
            //locationSkippedChar = findString(content, skippedChar);
            for (int i = 0; i < skippedChar.length; i++) {
                content = content.replace(!isParsing ? skippedChar[i] : REPLACEMENT_CHARACTER + i, !isParsing ? REPLACEMENT_CHARACTER + i : skippedChar[i]);
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
        content = new StringBuilder(edtTo.getText().toString());
        content = new StringBuilder(replacementSkippedChars(content.toString(), true));
        String[] arrNameAttribute = nameAttribute.toString().split("\n");
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

                nameAttribute = new StringBuilder("");
                content = new StringBuilder("");
                for (int i = 0; i < nList.getLength(); i++) {
                    Node nNode = nList.item(i);
                    // חיפוש והוספת הערך של ה-attribut name למערך
                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element element = (Element) nNode;
                        nameAttribute.append(element.getAttribute("name")).append("\n");
                    }

                    content.append(nNode.getTextContent().trim()).append("\n");
                }

                if (skippedChar != null && !skippedChar[0].isEmpty()) {
                    content = new StringBuilder(replacementSkippedChars(content.toString(), false));
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        edtFrom.setText(content);
                        if(BuildConfig.DEBUG) {
                            String[] arrName = nameAttribute.toString().split("\n");
                            String[] arrContent = content.toString().split("\n");
                            for (int i = 0; i < arrName.length; i++) {
                                String strBody = "name = " + arrName[i] + "  value = " + arrContent[i];
                                edtTo.setText(edtTo.getText() + strBody + "\n");
                            }
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
            }

            // כתיבת התוכן לקובץ ושמירתו באחסון החיצוני של האפליקציה
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(outputStream);
            transformer.transform(source, result);
            Log.d("SaveToXml", "File saved successfully: " + uriSave.getPath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}