package com.home.pr.opendoor;

import android.app.IntentService;
import android.content.Intent;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by Pr on 28/10/2016.
 */

//this intent service is called by the application to send requests to adafruit service

public class AdafruitUpdateService extends IntentService {
    private static final String TAG = IntentService.class.getSimpleName();
    public String DOOR_UPDATE= "";
    public static final String DOOR_BROADCAST = "com.example.android.fingerprintdialog.DOOR_BROADCAST";
    public static final String DOOR_STATUS = "com.example.android.fingerprintdialog.DOOR_STATUS";
    public AdafruitUpdateService() {
        super("AdafruitUpdateService");
    }
    Retrofit retrofit = new Retrofit.Builder()
            .baseUrl("https://io.adafruit.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build();
    @Override
    protected void onHandleIntent(Intent workIntent) {
        DOOR_UPDATE = workIntent.getStringExtra("DOOR_UPDATE");
        Log.e(TAG,DOOR_UPDATE);
        sendAdafruitUpdateRequest(DOOR_UPDATE);
    }

    public void sendAdafruitUpdateRequest(String status) {
        final String sample=status;
        AdafruitApiCallModel.UpdateDoorStatus update = retrofit.create(AdafruitApiCallModel.UpdateDoorStatus.class);
        Call<ResponseClass> response = update.getResponse(new DoorValue(Integer.parseInt(status)));
        response.enqueue(new Callback<ResponseClass> () {
            @Override
            public void onResponse(Call<ResponseClass> c, Response<ResponseClass> r) {
                Log.e(TAG, Integer.toString(r.code()));
                if (r.code() == 201) {
                    Intent localIntent = new Intent(DOOR_BROADCAST).putExtra(DOOR_STATUS,sample);
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(localIntent);
                    if (sample != "0") {
                        final Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                sendAdafruitUpdateRequest("0");
                            }
                        }, 10000);
                    }
                }
                else {
                    Log.e(TAG,"Unknown response code from server");
                }
            }
            public void onFailure(Call<ResponseClass> c, Throwable t) {
                Log.e(TAG,"Status update failed");
            }
        });
    }
}
