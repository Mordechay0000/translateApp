package com.mordechay.myd.translateapp;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
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
    private static final String DEFAULT_SHRED_PREFERENCES = "save_data";
    private static final String SKIPPED_CHARS_KEY = "skipped_chars";
    private ColorStateList successColor;
    private Button btnSaveTranslate;
    private TextView edt;
    private ActivityResultLauncher<Intent> fileActivityResult;
    private static ArrayList<String> skippedChar;
    private String nameAttribute;
    private String content;
    private ProgressBar progressBar;
    private ColorStateList clrDefault;
    private boolean isSave;
    private AlertDialog dialog;
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

        startCreditDialog();
        skippedChar = new ArrayList<>(getSharedPreferences(DEFAULT_SHRED_PREFERENCES, 0).getStringSet(SKIPPED_CHARS_KEY, new HashSet<>(Arrays.asList("%s", "%1$s", "%2$s", "%3$s", "%4$s", "%5$s", "%6$s", "%7$s", "%8$s", "%9$s", "%d", "%1$d", "%2$d", "%3$d", "%4$d", "%5$d", "%6$d", "%7$d", "%8$d", "%9$d"))));
        progressBar = findViewById(R.id.progress_bar);
        progressBar.setMax(100);
        progressBar.setProgress(0);
        progressBar.setVisibility(View.VISIBLE);
        clrDefault = progressBar.getProgressTintList();
        successColor = ColorStateList.valueOf(getColor(R.color.success));
        edt = findViewById(R.id.edt);
        findViewById(R.id.btn_manger_skipped_chars).setOnClickListener(this);
        findViewById(R.id.btn_select_file).setOnClickListener(this);
        btnSaveTranslate = findViewById(R.id.btn_translate);
        btnSaveTranslate.setOnClickListener(this);
    }

    private void startCreditDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(R.layout.credit_dialog);
        builder.setCancelable(false);
        dialog = builder.create();
        dialog.show();
        TextView txt = dialog.findViewById(R.id.show_html);
        txt.setText(Html.fromHtml(getText(R.string.Details_about_the_developer).toString(), Html.FROM_HTML_MODE_LEGACY));
        dialog.findViewById(R.id.btn_credit_ok).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id ==R.id.btn_manger_skipped_chars) {
            openMangerSkippedDialog();
        } else if (id == R.id.btn_select_file) {
            pickFile(false);
        } else if (id == R.id.btn_translate) {
            pickFile(true);
        }

    }

    private void openMangerSkippedDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(R.layout.manger_skipped_chars);
        builder.setCancelable(true);
        AlertDialog dialogSkipped = builder.create();
        new Thread(() -> {
            StringBuilder strText = new StringBuilder();
            for (int i = 0; i < skippedChar.size() - 1; i++){
                strText.append(skippedChar.get(i)).append(",");
            }
            /**
             * So that the last one does not add an unnecessary comma
             */
            {
                strText.append(skippedChar.get(skippedChar.size() - 1));
            }

            runOnUiThread(() -> {
                dialogSkipped.show();
                EditText edtSkippedChars = dialogSkipped.findViewById(R.id.edt_skipped_chars);
                edtSkippedChars.setText(strText);
                dialogSkipped.findViewById(R.id.btn_save_skipped_chars).setOnClickListener(v ->
                        {
                            getSharedPreferences(DEFAULT_SHRED_PREFERENCES, 0).edit().putStringSet(SKIPPED_CHARS_KEY, new HashSet<>(Arrays.asList(edtSkippedChars.getText().toString().trim().split(",")))).apply();
                            skippedChar = new ArrayList<>(getSharedPreferences(DEFAULT_SHRED_PREFERENCES, 0).getStringSet(SKIPPED_CHARS_KEY, new HashSet<>(Arrays.asList("%s", "%1$s", "%2$s", "%3$s", "%4$s", "%5$s", "%6$s", "%7$s", "%8$s", "%9$s", "%d", "%1$d", "%2$d", "%3$d", "%4$d", "%5$d", "%6$d", "%7$d", "%8$d", "%9$d"))));
                            dialogSkipped.dismiss();
                        }
                );
            });
        }).start();
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
        content = edt.getText().toString();
        content = replacementSkippedChars(content, true);
        String[] arrNameAttribute = nameAttribute.split("\n");
        String[] arrContent = content.toString().split("\n");
        createdTranslateXml(arrNameAttribute, arrContent, UriSave);
    }


    private void parserXml(Uri selectedFile) {
        edt.setText(R.string.file_parser);
        progressBar.setProgress(0);
        progressBar.setProgressTintList(clrDefault);
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
                int nListLength = nList.getLength();
                for (int i = 0; i < nListLength - 1; i++) {
                    Node nNode = nList.item(i);
                    // חיפוש והוספת הערך של ה-attribut name למערך
                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element element = (Element) nNode;
                        nameAttribute += element.getAttribute("name") + "\n";
                    }

                    content += nNode.getTextContent().trim() + "\n";

                    int progress = (i * 100) / nListLength; // חישוב התקדמות
                    runOnUiThread(() -> progressBar.setProgress(progress));
                }


                /**
                 *So that the last one won't add an unnecessary line drop
                 */
                {
                    Node nNode = nList.item(nListLength - 1);
                    // חיפוש והוספת הערך של ה-attribut name למערך
                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element element = (Element) nNode;
                        nameAttribute += element.getAttribute("name");
                    }

                    content += nNode.getTextContent().trim();

                    runOnUiThread(() -> progressBar.setProgress(99));
                }

                if (skippedChar != null && !skippedChar.get(0).isEmpty()) {
                    content = replacementSkippedChars(content, false);
                }

                runOnUiThread(() -> {
                    edt.setText(content);
                    edt.setEnabled(true);
                    btnSaveTranslate.setEnabled(true);
                    progressBar.setProgress(100);
                    progressBar.setProgressTintList(successColor);
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, getText(R.string.error_parsing_file), Toast.LENGTH_SHORT).show()
                );
                e.printStackTrace();
            }
        }).start();
    }


    private void createdTranslateXml(String[] arrNameAttribute, String[] arrContent, Uri uriSave) {
        progressBar.setProgressTintList(clrDefault);
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
            progressBar.setProgress(100);
            progressBar.setProgressTintList(successColor);
            btnSaveTranslate.setEnabled(false);
            edt.setEnabled(false);
            edt.setText(R.string.save_successful);
            Log.d("SaveToXml", "File saved successfully: " + uriSave.getPath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        dialog.show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (dialog != null) {
            dialog.dismiss();
        }
    }
}