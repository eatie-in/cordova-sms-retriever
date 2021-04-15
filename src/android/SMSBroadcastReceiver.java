package com.vishnu.cordova.sms.retriever;

import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.auth.api.phone.SmsRetriever;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Status;

public class SMSBroadcastReceiver extends BroadcastReceiver {
    private static String TAG = "SMSBroadcastReceiver";
    public static final int SMS_CONSENT_REQUEST = 2;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (SmsRetriever.SMS_RETRIEVED_ACTION.equals(intent.getAction())) {
            Bundle extras = intent.getExtras();
            Status status = (Status) extras.get(SmsRetriever.EXTRA_STATUS);

            switch (status.getStatusCode()) {
                case CommonStatusCodes.SUCCESS:
                    // Get SMS message contents
                    String message = (String) extras.get(SmsRetriever.EXTRA_SMS_MESSAGE);
                    Log.i(TAG, "onReceive: " + message);
                    if (message != null) {
                        SMSRetriever.onSMS(message);
                    } else {
                        // Get consent intent
                        Intent consentIntent = extras.getParcelable(SmsRetriever.EXTRA_CONSENT_INTENT);
                        try {
                            SMSRetriever.mCordova.setActivityResultCallback(SMSRetriever.mPlugin);
                            SMSRetriever.mActivity.startActivityForResult(consentIntent, SMS_CONSENT_REQUEST);
                        } catch (ActivityNotFoundException e) {
                            Log.e(TAG, "onReceive: " + e.getMessage());
                            SMSRetriever.onTimeout(e.getMessage());
                        }
                    }
                    break;
                case CommonStatusCodes.TIMEOUT:
                    Log.e(TAG, "onReceive:  Timeout");
                    SMSRetriever.onTimeout("Timeout");
                    break;
            }
        }
    }
}
