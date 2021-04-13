package com.vishnu.cordova.sms.retriever;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;


import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.Credentials;
import com.google.android.gms.auth.api.credentials.HintRequest;
import com.google.android.gms.auth.api.phone.SmsRetriever;
import com.google.android.gms.auth.api.phone.SmsRetrieverClient;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.apache.cordova.CordovaWebView;
import org.apache.cordova.LOG;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;

import static android.app.Activity.RESULT_OK;


public class SMSRetriever extends CordovaPlugin {
    private static String TAG = "SMSRetriever";
    private static final int CREDENTIAL_PICKER_REQUEST = 01;
    private SMSBroadcastReceiver smsBroadcastReceiver;
    private static final int REQ_USER_CONSENT = 200;
    private static CallbackContext mCallbackContext;
    private Context mApplicationContext;
    public static CordovaInterface mCordova;
    public static SMSRetriever mPlugin;
    public static Activity mActivity;


    private static final String HASH_TYPE = "SHA-256";
    public static final int NUM_HASHED_BYTES = 9;
    public static final int NUM_BASE64_CHAR = 11;


    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        mActivity = cordova.getActivity();
        mApplicationContext = cordova.getContext();
        mPlugin = this;
        mCordova = cordova;
        Log.i(TAG, "initialize: ");
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.i(TAG, "onStart: ");
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        mCallbackContext = callbackContext;
        if (action.equals("getHint")) {
            getHint();
        } else if (action.equals("getSMS")) {
            getSMS(callbackContext, args);
        } else if (action.equals("getAppHash")) {
            getAppHash(callbackContext);
        } else {
            mCallbackContext.error("invalid action");
            return false;
        }
        return true;
    }


    private void getHint() {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                HintRequest hintRequest = new HintRequest.Builder()
                        .setPhoneNumberIdentifierSupported(true)
                        .build();
                PendingIntent intent = Credentials.getClient(mApplicationContext).getHintPickerIntent(hintRequest);
                try {
                    cordova.setActivityResultCallback(mPlugin);
                    mActivity.startIntentSenderForResult(intent.getIntentSender(),
                            CREDENTIAL_PICKER_REQUEST, null, 0, 0, 0);
                } catch (IntentSender.SendIntentException e) {
                    mCallbackContext.error(e.toString());
                }
            }
        });
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CREDENTIAL_PICKER_REQUEST && resultCode == RESULT_OK) {
            Credential credential = data.getParcelableExtra(Credential.EXTRA_KEY);
            mCallbackContext.success(credential.getId());
            return;
        }
        if (requestCode == SMSBroadcastReceiver.SMS_CONSENT_REQUEST && ((resultCode == RESULT_OK) && (data != null))) {
            String message = data.getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE);
            mCallbackContext.success(message);
            return;
        }
        PluginResult result = new PluginResult(PluginResult.Status.ERROR);
        mCallbackContext.sendPluginResult(result);
    }

    public void getAppHash(CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void run() {
                ArrayList<String> appcodes = getAppSignatures();
                PluginResult result = new PluginResult(PluginResult.Status.OK, String.join(",", appcodes));
                callbackContext.sendPluginResult(result);
            }
        });
    }

    public void getSMS(CallbackContext callbackContext, JSONArray args) {
        try {
            JSONObject options = args.length() > 0 ? args.getJSONObject(0) : new JSONObject();
            Boolean consent = options.has("consent") && options.getBoolean("consent");
            cordova.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    start(consent);
                    PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
                    result.setKeepCallback(true);
                    callbackContext.sendPluginResult(result);
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
            mCallbackContext.error(e.getMessage());
        }
    }

    public static void onSMS(String sms) {
        if (mCallbackContext == null) {
            Log.w(TAG, "onSMS: no callbackcontext");
            return;
        }
        PluginResult result = new PluginResult(PluginResult.Status.OK, sms);
        mCallbackContext.sendPluginResult(result);
    }

    public static void onTimeout(String error) {
        if (mCallbackContext == null) {
            Log.w(TAG, "onSMS: no callbackcontext");
            return;
        }
        PluginResult result = new PluginResult(PluginResult.Status.ERROR, error);
        mCallbackContext.sendPluginResult(result);
    }

    private void start(Boolean consent) {
        SmsRetrieverClient client = SmsRetriever.getClient(cordova.getActivity());
        Task<Void> task;
        if (consent) {
            task = client.startSmsUserConsent(null);
        } else {
            task = client.startSmsRetriever();
        }
        task.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.i(TAG, "onSuccess:  sms retriever started");
            }
        });

        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e(TAG, "onFailure: " + e.getMessage());
            }
        });
    }


    @Override
    public void onStop() {
        super.onStop();
    }

    /**
     * Get all the app signatures for the current package
     *
     * @return
     */
    public ArrayList<String> getAppSignatures() {
        ArrayList<String> appCodes = new ArrayList<>();
        try {
            // Get all package signatures for the current package
            String packageName = mActivity.getPackageName();
            PackageManager packageManager = mActivity.getPackageManager();
            Signature[] signatures = packageManager.getPackageInfo(packageName,
                    PackageManager.GET_SIGNATURES).signatures;

            // For each signature create a compatible hash
            for (Signature signature : signatures) {
                String hash = hash(packageName, signature.toCharsString());
                if (hash != null) {
                    appCodes.add(String.format("%s", hash));
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Unable to find package to obtain hash.", e);
            return null;
        }
        return appCodes;
    }

    private static String hash(String packageName, String signature) {
        String appInfo = packageName + " " + signature;
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(HASH_TYPE);
            messageDigest.update(appInfo.getBytes(StandardCharsets.UTF_8));
            byte[] hashSignature = messageDigest.digest();

            // truncated into NUM_HASHED_BYTES
            hashSignature = Arrays.copyOfRange(hashSignature, 0, NUM_HASHED_BYTES);
            // encode into Base64
            String base64Hash = Base64.encodeToString(hashSignature, Base64.NO_PADDING | Base64.NO_WRAP);
            base64Hash = base64Hash.substring(0, NUM_BASE64_CHAR);

            Log.d(TAG, String.format("pkg: %s -- hash: %s", packageName, base64Hash));
            return base64Hash;
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "hash:NoSuchAlgorithm", e);
        }
        return null;
    }
}
