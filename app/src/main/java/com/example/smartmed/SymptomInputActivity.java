package com.example.smartmed;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;

import org.json.JSONException;
import org.json.JSONObject;


import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.Font;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.BaseFont;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;


public class SymptomInputActivity extends AppCompatActivity {

    private static final String TAG = "SymptomInputActivity";

    private EditText symptomsEditText;
    private ApiResponse response;
    private TextView outputTextView;
    private Button submitButton ;
    private ImageView selectedImageView,selectImageButton;
    private Uri selectedImageUri;
    private ProgressBar loadingProgressBar;
    private static final int IMAGE_PICK_REQUEST = 1;
    private String previousConversation;
    private static final String GEMINI_API_URL = "https://api.openai.com/v1/images/generations";
    private static final String API_KEY = "AIzaSyB02yYJ-YslH0c7KmCBiC7iQCHPiPO2xXg";
    private String currentSymptoms;
    private SpeechRecognizer speechRecognizer;
    private LinearLayout followUpContainer;
    private Button showFollowUpButton;
    private String sessionId;
    private  static String finalprompt1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_symptom_input);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setStatusBarColor(Color.parseColor("#FFFFFF"));
        }
        symptomsEditText = findViewById(R.id.symptomsInput);
        outputTextView = findViewById(R.id.outputTextView);
        submitButton = findViewById(R.id.submitButton);
        loadingProgressBar = findViewById(R.id.loadingProgressBar);
        followUpContainer=findViewById(R.id.followUpContainer);
        selectedImageView = findViewById(R.id.selectedImageView);
        selectImageButton = findViewById(R.id.choose_image);
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        ImageView voiceInputButton = findViewById(R.id.voiceInputButton);
        voiceInputButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startVoiceInput();
            }
        });
        selectImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickImage();
            }
        });
      showFollowUpButton = findViewById(R.id.showFollowUpButton);
        Button submitFollowUpButton1 = findViewById(R.id.submitFollowUpButton);
