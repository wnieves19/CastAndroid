package com.optivon.castapp;

import android.util.Log;

import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;

/**
 * Created by wilfredonieves on 12/23/15.
 */
public class CustomChannel implements Cast.MessageReceivedCallback {
    public String getNamespace() {
        return "urn:x-cast:com.example.custom";
    }

    @Override
    public void onMessageReceived(CastDevice castDevice, String namespace,
                                  String message) {
        Log.d(VoiceCast.TAG, "onMessageReceived: " + message);
    }
}