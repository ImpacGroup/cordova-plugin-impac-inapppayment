package de.impacgroup.inapppayment;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class IMPValidationController {

    private IMPValidationConfig config;
    private Gson gson = new Gson();
    private RequestQueue queue;

    IMPValidationController(Context context) {
        queue = Volley.newRequestQueue(context);
    }

    public void setConfig(IMPValidationConfig config) {
        this.config = config;
    }

    interface IMPValidationListener {
        public void failedValidation(String error);
        public void validationFinished(boolean isValid);
    }

    public void validate(@NonNull final IMPValidationModel model, @NonNull final IMPValidationListener listener) {
        JSONObject object = getJSONObjectFor(gson.toJson(model));
        if (this.config != null) {
            if (object != null) {
                JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.POST, this.config.url, object,
                        new Response.Listener<JSONObject>()
                        {
                            @Override
                            public void onResponse(JSONObject response) {
                                try {
                                    listener.validationFinished(response.getBoolean("success"));
                                } catch (JSONException e) {
                                    listener.failedValidation("Invalid answer from api.");
                                };
                            }
                        },
                        new Response.ErrorListener()
                        {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                listener.failedValidation(error.getLocalizedMessage());
                            }
                        }
                ) {

                    /**
                     * Passing auth request headers
                     */
                    @Override
                    public Map<String, String> getHeaders() {
                        HashMap<String, String> headers = new HashMap<>();
                        headers.put("Content-Type", "application/json");
                        headers.put("Authorization", config.authorizationType + " " + config.accessString);
                        return headers;
                    }
                };
                queue.add(postRequest);
            } else {
                listener.failedValidation("Could not parse validation");
            }
        } else {
            listener.failedValidation("Missing validation configuration");
        }
    }

    private @Nullable JSONObject getJSONObjectFor(String jsonString) {
        try {
            return new JSONObject(jsonString);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }
}
