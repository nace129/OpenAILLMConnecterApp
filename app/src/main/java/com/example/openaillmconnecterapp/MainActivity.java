package com.example.openaillmconnecterapp;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private EditText promptInput;
    private TextView responseText;
    private Button sendButton;
    private Button cancelButton;
    private RadioGroup modelGroup;

    private LLMApiTask currentTask;

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        promptInput = findViewById(R.id.promptInput);
        responseText = findViewById(R.id.responseText);
        sendButton = findViewById(R.id.sendButton);
        cancelButton = findViewById(R.id.cancelButton);
        modelGroup = findViewById(R.id.modelGroup);


        sendButton.setOnClickListener(v -> {
            String prompt = promptInput.getText().toString();
            if (!prompt.isEmpty()) {
                String selectedModel = getSelectedModel();
                currentTask = new LLMApiTask(selectedModel);
                currentTask.execute(prompt);
                sendButton.setEnabled(false);
                cancelButton.setEnabled(true);
            }
        });

        cancelButton.setOnClickListener(v -> {
            if (currentTask != null) {
                currentTask.cancel(true);
                sendButton.setEnabled(true);
                cancelButton.setEnabled(false);
                Toast.makeText(MainActivity.this, "Request cancelled", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getSelectedModel() {
        int checkedId = modelGroup.getCheckedRadioButtonId();
        if (checkedId == R.id.radioHuggingFace) {
            return "huggingface";
        } else {
            return "openai";  // Default to OpenAI, but this can be changed
        }
    }

    @SuppressWarnings("deprecation")
    private class LLMApiTask extends AsyncTask<String, Void, String> {
        private String modelType;

        public LLMApiTask(String modelType) {
            this.modelType = modelType;
        }

        @Override
        protected String doInBackground(String... params) {
            String prompt = params[0];
            try {
                if (modelType.equals("huggingface")) {
                    return callHuggingFaceAPI(prompt);
                } else {
                    return callOpenAIAPI(prompt);
                }
            } catch (Exception e) {
                return "Error: " + e.getMessage();
            }
        }

        private String callOpenAIAPI(String prompt) throws IOException, JSONException {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("model", "gpt-3.5-turbo");
//                    .put("messages", new JSONArray().put(new JSONObject().put("role", "user").put("content", prompt)));

            JSONArray messages = new JSONArray();
            messages.put(new JSONObject().put("role", "system").put("content", prompt));
//            messages.put(new JSONObject().put("role", "user").put("content", userMessage));
            jsonBody.put("messages", messages);
            jsonBody.put("temperature", 0.7);


            RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
            Request request = new Request.Builder()
                    .url("https://api.openai.com/v1/chat/completions")
                    .header("Authorization", "Bearer OPENAI_API_KEY")
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful())
                    throw new IOException("Unexpected response: " + response);

                String responseBody = response.body().string();
                if (responseBody == null || responseBody.isEmpty())
                    return "Error: Empty response from server";

                try {
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    JSONArray choices = jsonResponse.optJSONArray("choices");

                    if (choices == null || choices.length() == 0)
                        return "Error: No choices in response";

                    JSONObject firstChoice = choices.getJSONObject(0);
                    JSONObject messageObj = firstChoice.optJSONObject("message");

                    if (messageObj == null || !messageObj.has("content"))
                        return "Error: Unexpected response format";

                    return messageObj.getString("content");

                } catch (JSONException e) {
                    return "Error parsing JSON: " + e.getMessage();
                }
            }
        }


//        private String callHuggingFaceAPI(String prompt) throws IOException, JSONException {
////            JSONObject jsonBody = new JSONObject().put("inputs", prompt);
//
//            JSONObject jsonBody = new JSONObject();
////            jsonBody.put("model", "gpt-3.5-turbo");
//            JSONArray messages = new JSONArray();
//            messages.put(new JSONObject().put("inputs", prompt));
//            jsonBody.put("messages", messages);
//            //jsonBody.put("temperature", 0.7);
//
//            RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
//            Request request = new Request.Builder()
//                    .url("https://api-inference.huggingface.co/models/facebook/blenderbot-3B")
//                    .header("Authorization", "Bearer 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")
//                    .post(body)
//                    .build();
//
//            try (Response response = client.newCall(request).execute()) {
//                if (!response.isSuccessful())
//                    throw new IOException("Unexpected response: " + response);
//
//                String responseBody = response.body().string();
//                if (responseBody == null || responseBody.isEmpty())
//                    return "Error: Empty response from server";
//
//                // Debugging: Print raw response
//                System.out.println("Hugging Face Response: " + responseBody);
//
//                try {
//                    JSONArray jsonResponse = new JSONArray(responseBody);
//                    if (jsonResponse.length() == 0)
//                        return "Error: No response data";
//
//                    return jsonResponse.getJSONObject(0).optString("generated_text", "Error: Missing 'generated_text' field");
//
//                } catch (JSONException e) {
//                    return "Error parsing JSON: " + e.getMessage();
//                }
//            }
//        }
private String callHuggingFaceAPI(String prompt) throws IOException, JSONException {
    // Create the correct JSON body
    JSONObject jsonBody = new JSONObject();
    jsonBody.put("inputs", prompt);  // Correct structure

    RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
    Request request = new Request.Builder()
            .url("https://api-inference.huggingface.co/models/facebook/blenderbot-3B")
            .header("Authorization", "Bearer HUGGINGFACE_TOKEN")
            .post(body)
            .build();

    try (Response response = client.newCall(request).execute()) {
        if (!response.isSuccessful()) {
            return "Error: Unexpected response: " + response.code() + " - " + response.message();
        }

        String responseBody = response.body().string();
        if (responseBody == null || responseBody.isEmpty()) {
            return "Error: Empty response from server";
        }

        // Debugging: Print raw response
        System.out.println("Hugging Face Response: " + responseBody);

        // Parse the response correctly
        JSONArray jsonResponse = new JSONArray(responseBody);
        if (jsonResponse.length() == 0) {
            return "Error: No response data";
        }

        return jsonResponse.getJSONObject(0).optString("generated_text", "Error: Missing 'generated_text' field");

    } catch (JSONException e) {
        return "Error parsing JSON: " + e.getMessage();
    }
}


        @Override
        protected void onPostExecute(String result) {
            responseText.setText(result);
            sendButton.setEnabled(true);
            cancelButton.setEnabled(false);
            currentTask = null;
        }
    }
}
