package com.vishnu.cordova.sms.retriever;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.os.Build;
import android.os.Bundle;


import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.Credentials;
import com.google.android.gms.auth.api.credentials.HintRequest;
import com.google.android.gms.auth.api.phone.SmsRetriever;
import com.google.android.gms.auth.api.phone.SmsRetrieverClient;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.Task;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.apache.cordova.CordovaWebView;
import org.apache.cordova.LOG;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;



import static android.app.Activity.RESULT_OK;


public class SMSRetriever extends CordovaPlugin {
    private static  String TAG = "SMSRetriever";
    private Activity mActivity;
    private static final int CREDENTIAL_PICKER_REQUEST = 01;
    private SMSBroadcastReceiver smsBroadcastReceiver;
    private static final int REQ_USER_CONSENT = 200;
    private CallbackContext mCallbackContext;
    private CordovaPlugin mPlugin;
    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        smsBroadcastReceiver = new SMSBroadcastReceiver();
        mActivity = cordova.getActivity();
        mPlugin = this;
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        mCallbackContext = callbackContext;
        return this.validateAction(action,callbackContext);
    }

    private Boolean validateAction(String action,CallbackContext callbackContext){
        if(action.equals("getHint")){
            cordova.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    getHint();
                }
            });
            return  true;
        }
        if(action.equals("listen")){
            cordova.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    // register broadcast receiver
                    registerBroadcastReceiver();
                }
            });
            return  true;
        }
        if(action.equals("remove")){
            cordova.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
//                    if(smsBroadcastReceiver != null){
//                        LOG.v(TAG,"receiver not null");
                        mActivity.unregisterReceiver(smsBroadcastReceiver);
//                    }
                    mCallbackContext.success(1);
                }
            });
            return  true;
        }

        if(action.equals("getMessage")){
            cordova.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    startListening();
                }
            });
            return  true;
        }
        return false;
    }

    private void getHint(){
        HintRequest hintRequest = new HintRequest.Builder()
                .setPhoneNumberIdentifierSupported(true)
                .build();
        PendingIntent intent = Credentials.getClient(cordova.getContext()).getHintPickerIntent(hintRequest);
        try {
            cordova.setActivityResultCallback(this);
            mActivity.startIntentSenderForResult(intent.getIntentSender(),
                    CREDENTIAL_PICKER_REQUEST, null, 0, 0, 0);
        } catch (IntentSender.SendIntentException e) {
            mCallbackContext.error(e.toString());
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == CREDENTIAL_PICKER_REQUEST && resultCode == RESULT_OK){
            Credential credential = data.getParcelableExtra(Credential.EXTRA_KEY);
            mCallbackContext.success(credential.getId());
            return;
        }
        if (requestCode == REQ_USER_CONSENT && ((resultCode == RESULT_OK) && (data != null))) {
                String message = data.getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE);
            mCallbackContext.success(message);
            return;
        }
        mCallbackContext.error("NONE");
    }

    public void startListening(){
        SmsRetrieverClient client = SmsRetriever.getClient(mActivity);
        Task<Void> task = client.startSmsUserConsent(null);
        task.addOnSuccessListener(aVoid -> {
            LOG.v(TAG,"retriever started");
        });

        task.addOnFailureListener(e -> {
            LOG.e(TAG,"sms timeout");
        });
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    private void registerBroadcastReceiver() {
        IntentFilter intentFilter = new IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION);
        mActivity.registerReceiver(smsBroadcastReceiver,intentFilter);
        mCallbackContext.success(1);
    }


    public final class SMSBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == SmsRetriever.SMS_RETRIEVED_ACTION) {
                Bundle extras = intent.getExtras();
                Status smsRetrieverStatus = (Status) extras.get(SmsRetriever.EXTRA_STATUS);
                switch (smsRetrieverStatus.getStatusCode()) {
                    case CommonStatusCodes.SUCCESS:
                        Intent messageIntent = extras.getParcelable(SmsRetriever.EXTRA_CONSENT_INTENT);
                        cordova.setActivityResultCallback(mPlugin);
                        mActivity.startActivityForResult(messageIntent, REQ_USER_CONSENT);
                        break;
                    case CommonStatusCodes.TIMEOUT:
                        mCallbackContext.error("TIMEOUT");;
                        break;
                }
            }
        }

    }

}