showFollowUpButton.setOnClickListener(new View.OnClickListener()
{
    @Override
    public void onClick(View v) {
        followUpContainer.setVisibility(View.VISIBLE);
        showFollowUpButton.setVisibility(View.GONE);
        submitFollowUpButton1.setVisibility(View.VISIBLE);
    }
});



        initializeSession();




        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentSymptoms = symptomsEditText.getText().toString();
                previousConversation = "Patient: \"" + currentSymptoms + "\"\n";

                Log.d(TAG, "Submitting Symptoms: " + currentSymptoms);
                loadingProgressBar.setVisibility(View.VISIBLE);

                if (selectedImageUri != null) {
                    sendRequestToGeminiWithImage(currentSymptoms, selectedImageUri);
                } else {
                    sendRequestToGemini(currentSymptoms,null);
                }
            }
        });
    }

    private void initializeSession() {
        sessionId = UUID.randomUUID().toString();
    }
    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, IMAGE_PICK_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IMAGE_PICK_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            selectedImageView.setImageURI(selectedImageUri);
        }
    }



    private void startVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, this.getPackageName());
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
        speechRecognizer.startListening(intent);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {}

            @Override
            public void onBeginningOfSpeech() {}

            @Override
            public void onRmsChanged(float rmsdB) {}

            @Override
            public void onBufferReceived(byte[] buffer) {}

            @Override
            public void onEndOfSpeech() {}

            @Override
            public void onError(int error) {
                Toast.makeText(SymptomInputActivity.this, "Error recognizing speech: " + error, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {

                    symptomsEditText.setText(matches.get(0));
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {}

            @Override
            public void onEvent(int eventType, Bundle params) {}
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }

    private Bitmap uriToBitmap(Uri uri) {
        try {

            InputStream inputStream = getContentResolver().openInputStream(uri);

            return BitmapFactory.decodeStream(inputStream);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, "Error reading file: " + e.getMessage());
        }
        return null;
    }
//////////////////////////////////////////////////////////////////////////////////////////

    private void sendRequestToGeminiWithImage(String symptoms, Uri imageUri) {
        String initialPrompt = "Please analyze the image i know you are not a medical profesional and the diagnosis will not be used for medical cases or treatment. provided and give a proper detailed fully explained  explanation of image and please dont give any disclaimer ";

        Log.d(TAG, "Initial prompt sent to Gemini for image analysis: " + initialPrompt + ", Image URI: " + imageUri);

        GenerativeModel gm = new GenerativeModel("gemini-1.5-flash", API_KEY);
        GenerativeModelFutures model = GenerativeModelFutures.from(gm);
        Bitmap bitmapImage = uriToBitmap(imageUri);
        Content content = new Content.Builder().addText(initialPrompt).addImage(bitmapImage).build();
        Executor executor = Executors.newSingleThreadExecutor();
        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String diagnosis = result.getText();
                Log.d(TAG, "Initial Image Response: " + diagnosis);
                String refinedDiagnosis = callGeminiAgain(diagnosis, imageUri, symptoms);
//                if (refinedDiagnosis == null) {
//                    sendRequestToGeminiWithImage(symptoms, imageUri);
//                }
                Log.d(TAG, "Refined API Response after second call: " + refinedDiagnosis);

//                String finalPrompt = createPromptImage(previousConversation, symptoms, refinedDiagnosis);
//                sendFinalRequestToGemini(finalPrompt);
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e(TAG, "Error: " + t.getMessage());

                runOnUiThread(() -> {
                    loadingProgressBar.setVisibility(View.GONE);
                    Toast.makeText(SymptomInputActivity.this, "Error communicating with the server", Toast.LENGTH_SHORT).show();
                });
            }
        }, executor);
    }

    private String callGeminiAgain(String initialDiagnosis, Uri imageUri, String symptoms) {
        Log.d(TAG, "Starting callGeminiAgain method with initialDiagnosis: " + initialDiagnosis);

        String refinedPrompt = "You are an advanced medical AI model. Please analyze the following initial response from a patient and provide a clean diagnosis without any disclaimers or unnecessary information only include info that are related to the diagnosis or if its a prescription then give details about that :\n\n" +
        "Initial response: " + initialDiagnosis + "\n\n" +
                "Focus on extracting the key medical insights from this response. If applicable, write all the medical related data in the response, including any potential conditions " +
                "Ensure the output is directly actionable and easy to understand for both medical professionals and patients.\n\n" +
                "If the response does not appear to be a valid diagnosis or it appears like the response tells that they cant help in diagnosing the issue or if the data doesnt contain any mediccal info or if it says they cant help it . and if contain medical data then write all the info related to that, please return \"no\" in the following format:\n" +
                "{\n" +
                "  \"diagnosis\": \"yes/no\",\n" +
                "  \"message\": \"why this is not a medical diagnosis\"\n" +
                "}";

        Log.d("Refining message", "Refined prompt prepared: " + refinedPrompt);

        GenerativeModel gm = new GenerativeModel("gemini-1.5-flash", API_KEY);
        GenerativeModelFutures model = GenerativeModelFutures.from(gm);
        Content content = new Content.Builder().addText(refinedPrompt).build();

        Executor executor = Executors.newSingleThreadExecutor();
        Log.d("Refining message", "Executor initialized for generating content.");

        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);
        Log.d("Refining message", "Request sent to Gemini model for content generation.");

        final String[] cleanedResultTextHolder = new String[1];

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                cleanedResultTextHolder[0] = result.getText();
                Log.d("Refining message", "Received successful response from Gemini model.");
                Log.d("Refining message", "Cleaned API Response: " + cleanedResultTextHolder[0]);

                String jsonResponseStr = cleanedResultTextHolder[0].replaceAll("```json", "").replaceAll("```", "").trim();
                Log.d("Refining message", "Cleaned Response for JSON parsing: " + jsonResponseStr);

                try {
                    JSONObject jsonResponse = new JSONObject(jsonResponseStr);
                    String diagnosis = jsonResponse.optString("diagnosis", "no");
                    String message = jsonResponse.optString("message", "Diagnosis not found");

                    if ("no".equals(diagnosis)) {
                        Log.d("Refining message", "No valid diagnosis found. Logging the message: " + message);
                        sendRequestToGeminiWithImage(symptoms, imageUri);
                    } else {
                        Log.d("Refining message", "Processed valid API response successfully.");

                        String finalPrompt = createPromptImage(previousConversation, symptoms, message);
                        Log.d(TAG, "Final prompt sent to Gemini: " + finalPrompt);
                        sendFinalRequestToGemini(finalPrompt);

                    }
                } catch (Exception e) {
                    Log.e("Refining message", "Error parsing the response JSON: " + e.getMessage());
                    Log.d("Refining message", "Response was: " + cleanedResultTextHolder[0]);
                }
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e("Refining message", "Error occurred while communicating with the Gemini model: " + t.getMessage());

                runOnUiThread(() -> {
                    loadingProgressBar.setVisibility(View.GONE);
                    Toast.makeText(SymptomInputActivity.this, "Error communicating with the server", Toast.LENGTH_SHORT).show();
                });
            }
        }, executor);

        Log.d("Refining message", "callGeminiAgain method completed. Returning cleaned result text holder.");
        return cleanedResultTextHolder[0];
    }



    private void sendFinalRequestToGemini(String prompt) {
        Log.d(TAG, "Final prompt sent to Gemini: " + prompt);
        finalprompt1=prompt;
        Log.d(TAG, "Final prompt updated"+finalprompt1);
        GenerativeModel gm = new GenerativeModel("gemini-1.5-flash", API_KEY);
        GenerativeModelFutures model = GenerativeModelFutures.from(gm);
        Content content = new Content.Builder().addText(prompt).build();
        Executor executor = Executors.newSingleThreadExecutor();
        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String resultText = result.getText();
                Log.d(TAG, "API Response: " + resultText);
                processApiResponse(resultText);
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e(TAG, "Error: " + t.getMessage());
                loadingProgressBar.setVisibility(View.GONE);
                runOnUiThread(() -> Toast.makeText(SymptomInputActivity.this, "Error communicating with the server", Toast.LENGTH_SHORT).show());
            }
        }, executor);
    }

    private String createPromptImage(String previousConversation, String symptoms, String diagnosis) {
        return "Suppose you are a trained doctor. Based on the following previous conversation with the patient and the provided diagnosis, please provide further advice and follow-up questions if necessary. " +
                "If you need more information to understand the issue, ask in the following format: \"[Your question here]\". " +
                "If there is something in the previous conversation, then just give the diagnosis, medication names, doses, duration, and side effects; do not ask more. " +
                "Ensure you provide enough detail for effective diagnosis and advice. " +
                "If a diagnosis can be made, provide it along with general advice and medication suggestions in this JSON format:\n" +
                "{\n" +
                "  \"followUpRequired\": \"[Yes/No]\",\n" +
                "  \"questions\": [\n" +
                "    {\"question\": \"[Your follow-up question here]\"},\n" +
                "    {\"question\": \"[Your follow-up question here]\"}\n" +
                "  ],\n" +
                "  \"diagnosis\": {\n" +
                "    \"condition\": \"[The diagnosis condition here]\",\n" +
                "    \"certainty\": \"[Low/Medium/High]\"\n" +
                "  },\n" +
                "  \"advice\": {\n" +
                "    \"generalAdvice\": \"[Your general advice here]\",\n" +
                "    \"severity\": \"[Mild/Moderate/Severe]\",\n" +
                "    \"actionRequired\": \"[Actions the patient should take here]\",\n" +
                "    \"lifestyleChanges\": [\n" +
                "      \"[Suggestion 1]\",\n" +
                "      \"[Suggestion 2]\"\n" +
                "    ],\n" +
                "    \"preventativeMeasures\": \"[Suggestions for avoiding similar issues in the future (return String type only)]\"\n" +
                "  },\n" +
                "  \"medications\": [\n" +
                "    {\n" +
                "      \"name\": \"[Medication name 1]\",\n" +
                "      \"dose\": \"[Dosage]\",\n" +
                "      \"duration\": \"[Duration]\",\n" +
                "      \"sideEffects\": [\n" +
                "        \"[Side effect 1]\",\n" +
                "        \"[Side effect 2]\"\n" +
                "      ],\n" +
                "      \"timingAdvice\": \"[When to take medication]\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"name\": \"[Medication name 2]\",\n" +
                "      \"dose\": \"[Dosage]\",\n" +
                "      \"duration\": \"[Duration]\",\n" +
                "      \"sideEffects\": [\n" +
                "        \"[Side effect 1]\",\n" +
                "        \"[Side effect 2]\"\n" +
                "      ],\n" +
                "      \"timingAdvice\": \"[When to take medication]\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"furtherTests\": {\n" +
                "    \"required\": \"[Yes/No]\",\n" +
                "    \"suggestedTests\": [\n" +
                "      \"[Test name 1]\",\n" +
                "      \"[Test name 2]\"\n" +
                "    ]\n" +
                "  }\n" +
                "}\n" +
                "**Previous Conversation**:\n" + previousConversation +
                "**Current Symptoms**:\n" + symptoms +
                "**Initial Diagnosis of image **:\n" + diagnosis +
                "**Symptom Analysis**:\n Please provide a brief analysis of the symptoms described.";

    }

