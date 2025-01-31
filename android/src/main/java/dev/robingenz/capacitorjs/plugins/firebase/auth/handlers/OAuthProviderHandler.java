package dev.robingenz.capacitorjs.plugins.firebase.auth.handlers;

import android.util.Log;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.PluginCall;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.OAuthCredential;
import com.google.firebase.auth.OAuthProvider;
import dev.robingenz.capacitorjs.plugins.firebase.auth.FirebaseAuthentication;
import dev.robingenz.capacitorjs.plugins.firebase.auth.FirebaseAuthenticationPlugin;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;

public class OAuthProviderHandler {

    private final FirebaseAuthentication pluginImplementation;

    public OAuthProviderHandler(FirebaseAuthentication pluginImplementation) {
        this.pluginImplementation = pluginImplementation;
    }

    public void signIn(PluginCall call, String providerId) {
        OAuthProvider.Builder provider = OAuthProvider.newBuilder(providerId);
        applySignInConfig(call, provider);
        Task<AuthResult> pendingResultTask = pluginImplementation.getFirebaseAuthInstance().getPendingAuthResult();
        if (pendingResultTask == null) {
            startActivityForSignIn(call, provider);
        } else {
            finishActivityForSignIn(call, pendingResultTask);
        }
    }

    private void startActivityForSignIn(final PluginCall call, OAuthProvider.Builder provider) {
        pluginImplementation
            .getFirebaseAuthInstance()
            .startActivityForSignInWithProvider(pluginImplementation.getPlugin().getActivity(), provider.build())
            .addOnSuccessListener(
                authResult -> {
                    AuthCredential credential = authResult.getCredential();
                    // Todo: Change the implementation - Try to fix Microsoft login
                    //pluginImplementation.handleSuccessfulSignIn(call, credential, null);
                    pluginImplementation.handleSuccessfulMicrosoftSignIn(call, credential, null);
                }
            )
            .addOnFailureListener(exception -> pluginImplementation.handleFailedSignIn(call, null, exception));
    }

    private void finishActivityForSignIn(final PluginCall call, Task<AuthResult> pendingResultTask) {
        pendingResultTask
            .addOnSuccessListener(
                authResult -> {
                    AuthCredential credential = authResult.getCredential();
                    pluginImplementation.handleSuccessfulSignIn(call, credential, null);
                }
            )
            .addOnFailureListener(exception -> pluginImplementation.handleFailedSignIn(call, null, exception));
    }

    private void applySignInConfig(PluginCall call, OAuthProvider.Builder provider) {
        JSArray customParameters = call.getArray("customParameters");
        if (customParameters != null) {
            try {
                List<Object> customParametersList = customParameters.toList();
                for (int i = 0; i < customParametersList.size(); i++) {
                    JSObject customParameter = JSObject.fromJSONObject((JSONObject) customParametersList.get(i));
                    String key = customParameter.getString("key");
                    String value = customParameter.getString("value");
                    if (key == null || value == null) {
                        continue;
                    }
                    provider.addCustomParameter(key, value);
                }
            } catch (JSONException exception) {
                Log.e(FirebaseAuthenticationPlugin.TAG, "applySignInConfig failed.", exception);
            }
        }
    }
}
