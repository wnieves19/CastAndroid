package com.optivon.castapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.MediaRouteActionProvider;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by wilfredonieves on 12/22/15.
 */
public class VoiceCast extends AppCompatActivity implements RecognitionListener {
    /**Shows SpeechRecognizer returned Text */
    private TextView returnedText;
    private Button listenButton;
    private SpeechRecognizer speech = null;
    private Intent recognizerIntent;
    private boolean listening = false;
    public static final int REQUEST_PERMISSION_RECORD_AUDIO = 3;
    private MediaRouteSelector mMediaRouteSelector;
    private MediaRouter mMediaRouter;
    private CastDevice mSelectedDevice;
    private MediaRouter.Callback mMediaRouterCallback;
    private GoogleApiClient mApiClient;
    private Cast.Listener mCastClientListener;
    private ConnectionCallbacks mConnectionCallbacks;
    private ConnectionFailedListener mConnectionFailedListener;
    public static final String TAG = "Cast App";
    private boolean mWaitingForReconnect;
    private boolean mApplicationStarted;
    private CustomChannel mCustomChannel;
    private String mSessionId;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.voice);
        returnedText = (TextView)findViewById(R.id.speech_text);
        returnedText.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark));
        listenButton = (Button)findViewById(R.id.listen);
        speech = SpeechRecognizer.createSpeechRecognizer(this);
        speech.setRecognitionListener(this);
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en");
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,
                this.getPackageName());
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);


        listenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int permissionCheck = ContextCompat.checkSelfPermission(VoiceCast.this, Manifest.permission.RECORD_AUDIO);
                if(permissionCheck!= PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(VoiceCast.this,
                            new String[]{Manifest.permission.RECORD_AUDIO},
                            REQUEST_PERMISSION_RECORD_AUDIO);
                }else{
                    listen();
                }

            }
        });
        setMediaRouter();

        setCastClientListener();

        mConnectionCallbacks = new ConnectionCallbacks();
        mConnectionFailedListener = new ConnectionFailedListener();

    }
    private void setMediaRouter(){
        mMediaRouter = MediaRouter.getInstance(getApplicationContext());
        mMediaRouteSelector = new MediaRouteSelector.Builder()
                .addControlCategory(CastMediaControlIntent.categoryForCast("D5D82017"))
                .build();
        mMediaRouterCallback = new CastMediaRouterCallback();
    }
    private void setCastClientListener(){
        mCastClientListener = new Cast.Listener() {
            @Override
            public void onApplicationStatusChanged() {
                if (mApiClient != null) {
                    Log.d(TAG, "onApplicationStatusChanged: " + Cast.CastApi.getApplicationStatus(mApiClient));
                }
            }

            @Override
            public void onVolumeChanged() {
                if (mApiClient != null) {
                    Log.d(TAG, "onVolumeChanged: " + Cast.CastApi.getVolume(mApiClient));
                }
            }

            @Override
            public void onApplicationDisconnected(int errorCode) {
                teardown(true);
            }
        };
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.voice, menu);
        //Add cast button to ActionBar and get its ActionProvider
        MenuItem mediaRouteMenuItem = menu.findItem(R.id.media_route_menu_item);
        MediaRouteActionProvider mediaRouteActionProvider =
                (MediaRouteActionProvider) MenuItemCompat.getActionProvider(mediaRouteMenuItem);
        mediaRouteActionProvider.setRouteSelector(mMediaRouteSelector);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    /**
     * Request permission to record audio response to dialog
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_PERMISSION_RECORD_AUDIO: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    listen();
                } else {
                    finish();
                }
            }

        }
    }

    /**
     * Start/Stop listening to
     */
    private void listen(){
    if(listening){
        speech.stopListening();
        returnedText.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark));
        listening=false;
    }else {
        returnedText.setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark));
        listening = true;
        speech.startListening(recognizerIntent);
    }
}

    @Override
    public void onReadyForSpeech(Bundle params) {

    }

    @Override
    public void onBeginningOfSpeech() {

    }

    @Override
    public void onRmsChanged(float rmsdB) {

    }

    @Override
    public void onBufferReceived(byte[] buffer) {

    }

    @Override
    public void onEndOfSpeech() {
        listening=false;
    }

    @Override
    public void onError(int error) {

        String errorMessage = ErrorFactory.getErrorText(error);
        returnedText.setText(errorMessage);

    }

    @Override
    public void onResults(Bundle results) {

        ArrayList<String> matches = results
                .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        returnedText.setText(matches.get(0));

        sendMessage(matches.get(0));

    }

    @Override
    public void onPartialResults(Bundle partialResults) {

    }

    @Override
    public void onEvent(int eventType, Bundle params) {

    }
    @Override
    protected void onStart() {
        super.onStart();
        mMediaRouter.addCallback(mMediaRouteSelector, mMediaRouterCallback,
                MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY);
    }

    @Override
    protected void onStop() {
        mMediaRouter.removeCallback(mMediaRouterCallback);
        super.onStop();
    }
    private void teardown(boolean selectDefaultRoute) {
        Log.d(TAG, "teardown");
        if (mApiClient != null) {
            if (mApplicationStarted) {
                if (mApiClient.isConnected() || mApiClient.isConnecting()) {
                    try {
                        Cast.CastApi.stopApplication(mApiClient, mSessionId);
                        if (mCustomChannel != null) {
                            Cast.CastApi.removeMessageReceivedCallbacks(
                                    mApiClient,
                                    mCustomChannel.getNamespace());
                            mCustomChannel = null;
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Exception while removing channel", e);
                    }
                    mApiClient.disconnect();
                }
                mApplicationStarted = false;
            }
            mApiClient = null;
        }
        if (selectDefaultRoute) {
            mMediaRouter.selectRoute(mMediaRouter.getDefaultRoute());
        }
        mSelectedDevice = null;
        mWaitingForReconnect = false;
        mSessionId = null;
    }
    private void sendMessage(String message) {
        if (mApiClient != null && mCustomChannel != null) {
            try {
                Cast.CastApi.sendMessage(mApiClient, mCustomChannel.getNamespace(), message)
                        .setResultCallback(
                                new ResultCallback<Status>() {
                                    @Override
                                    public void onResult(Status result) {
                                        if (!result.isSuccess()) {
                                            Log.e(TAG, "Sending message failed");
                                        }
                                    }
                                });
            } catch (Exception e) {
                Log.e(TAG, "Exception while sending message", e);
            }
        }
    }


    private class CastMediaRouterCallback extends MediaRouter.Callback {

        @Override
        public void onRouteSelected(MediaRouter router, MediaRouter.RouteInfo info) {
            mSelectedDevice = CastDevice.getFromBundle(info.getExtras());
            String routeId = info.getId();
            createClient();
        }
        private void createClient(){
            Cast.CastOptions.Builder apiOptionsBuilder = Cast.CastOptions
                    .builder(mSelectedDevice, mCastClientListener);

            mApiClient = new GoogleApiClient.Builder(VoiceCast.this)
                    .addApi(Cast.API, apiOptionsBuilder.build())
                    .addConnectionCallbacks(mConnectionCallbacks)
                    .addOnConnectionFailedListener(mConnectionFailedListener)
                    .build();
            mApiClient.connect();
        }
        @Override
        public void onRouteUnselected(MediaRouter router, MediaRouter.RouteInfo info) {
            teardown(true);
            mSelectedDevice = null;
        }
    }

    private class ConnectionCallbacks implements GoogleApiClient.ConnectionCallbacks {

        @Override
        public void onConnected(@Nullable Bundle bundle) {
            if (mWaitingForReconnect) {
                mWaitingForReconnect = false;
//                reconnectChannels();
            } else {
                try {
                    Cast.CastApi.launchApplication(mApiClient, "D5D82017", false)
                            .setResultCallback(
                                    new ResultCallback<Cast.ApplicationConnectionResult>() {
                                        @Override
                                        public void onResult(Cast.ApplicationConnectionResult result) {
                                            Status status = result.getStatus();
                                            if (status.isSuccess()) {
                                                ApplicationMetadata applicationMetadata =  result.getApplicationMetadata();
                                                mSessionId = result.getSessionId();
                                                String applicationStatus = result.getApplicationStatus();
                                                boolean wasLaunched = result.getWasLaunched();
                                                mApplicationStarted = true;

                                                mCustomChannel = new CustomChannel();
                                                try {
                                                    Cast.CastApi.setMessageReceivedCallbacks(mApiClient,
                                                            mCustomChannel.getNamespace(),
                                                            mCustomChannel);
                                                } catch (IOException e) {
                                                    Log.e(TAG, "Exception while creating channel", e);
                                                }
                                            
                                            } else {
                                                teardown(true);
                                            }
                                        }
                                    });

                } catch (Exception e) {
                    Log.e(TAG, "Failed to launch application", e);
                }
            }
        }

        @Override
        public void onConnectionSuspended(int i) {

        }
    }
    private class ConnectionFailedListener implements GoogleApiClient.OnConnectionFailedListener {
        @Override
        public void onConnectionFailed(ConnectionResult result) {
            teardown(true);
        }
    }

}
