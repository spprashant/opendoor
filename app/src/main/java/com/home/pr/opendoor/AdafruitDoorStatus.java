package com.home.pr.opendoor;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by Pr on 19/11/2016.
 */
public class AdafruitDoorStatus extends IntentService {
    private static final String TAG = IntentService.class.getSimpleName();
    public static final String DOOR_BROADCAST = "com.example.android.fingerprintdialog.DOOR_BROADCAST";
    public static final String DOOR_STATUS = "com.example.android.fingerprintdialog.DOOR_STATUS";
    public AdafruitDoorStatus() {
        super("AdafruitDoorStatus");
    }
    Retrofit retrofit = new Retrofit.Builder()
            .baseUrl("https://io.adafruit.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build();

    @Override
    protected void onHandleIntent(Intent intent) {
        AdafruitApiCallModel.GetDoorStatus status = retrofit.create(AdafruitApiCallModel.GetDoorStatus.class);
        Call<StatusClass> response = status.getResponse();
        response.enqueue(new Callback<StatusClass> () {
            @Override
            public void onResponse(Call<StatusClass> c, Response<StatusClass> r) {
                Log.e(TAG,Integer.toString(r.code()));
                StatusClass sc =  r.body();
                if (r.code() == 200) {
                    Intent localIntent = new Intent(DOOR_BROADCAST).putExtra(DOOR_STATUS,sc.getDoorStatus());
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(localIntent);
                }
            }
            public void onFailure(Call<StatusClass> c, Throwable t) {
                Log.e(TAG,"Status update failed");
            }
        });
    }
}