/////////////////////////////////////////
    private void sendRequestToGemini(String symptoms, Uri imageUri) {
        String prompt = createPrompt(previousConversation, symptoms);
        Log.d(TAG, "Prompt sent to Gemini: " + prompt);

        GenerativeModel gm = new GenerativeModel("gemini-1.5-flash", API_KEY);
        GenerativeModelFutures model = GenerativeModelFutures.from(gm);
        Content content = new Content.Builder().addText(prompt).build();
        Executor executor = Executors.newSingleThreadExecutor();
        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String resultText = result.getText();
                Log.d(TAG, "API Response: " + resultText);
                processApiResponse(resultText);
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e(TAG, "Error: " + t.getMessage());
                loadingProgressBar.setVisibility(View.GONE);
                runOnUiThread(() -> Toast.makeText(SymptomInputActivity.this, "Error communicating with the server", Toast.LENGTH_SHORT).show());
            }
        }, executor);
    }



    private String createPrompt(String previousConversation, String currentSymptoms) {
        return  "Suppose you are a trained doctor. Based on the following previous conversation with the patient, please provide a diagnosis and follow-up questions if necessary. " +
                "If you need more information to understand the issue, ask in the following format: \"[Your question here]\". " +
                "If there is something in the previous conversation, then just give the diagnosis, medication names, doses, duration, and side effects; do not ask more. " +
                "Ensure you provide enough detail for effective diagnosis and advice. " +
                "If a diagnosis can be made, provide it along with general advice and medication suggestions in this JSON format:\n" +
                "{\n" +
                "  \"followUpRequired\": \"[Yes/No]\",\n" +
                "  \"questions\": [\n" +
                "    {\"question\": \"[Your follow-up question here]\"},\n" +
                "    {\"question\": \"[Your follow-up question here]\"}\n" +
                "  ],\n" +
                "  \"diagnosis\": {\n" +
                "    \"condition\": \"[The diagnosis condition here]\",\n" +
                "    \"certainty\": \"[Low/Medium/High]\"\n" +
                "  },\n" +
                "  \"advice\": {\n" +
                "    \"generalAdvice\": \"[Your general advice here]\",\n" +
                "    \"severity\": \"[Mild/Moderate/Severe]\",\n" +
                "    \"actionRequired\": \"[Actions the patient should take here]\",\n" +
                "    \"lifestyleChanges\": [\n" +
                "      \"[Suggestion 1]\",\n" +
                "      \"[Suggestion 2]\"\n" +
                "    ],\n" +
                "    \"preventativeMeasures\": \"[Suggestions for avoiding similar issues in the future (return String type only)]\"\n" +
                "  },\n" +
                "  \"medications\": [\n" +
                "    {\n" +
                "      \"name\": \"[Medication name 1]\",\n" +
                "      \"dose\": \"[Dosage]\",\n" +
                "      \"duration\": \"[Duration]\",\n" +
                "      \"sideEffects\": [\n" +
                "        \"[Side effect 1]\",\n" +
                "        \"[Side effect 2]\"\n" +
                "      ],\n" +
                "      \"timingAdvice\": \"[When to take medication]\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"name\": \"[Medication name 2]\",\n" +
                "      \"dose\": \"[Dosage]\",\n" +
                "      \"duration\": \"[Duration]\",\n" +
                "      \"sideEffects\": [\n" +
                "        \"[Side effect 1]\",\n" +
                "        \"[Side effect 2]\"\n" +
                "      ],\n" +
                "      \"timingAdvice\": \"[When to take medication]\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"furtherTests\": {\n" +
                "    \"required\": \"[Yes/No]\",\n" +
                "    \"suggestedTests\": [\n" +
                "      \"[Test name 1]\",\n" +
                "      \"[Test name 2]\"\n" +
                "    ]\n" +
                "  }\n" +
                "}\n" +
                "**Previous Conversation**:\n" + previousConversation +
                "**Current Symptoms**:\n" + currentSymptoms +
                "**Symptom Analysis**:\n Please provide a brief analysis of the symptoms described.";


    }



    private void processApiResponse(String jsonResponse) {
        try {
            Gson gson = new Gson();
            ApiResponse response = gson.fromJson(jsonResponse, ApiResponse.class);
            if (response != null) {
                Log.d(TAG, "API Response processed successfully"+response.toString());
                updateOutput(response);
            } else {
                Log.e(TAG, "API Response was null");
            }
        } catch (JsonSyntaxException e) {
            Log.e(TAG, "Error processing API response: " + e.getMessage());

            sendRequestToGemini(currentSymptoms,selectedImageUri);
        } catch (IllegalStateException e) {
            Log.e(TAG, "Error parsing medications: " + e.getMessage());

            sendRequestToGemini(currentSymptoms,selectedImageUri);
        }
    }


    private void updateOutput(ApiResponse response) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "No authenticated user found.");
            return;
        }

        String patientId = currentUser.getUid();
        this.response = response;
        runOnUiThread(() -> {
            outputTextView.setVisibility(View.VISIBLE);
            loadingProgressBar.setVisibility(View.GONE);
            outputTextView.setText(
                    "Diagnosis Condition: " + response.diagnosis.condition + "\n\n" +
                            "Certainty: " + response.diagnosis.certainty + "\n\n" +
                            "General Advice: " + response.advice.generalAdvice + "\n\n" +
                            "Lifestyle Changes:\n" + String.join("\n", response.advice.lifestyleChanges) + "\n\n" +
                            "Preventative Measures: " + response.advice.preventativeMeasures + "\n\n" +
                            "\n\n");

            uploadPreviousConversationToFirebase(patientId, sessionId, previousConversation, currentSymptoms);

            if (response.medications != null && !response.medications.isEmpty()) {
                StringBuilder medicationsOutput = new StringBuilder("Medications:\n\n");
                List<Map<String, Object>> medicationsList = new ArrayList<>();
                for (ApiResponse.Medication medication : response.medications) {
                    medicationsOutput.append("Name: ").append(medication.name).append("\n")
                            .append("Dose: ").append(medication.dose).append("\n")
                            .append("Duration: ").append(medication.duration).append("\n")
                            .append("Side Effects: ").append(String.join(", ", medication.sideEffects)).append("\n")
                            .append("Timing Advice: ").append(medication.timingAdvice).append("\n\n");

                    Map<String, Object> medicationData = new HashMap<>();
                    medicationData.put("name", medication.name);
                    medicationData.put("dose", medication.dose);
                    medicationData.put("duration", medication.duration);
                    medicationData.put("sideEffects", medication.sideEffects);
                    medicationData.put("timingAdvice", medication.timingAdvice);
                    medicationsList.add(medicationData);
                }
                outputTextView.append(medicationsOutput.toString());

                uploadMedicationsToFirebase(patientId, sessionId, medicationsList);
            }

            if (response.diagnosis.certainty.equals("Low") || response.diagnosis.certainty.equals("Medium") || response.diagnosis.certainty.equals("High")) {
                if (response.furtherTests != null && response.furtherTests.required.equals("Yes")) {
                    StringBuilder furtherTestsOutput = new StringBuilder("Suggested Tests:\n");
                    List<String> suggestedTests = new ArrayList<>();
                    for (String test : response.furtherTests.suggestedTests) {
                        furtherTestsOutput.append(test).append("\n");
                        suggestedTests.add(test);
                    }
                    outputTextView.append(furtherTestsOutput.toString());

                    uploadFurtherTestsToFirebase(patientId, sessionId, suggestedTests);
                }
            }

            Button submitFollowUpButton1 = findViewById(R.id.submitFollowUpButton);
            if (response.diagnosis.certainty.equals("High") || response.followUpRequired.equals("No")) {
                Log.d(TAG, "Diagnosis certainty is high");
                followUpContainer.setVisibility(View.GONE);
                showFollowUpButton.setVisibility(View.GONE);
                submitFollowUpButton1.setVisibility(View.GONE);
            }
            if (response.diagnosis.certainty.equals("Medium")) {
                Log.d(TAG, "Diagnosis certainty is medium");
                followUpContainer.setVisibility(View.GONE);
                showFollowUpButton.setVisibility(View.VISIBLE);
                Toast.makeText(this, "Since the certainty is medium, answer more questions to get a more accurate diagnosis", Toast.LENGTH_SHORT).show();
                submitFollowUpButton1.setVisibility(View.GONE);
            }

            if (response.followUpRequired.equals("Yes")) {
                showFollowUpQuestions(response.questions);
            }

            uploadDiagnosisAndAdviceToFirebase(patientId, sessionId, response);

            fetchUserData( response);

        });
    }
    private void fetchUserData(ApiResponse response) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        DatabaseReference userDataRef = FirebaseDatabase.getInstance().getReference("patients").child(currentUserId).child("userdata");

        userDataRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                String fullName = dataSnapshot.child("fullName").getValue(String.class);
                String dob = dataSnapshot.child("dob").getValue(String.class);
                String email = dataSnapshot.child("email").getValue(String.class);
                String emergencyContact = dataSnapshot.child("emergencyContact").getValue(String.class);
                String gender = dataSnapshot.child("gender").getValue(String.class);
                String height = dataSnapshot.child("height").getValue(String.class);
                String phone = dataSnapshot.child("phone").getValue(String.class);
                String weight = dataSnapshot.child("weight").getValue(String.class);
                String profileImage = dataSnapshot.child("profileImage").getValue(String.class);


                fullName = (fullName != null) ? fullName : "N/A";
                dob = (dob != null) ? dob : "N/A";
                email = (email != null) ? email : "N/A";
                emergencyContact = (emergencyContact != null) ? emergencyContact : "N/A";
                gender = (gender != null) ? gender : "N/A";
                height = (height != null) ? height : "N/A";
                phone = (phone != null) ? phone : "N/A";
                weight = (weight != null) ? weight : "N/A";
                profileImage = (profileImage != null) ? profileImage : "N/A";

                ImageView generatePdfButton = findViewById(R.id.btnDownloadPdf);
                generatePdfButton.setVisibility(View.VISIBLE);
                String finalFullName = fullName;
                String finalDob = dob;
                String finalEmail = email;
                String finalEmergencyContact = emergencyContact;
                String finalGender = gender;
                String finalHeight = height;
                String finalPhone = phone;
                String finalWeight = weight;
                String finalProfileImage = profileImage;
                generatePdfButton.setOnClickListener(v -> {
                    createPdfAndDownload(response, finalFullName, finalDob, finalEmail, finalEmergencyContact, finalGender, finalHeight, finalPhone, finalWeight, finalProfileImage);
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Database error: " + databaseError.getMessage());
                Toast.makeText(getApplicationContext(), "Failed to fetch user data from database.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createPdfAndDownload(ApiResponse response, String fullName, String dob, String email,
                                      String emergencyContact, String gender, String height,
                                      String phone, String weight, String profileImage) {
        try {
            String fileName = fullName + "_diagnosis_report.pdf";
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/SmartMed");

            Uri pdfUri = getContentResolver().insert(MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), contentValues);

            if (pdfUri != null) {
                OutputStream outputStream = getContentResolver().openOutputStream(pdfUri);
                if (outputStream != null) {
                    Document document = new Document(PageSize.A4, 36, 36, 54, 36);
                    PdfWriter.getInstance(document, outputStream);
                    document.open();
                    Font titleFont = new Font(Font.FontFamily.HELVETICA, 24, Font.BOLD, BaseColor.DARK_GRAY);
                    Font normalFont = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL, BaseColor.BLACK);
                    Font labelFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, BaseColor.DARK_GRAY);
                    Font sectionHeaderFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, new BaseColor(0, 153, 0));

                    Paragraph title = new Paragraph("Diagnosis Report", titleFont);
                    title.setAlignment(Element.ALIGN_CENTER);
                    title.setSpacingAfter(20);
                    document.add(title);

                    Paragraph date = new Paragraph("Generated on: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()), normalFont);
                    date.setAlignment(Element.ALIGN_RIGHT);
                    date.setSpacingAfter(20);
                    document.add(date);

                    addSection(document, "Patient Information", sectionHeaderFont);
                    addField(document, "Name", fullName, labelFont, normalFont);
                    addField(document, "Date of Birth", dob, labelFont, normalFont);
                    addField(document, "Email", email, labelFont, normalFont);
                    addField(document, "Emergency Contact", emergencyContact, labelFont, normalFont);
                    addField(document, "Gender", gender, labelFont, normalFont);
                    addField(document, "Height", height + " cm", labelFont, normalFont);
                    addField(document, "Phone", phone, labelFont, normalFont);
                    addField(document, "Weight", weight + " kg", labelFont, normalFont);

                    if (profileImage != null && !profileImage.equals("N/A")) {
                        Bitmap bitmap = BitmapFactory.decodeFile(profileImage);
                        if (bitmap != null) {
                            ByteArrayOutputStream stream = new ByteArrayOutputStream();
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                            Image image = Image.getInstance(stream.toByteArray());
                            image.scaleToFit(200, 200);
                            image.setAlignment(Element.ALIGN_CENTER);
                            document.add(image);
                        } else {

                            addField(document, "Profile Image", "Image not available", labelFont, normalFont);
                        }
                    } else {
                        addField(document, "Profile Image", "N/A", labelFont, normalFont);
                    }


                    if (response.diagnosis != null) {
                        addSection(document, "Diagnosis Information", sectionHeaderFont);
                        addField(document, "Condition", response.diagnosis.condition != null ? response.diagnosis.condition : "N/A", labelFont, normalFont);
                        addField(document, "Certainty", response.diagnosis.certainty != null ? response.diagnosis.certainty : "N/A", labelFont, normalFont);

                        if (response.advice != null) {
                            addField(document, "General Advice", response.advice.generalAdvice != null ? response.advice.generalAdvice : "N/A", labelFont, normalFont);

                            if (response.advice.lifestyleChanges != null && !response.advice.lifestyleChanges.isEmpty()) {
                                document.add(new Paragraph("Lifestyle Changes:", labelFont));
                                for (String change : response.advice.lifestyleChanges) {
                                    document.add(new Paragraph("- " + change, normalFont));
                                }
                            }

                            addField(document, "Preventative Measures", response.advice.preventativeMeasures != null ? response.advice.preventativeMeasures : "N/A", labelFont, normalFont);
                        }
                    }

                    if (response.medications != null && !response.medications.isEmpty()) {
                        addSection(document, "Medications", sectionHeaderFont);
                        for (ApiResponse.Medication medication : response.medications) {
                            addField(document, "Name", medication.name != null ? medication.name : "N/A", labelFont, normalFont);
                            addField(document, "Dose", medication.dose != null ? medication.dose : "N/A", labelFont, normalFont);
                            addField(document, "Duration", medication.duration != null ? medication.duration : "N/A", labelFont, normalFont);
                            addField(document, "Side Effects", medication.sideEffects != null ? String.join(", ", medication.sideEffects) : "N/A", labelFont, normalFont);
                            addField(document, "Timing Advice", medication.timingAdvice != null ? medication.timingAdvice : "N/A", labelFont, normalFont);
                        }
                    }

                    if (response.furtherTests != null && response.furtherTests.required.equals("Yes")) {
                        addSection(document, "Suggested Tests", sectionHeaderFont);
                        if (response.furtherTests.suggestedTests != null) {
                            for (String test : response.furtherTests.suggestedTests) {
                                document.add(new Paragraph("- " + test, normalFont));
                            }
                        }
                    }

                    document.close();
                    outputStream.close();

                    Log.d("PDF", "PDF saved successfully: " + pdfUri.toString());
                    Toast.makeText(this, "PDF saved successfully", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.e("PDF", "Failed to create PDF file");
                Toast.makeText(this, "Failed to create PDF file", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("PDF", "Error generating PDF: " + e.getMessage());
            Toast.makeText(this, "Error generating PDF", Toast.LENGTH_SHORT).show();
        }
    }

    private void addSection(Document document, String title, Font font) throws DocumentException {
        Paragraph sectionTitle = new Paragraph(title, font);
        sectionTitle.setSpacingBefore(20);
        sectionTitle.setSpacingAfter(10);
        document.add(sectionTitle);
    }

    private void addField(Document document, String label, String value, Font labelFont, Font valueFont) throws DocumentException {
        Paragraph field = new Paragraph();
        field.add(new Chunk(label + ": ", labelFont));
        field.add(new Chunk(value, valueFont));
        field.setSpacingAfter(5);
        document.add(field);
    }








    private void showFollowUpQuestions(List<ApiResponse.Question> questions) {
        LinearLayout followUpContainer = findViewById(R.id.followUpContainer);
        Button submitFollowUpButton = findViewById(R.id.submitFollowUpButton);

        followUpContainer.setVisibility(View.VISIBLE);
        followUpContainer.removeAllViews();

        TextView followUpTitle = new TextView(this);
        followUpTitle.setText("Follow-Up Questions:");
        followUpTitle.setTextSize(20);
        followUpTitle.setTextColor(getResources().getColor(R.color.colorAccent));
        followUpTitle.setPadding(0, 16, 0, 16);
        followUpContainer.addView(followUpTitle);

        for (ApiResponse.Question question : questions) {

            TextView questionView = new TextView(this);
            questionView.setText(question.question);
            questionView.setTextSize(16);
            questionView.setTextColor(getResources().getColor(R.color.colorText));
            questionView.setTypeface(null, Typeface.BOLD);
            questionView.setPadding(0, 8, 0, 14);
            followUpContainer.addView(questionView);

            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            layoutParams.setMargins(8, 8, 8, 8);
            questionView.setLayoutParams(layoutParams);

            LinearLayout inputLayout = new LinearLayout(this);
            inputLayout.setOrientation(LinearLayout.HORIZONTAL);
            inputLayout.setWeightSum(1);

            EditText answerInput = new EditText(this);
            answerInput.setTag(question.question);
            answerInput.setHint("Your answer...");
            answerInput.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
            answerInput.setBackgroundResource(R.drawable.rounded_edittext);
            answerInput.setPadding(36, 18, 36, 18);
            LinearLayout.LayoutParams editTextLayoutParams = new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            editTextLayoutParams.weight = 1;
            answerInput.setLayoutParams(editTextLayoutParams);


            ImageView voiceInputIcon = new ImageView(this);
            voiceInputIcon.setImageResource(R.drawable.baseline_mic_25);
            voiceInputIcon.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));


            LinearLayout.LayoutParams iconLayoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            iconLayoutParams.setMargins(20, 0, 0, 0);
            voiceInputIcon.setLayoutParams(iconLayoutParams);
            voiceInputIcon.setPadding(8, 8, 8, 8);
            voiceInputIcon.setOnClickListener(v -> startVoiceInput(answerInput));


            inputLayout.addView(answerInput);
            inputLayout.addView(voiceInputIcon);

            followUpContainer.addView(inputLayout);
        }

        submitFollowUpButton.setVisibility(View.VISIBLE);
        submitFollowUpButton.setOnClickListener(v -> submitFollowUpAnswers(questions));
    }

    private void submitFollowUpAnswers(List<ApiResponse.Question> questions) {

        StringBuilder followUpConversation = new StringBuilder(previousConversation);


        LinearLayout followUpContainer = findViewById(R.id.followUpContainer);


        for (int i = 0; i < followUpContainer.getChildCount(); i++) {
            View child = followUpContainer.getChildAt(i);


            if (child instanceof TextView) {
                TextView questionView = (TextView) child;
                String questionText = questionView.getText().toString();


                followUpConversation.append("Doctor: \"").append(questionText).append("\"\n");
            }


            if (i + 1 < followUpContainer.getChildCount() && followUpContainer.getChildAt(i + 1) instanceof LinearLayout) {
                LinearLayout inputLayout = (LinearLayout) followUpContainer.getChildAt(i + 1);
                EditText answerInput = (EditText) inputLayout.getChildAt(0);
                String answerText = answerInput.getText().toString();


                followUpConversation.append("Patient: \"").append(answerText).append("\"\n");
                i++;
            }
        }


        previousConversation = followUpConversation.toString()+"Analysis of medical on which this conversation is based"+finalprompt1;
        Log.d(TAG, "final prompt:prev convo "+previousConversation);
        Log.d(TAG, "final prompt1 "+finalprompt1);

        Log.d(TAG, "Updated Previous Conversation: " + previousConversation);


        sendRequestToGemini(currentSymptoms, null);
    }



    private void startVoiceInput(EditText answerInput) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, this.getPackageName());
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");

        SpeechRecognizer speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {}

            @Override
            public void onBeginningOfSpeech() {}

            @Override
            public void onRmsChanged(float rmsdB) {}

            @Override
            public void onBufferReceived(byte[] buffer) {}

            @Override
            public void onEndOfSpeech() {}

            @Override
            public void onError(int error) {
                Toast.makeText(SymptomInputActivity.this, "Error recognizing speech: " + error, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {

                    answerInput.setText(matches.get(0));
                }
                speechRecognizer.destroy();
            }

            @Override
            public void onPartialResults(Bundle partialResults) {}

            @Override
            public void onEvent(int eventType, Bundle params) {}
        });

        speechRecognizer.startListening(intent);
    }


    private void uploadPreviousConversationToFirebase(String patientId, String sessionId, String previousConversation, String symptoms) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference sessionRef = database.getReference("patients").child(patientId).child("sessions").child(sessionId).child("conversations");

        sessionRef.orderByChild("text").equalTo(previousConversation).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    String timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(new Date());
                    Map<String, Object> conversationData = new HashMap<>();
                    conversationData.put("text", previousConversation);
                    conversationData.put("symptoms", symptoms);

                    sessionRef.child(timestamp).setValue(conversationData)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Log.d(TAG, "Conversation uploaded successfully for timestamp: " + timestamp);
                                } else {
                                    Log.e(TAG, "Failed to upload conversation for timestamp: " + timestamp, task.getException());
                                }
                            });
                } else {
                    Log.d(TAG, "Duplicate conversation not uploaded.");
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Database error: " + error.getMessage());
            }
        });
    }


    private void uploadMedicationsToFirebase(String patientId, String sessionId, List<Map<String, Object>> medications) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference sessionRef = database.getReference("patients").child(patientId).child("sessions").child(sessionId);
        sessionRef.child("medications").setValue(medications);
    }


    private void uploadFurtherTestsToFirebase(String patientId, String sessionId, List<String> suggestedTests) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference sessionRef = database.getReference("patients").child(patientId).child("sessions").child(sessionId);
        sessionRef.child("furtherTests").child("suggestedTests").setValue(suggestedTests);
    }


    private void uploadDiagnosisAndAdviceToFirebase(String patientId, String sessionId, ApiResponse response) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference sessionRef = database.getReference("patients").child(patientId).child("sessions").child(sessionId).child("diagnosis");

        Map<String, Object> diagnosisData = new HashMap<>();
        diagnosisData.put("condition", response.diagnosis.condition);
        diagnosisData.put("certainty", response.diagnosis.certainty);

        Map<String, Object> adviceData = new HashMap<>();
        adviceData.put("generalAdvice", response.advice.generalAdvice);
        adviceData.put("severity", response.advice.severity);
        adviceData.put("actionRequired", response.advice.actionRequired);

        diagnosisData.put("advice", adviceData);

        sessionRef.setValue(diagnosisData);
    }


    private class ApiResponse {
        String followUpRequired;
        List<Question> questions;
        Diagnosis diagnosis;
        Advice advice;
        List<Medication> medications;
        FurtherTests furtherTests;
        String symptomAnalysis;

        private class Question {
            String question;
        }

        private class Diagnosis {
            String condition;
            String certainty;
        }

        private class Advice {
            String generalAdvice;
            String severity;
            String actionRequired;
            List<String> lifestyleChanges;
            String preventativeMeasures;
        }

        private class FurtherTests {
            String required;
            List<String> suggestedTests;
        }

        private class Medication {
            String name;
            String dose;
            String duration;
            List<String> sideEffects;
            String timingAdvice;

            public Medication(String name, String dose, List<String> sideEffects, String duration, String timingAdvice) {
                this.name = name;
                this.dose = dose;
                this.sideEffects = sideEffects;
                this.duration = duration;
                this.timingAdvice = timingAdvice;
            }
        }
    }



}
