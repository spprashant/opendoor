/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.home.pr.opendoor;

import android.app.*;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.os.Handler;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Timer;
import java.util.TimerTask;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.inject.Inject;

/**
 * Main entry point for the sample, showing a backpack and "Purchase" button.
 */
public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static int doorIntendMode = 0;
    private static final String DIALOG_FRAGMENT_TAG = "myFragment";
    private static final String SECRET_MESSAGE = "Very secret message";
    /** Alias for our key in the Android Key Store */
    private static final String KEY_NAME = "my_key";

    IntentFilter mIntentStatusFilter = new IntentFilter("com.example.android.fingerprintdialog.DOOR_BROADCAST");
    DoorStatusReceiver mDoorStatusReceiver = new DoorStatusReceiver();

    @Inject KeyguardManager mKeyguardManager;
    @Inject FingerprintManager mFingerprintManager;
    @Inject FingerprintAuthenticationDialogFragment mFragment;
    @Inject KeyStore mKeyStore;
    @Inject KeyGenerator mKeyGenerator;
    @Inject Cipher mCipher;
    @Inject SharedPreferences mSharedPreferences;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        ((InjectedApplication) getApplication()).inject(this);
        LocalBroadcastManager.getInstance(this).registerReceiver(mDoorStatusReceiver,mIntentStatusFilter);
        setContentView(R.layout.activity_main);
        schedulePeriodicUpdate();
        Button authenticateButton = (Button) findViewById(R.id.authenticate_button);
        Button enrollButton = (Button) findViewById(R.id.enroll_button);
        if (!mKeyguardManager.isKeyguardSecure()) {
            // Show a message that the user hasn't set up a fingerprint or lock screen.
            Toast.makeText(this,
                    "Secure lock screen hasn't set up.\n"
                            + "Go to 'Settings -> Security -> Fingerprint' to set up a fingerprint",
                    Toast.LENGTH_LONG).show();
            authenticateButton.setEnabled(false);
            return;
        }

        //noinspection ResourceType
        if (!mFingerprintManager.hasEnrolledFingerprints()) {
            authenticateButton.setEnabled(false);
            // This happens when no fingerprints are registered.
            Toast.makeText(this,
                    "Go to 'Settings -> Security -> Fingerprint' and register at least one fingerprint",
                    Toast.LENGTH_LONG).show();
            return;
        }
        createKey();
        if(!initCipher()) {
            Toast.makeText(getApplicationContext(),
                    "Go to 'Settings -> Security -> Fingerprint' and register at least one fingerprint",
                    Toast.LENGTH_LONG).show();
            return;
        }
        authenticateButton.setEnabled(true);
        authenticateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doorIntendMode = 1;
                if(!initCipher()) {
                    Toast.makeText(getApplicationContext(),
                            "Go to 'Settings -> Security -> Fingerprint' and register at least one fingerprint",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                // Show the fingerprint dialog. The user has the option to use the fingerprint with
                // crypto, or you can fall back to using a server-side verified password.
                mFragment.setCryptoObject(new FingerprintManager.CryptoObject(mCipher));
                /*boolean useFingerprintPreference = mSharedPreferences
                           .getBoolean(getString(R.string.use_fingerprint_to_authenticate_key),
                                    true);*/
                mFragment.setStage(FingerprintAuthenticationDialogFragment.Stage.FINGERPRINT);
                mFragment.show(getFragmentManager(), DIALOG_FRAGMENT_TAG);
            }
        });
        enrollButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doorIntendMode = 3;
                //new ciphers for every new operation.. as per best practice
                if(!initCipher()) {
                    Toast.makeText(getApplicationContext(),
                            "Go to 'Settings -> Security -> Fingerprint' and register at least one fingerprint",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                // Set up the crypto object for later. The object will be authenticated by use
                // of the fingerprint.
                // Show the fingerprint dialog. The user has the option to use the fingerprint with
                // crypto, or you can fall back to using a server-side verified password.
                mFragment.setCryptoObject(new FingerprintManager.CryptoObject(mCipher));
                /*boolean useFingerprintPreference = mSharedPreferences
                           .getBoolean(getString(R.string.use_fingerprint_to_authenticate_key),
                                    true);*/
                mFragment.setStage(FingerprintAuthenticationDialogFragment.Stage.FINGERPRINT);
                mFragment.show(getFragmentManager(), DIALOG_FRAGMENT_TAG);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = new Intent(this, AdafruitDoorStatus.class);
        this.startService(intent);
    }

    /**
     * Initialize the {@link Cipher} instance with the created key in the {@link #createKey()}
     * method.
     *
     * @return {@code true} if initialization is successful, {@code false} if the lock screen has
     * been disabled or reset after the key was generated, or if a fingerprint got enrolled after
     * the key was generated.
     */
    private boolean initCipher() {
        try {
            mKeyStore.load(null);
            SecretKey key = (SecretKey) mKeyStore.getKey(KEY_NAME, null);
            mCipher.init(Cipher.ENCRYPT_MODE, key);
            return true;
        } catch (KeyPermanentlyInvalidatedException e) {
            return false;
        } catch (KeyStoreException | CertificateException | UnrecoverableKeyException | IOException
                | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to init Cipher", e);
        }
    }

    public void onPurchased(boolean withFingerprint) {
        if (withFingerprint) {
            // If the user has authenticated with fingerprint, verify that using cryptography and
            // then show the confirmation message.
            tryEncrypt();
        } else {
            // Authentication happened with backup password. Just show the confirmation message.
            updateDoorStatus(null);
        }
    }

    public void schedulePeriodicUpdate() {
        /*final Handler handler = new Handler();
        handler.postDelayed(new Runnable(){
            @Override
            public void run() {
                handler.postDelayed(this,10000);
                Intent intent = new Intent(getApplicationContext(), AdafruitDoorStatus.class);
                getApplicationContext().startService(intent);
            }
        },10000);*/
        Intent intent = new Intent(this, StatusReceiver.class);
        PendingIntent pIntent = PendingIntent.getBroadcast(this,StatusReceiver.REQUEST_CODE,intent,0);
        long firstMillis = System.currentTimeMillis();
        AlarmManager alarm = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        alarm.setRepeating(AlarmManager.RTC_WAKEUP,firstMillis,10000,pIntent);
    }

    // Show confirmation, if fingerprint was used show crypto information.
    private void updateDoorStatus(byte[] encrypted) {
        findViewById(R.id.confirmation_message).setVisibility(View.VISIBLE);
        //send out an intent to unlock the door
        Intent mTestIntent = new Intent(this, AdafruitUpdateService.class);
        mTestIntent.putExtra("DOOR_UPDATE",Integer.toString(doorIntendMode));
        this.startService(mTestIntent);
        doorIntendMode=0;
    }



    /**
     * Tries to encrypt some data with the generated key in {@link #createKey} which is
     * only works if the user has just authenticated via fingerprint.
     */
    private void tryEncrypt() {
        try {
            byte[] encrypted = mCipher.doFinal(SECRET_MESSAGE.getBytes());
            updateDoorStatus(encrypted);
        } catch (BadPaddingException | IllegalBlockSizeException e) {
            Toast.makeText(this, "Failed to encrypt the data with the generated key. "
                    + "Retry the purchase", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Failed to encrypt the data with the generated key." + e.getMessage());
        }
    }

    /**
     * Creates a symmetric key in the Android Key Store which can only be used after the user has
     * authenticated with fingerprint.
     */
    public void createKey() {
        // The enrolling flow for fingerprint. This is where you ask the user to set up fingerprint
        // for your flow. Use of keys is necessary if you need to know if the set of
        // enrolled fingerprints has changed.
        try {
            mKeyStore.load(null);
            // Set the alias of the entry in Android KeyStore where the key will appear
            // and the constrains (purposes) in the constructor of the Builder
            mKeyGenerator.init(new KeyGenParameterSpec.Builder(KEY_NAME,
                    KeyProperties.PURPOSE_ENCRYPT |
                            KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    // Require the user to authenticate with a fingerprint to authorize every use
                    // of the key
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .build());
            mKeyGenerator.generateKey();
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException
                | CertificateException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        /*int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);*/
        return true;
    }

    public class DoorStatusReceiver extends BroadcastReceiver {

        public DoorStatusReceiver() {}
        @Override
        public void onReceive (Context context, Intent intent) {
            String statusText =intent.getStringExtra("com.example.android.fingerprintdialog.DOOR_STATUS");
            TextView mStatusText = (TextView) findViewById(R.id.status_bar_text);
            LinearLayout mStatusLayout = (LinearLayout) findViewById(R.id.status_layout);
            if (statusText.equalsIgnoreCase("0")) {
                mStatusText.setText("Door locked");
                mStatusLayout.setBackgroundColor(Color.parseColor("#FF0000"));
            }
            else if (statusText.equalsIgnoreCase("1")){
                mStatusText.setText("Door unlocked");
                mStatusLayout.setBackgroundColor(Color.parseColor("#00FF00"));
            }
            else if (statusText.equalsIgnoreCase("3")){
                NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext()).setSmallIcon(R.mipmap.ic_launcher).setContentTitle("OpenDoor").setContentText("Unauthorized attempt to open door");
                NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                builder.setAutoCancel(true);
                Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);
                PendingIntent pIntent = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, 0);
                builder.setContentIntent(pIntent);
                notificationManager.notify(1,builder.build());
                mStatusText.setText("Unauthorized attempt!");
                mStatusLayout.setBackgroundColor(Color.parseColor("#FF00FF"));
            }
        }
    }
}
