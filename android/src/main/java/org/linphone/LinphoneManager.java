/*
 * Copyright (c) 2010-2019 Belledonne Communications SARL.
 *
 * This file is part of linphone-android
 * (see https://www.linphone.org).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.linphone;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import java.io.File;
import java.util.Timer;
import java.util.TimerTask;
import org.linphone.call.AndroidAudioManager;
import org.linphone.call.CallManager;
import org.linphone.core.AccountCreator;
import org.linphone.core.BuildConfig;
import org.linphone.core.Call;
import org.linphone.core.Call.State;
import org.linphone.core.Core;
import org.linphone.core.CoreListener;
import org.linphone.core.CoreListenerStub;
import org.linphone.core.Factory;
import org.linphone.core.PresenceActivity;
import org.linphone.core.PresenceBasicStatus;
import org.linphone.core.PresenceModel;
import org.linphone.core.ProxyConfig;
import org.linphone.core.Reason;
import org.linphone.core.Tunnel;
import org.linphone.core.TunnelConfig;
import org.linphone.core.tools.Log;
import org.linphone.settings.LinphonePreferences;
import org.linphone.utils.LinphoneUtils;
import org.linphone.utils.MediaScanner;

/** Handles Linphone's Core lifecycle */
public class LinphoneManager implements SensorEventListener {
    private final String mBasePath;
    private final String mRingSoundFile;
    private final String mCallLogDatabaseFile;
    private final String mFriendsDatabaseFile;
    private final String mUserCertsPath;

    private final Context mContext;
    private AndroidAudioManager mAudioManager;
    private CallManager mCallManager;
    private final PowerManager mPowerManager;
    private final ConnectivityManager mConnectivityManager;
    private TelephonyManager mTelephonyManager;
    private PhoneStateListener mPhoneStateListener;
    private WakeLock mProximityWakelock;
    private final SensorManager mSensorManager;
    private final Sensor mProximity;
    private final MediaScanner mMediaScanner;
    private Timer mTimer;

    private final LinphonePreferences mPrefs;
    private Core mCore;
    private CoreListenerStub mCoreListener;
    private AccountCreator mAccountCreator;

    private boolean mExited;
    private boolean mCallGsmON;
    private boolean mProximitySensingEnabled;
    private boolean mHasLastCallSasBeenRejected;
    private Runnable mIterateRunnable;

    public LinphoneManager(Context c) {
        mExited = false;
        mContext = c;
        mBasePath = c.getFilesDir().getAbsolutePath();
        mCallLogDatabaseFile = mBasePath + "/linphone-log-history.db";
        mFriendsDatabaseFile = mBasePath + "/linphone-friends.db";
        mRingSoundFile = mBasePath + "/share/sounds/linphone/rings/notes_of_the_optimistic.mkv";
        mUserCertsPath = mBasePath + "/user-certs";

        mPrefs = LinphonePreferences.instance();
        mPowerManager = (PowerManager) c.getSystemService(Context.POWER_SERVICE);
        mConnectivityManager =
                (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
        mSensorManager = (SensorManager) c.getSystemService(Context.SENSOR_SERVICE);
        mProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        mTelephonyManager = (TelephonyManager) c.getSystemService(Context.TELEPHONY_SERVICE);
        mPhoneStateListener =
                new PhoneStateListener() {
                    @Override
                    public void onCallStateChanged(int state, String phoneNumber) {
                        switch (state) {
                            case TelephonyManager.CALL_STATE_OFFHOOK:
                                Log.i("[Manager] Phone state is off hook");
                                setCallGsmON(true);
                                break;
                            case TelephonyManager.CALL_STATE_RINGING:
                                Log.i("[Manager] Phone state is ringing");
                                setCallGsmON(true);
                                break;
                            case TelephonyManager.CALL_STATE_IDLE:
                                Log.i("[Manager] Phone state is idle");
                                setCallGsmON(false);
                                break;
                        }
                    }
                };

        Log.i("[Manager] Registering phone state listener");
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);

        mHasLastCallSasBeenRejected = false;
        mCallManager = new CallManager(c);

        File f = new File(mUserCertsPath);
        if (!f.exists()) {
            if (!f.mkdir()) {
                Log.e("[Manager] " + mUserCertsPath + " can't be created.");
            }
        }

        mMediaScanner = new MediaScanner(c);

        mCoreListener =
                new CoreListenerStub() {
                    @SuppressLint("Wakelock")
                    @Override
                    public void onCallStateChanged(
                            final Core core,
                            final Call call,
                            final State state,
                            final String message) {
                        Log.i("[Manager] Call state is [", state, "]");
                        if (state == State.IncomingReceived
                                && !call.equals(core.getCurrentCall())) {
                            if (call.getReplacedCall() != null) {
                                // attended transfer will be accepted automatically.
                                return;
                            }
                        }

                        if ((state == State.IncomingReceived || state == State.IncomingEarlyMedia)
                                && getCallGsmON()) {
                            if (mCore != null) {
                                call.decline(Reason.Busy);
                            }
                        } else if (state == State.IncomingReceived
                                && (LinphonePreferences.instance().isAutoAnswerEnabled())
                                && !getCallGsmON()) {
                            LinphoneUtils.dispatchOnUIThreadAfter(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            if (mCore != null) {
                                                if (mCore.getCallsNb() > 0) {
                                                    mCallManager.acceptCall(call);
                                                }
                                            }
                                        }
                                    },
                                    mPrefs.getAutoAnswerTime());
                        } else if (state == State.End || state == State.Error) {
                            if (mCore.getCallsNb() == 0) {
                                // Disabling proximity sensor
                                enableProximitySensing(false);
                            }
                        } else if (state == State.UpdatedByRemote) {
                            // If the correspondent proposes video while audio call
                            boolean remoteVideo = call.getRemoteParams().videoEnabled();
                            boolean localVideo = call.getCurrentParams().videoEnabled();
                            boolean autoAcceptCameraPolicy =
                                    LinphonePreferences.instance()
                                            .shouldAutomaticallyAcceptVideoRequests();
                            if (remoteVideo
                                    && !localVideo
                                    && !autoAcceptCameraPolicy
                                    && mCore.getConference() == null) {
                                call.deferUpdate();
                            }
                        }
                    }
                };
    }

    public static synchronized LinphoneManager getInstance() {
        LinphoneManager manager = LinphoneContext.instance().getLinphoneManager();
        if (manager == null) {
            throw new RuntimeException(
                    "[Manager] Linphone Manager should be created before accessed");
        }
        if (manager.mExited) {
            throw new RuntimeException(
                    "[Manager] Linphone Manager was already destroyed. "
                            + "Better use getCore and check returned value");
        }
        return manager;
    }

    public static synchronized AndroidAudioManager getAudioManager() {
        return getInstance().mAudioManager;
    }

    public static synchronized CallManager getCallManager() {
        return getInstance().mCallManager;
    }

    public static synchronized Core getCore() {
        if (!LinphoneContext.isReady()) return null;

        if (getInstance().mExited) {
            // Can occur if the UI thread play a posted event but in the meantime the
            // LinphoneManager was destroyed
            // Ex: stop call and quickly terminate application.
            return null;
        }
        return getInstance().mCore;
    }

    /* End of static */

    public MediaScanner getMediaScanner() {
        return mMediaScanner;
    }

    public synchronized void destroy() {
        destroyManager();
        // Wait for Manager to destroy everything before setting mExited to true
        // Otherwise some objects might crash during their own destroy if they try to call
        // LinphoneManager.getCore(), for example to unregister a listener
        mExited = true;
    }

    public void restartCore() {
        Log.w("[Manager] Restarting Core");
        mCore.stop();
        mCore.start();
    }

    private void destroyCore() {
        Log.w("[Manager] Destroying Core");
        mCore.stop();
        mCore.removeListener(mCoreListener);
    }

    private synchronized void destroyManager() {
        Log.w("[Manager] Destroying Manager");
        changeStatusToOffline();

        if (mTelephonyManager != null) {
            Log.i("[Manager] Unregistering phone state listener");
            mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
        }

        if (mCallManager != null) mCallManager.destroy();
        if (mMediaScanner != null) mMediaScanner.destroy();
        if (mAudioManager != null) mAudioManager.destroy();

        if (mTimer != null) mTimer.cancel();

        if (mCore != null) {
            destroyCore();
            mCore = null;
        }
    }

    public synchronized void startLibLinphone(boolean isPush, CoreListener listener) {
        try {
            mCore =
                    Factory.instance()
                            .createCore(
                                    mPrefs.getLinphoneDefaultConfig(),
                                    mPrefs.getLinphoneFactoryConfig(),
                                    mContext);
            mCore.addListener(listener);
            mCore.addListener(mCoreListener);

            if (isPush) {
                Log.w(
                        "[Manager] We are here because of a received push notification, enter background mode before starting the Core");
                mCore.enterBackground();
            }

            mCore.start();

            mIterateRunnable =
                    new Runnable() {
                        @Override
                        public void run() {
                            if (mCore != null) {
                                mCore.iterate();
                            }
                        }
                    };
            TimerTask lTask =
                    new TimerTask() {
                        @Override
                        public void run() {
                            LinphoneUtils.dispatchOnUIThread(mIterateRunnable);
                        }
                    };
            /*use schedule instead of scheduleAtFixedRate to avoid iterate from being call in burst after cpu wake up*/
            mTimer = new Timer("Linphone scheduler");
            mTimer.schedule(lTask, 0, 20);

            configureCore();
        } catch (Exception e) {
            Log.e(e, "[Manager] Cannot start linphone");
        }
    }

    private synchronized void configureCore() {
        Log.i("[Manager] Configuring Core");
        mAudioManager = new AndroidAudioManager(mContext);

        mCore.setZrtpSecretsFile(mBasePath + "/zrtp_secrets");
        String deviceName = mPrefs.getDeviceName(mContext);
        String appName = "LinphoneAndroid";
        String androidVersion = BuildConfig.VERSION_NAME;
        String userAgent = appName + "/" + androidVersion + " (" + deviceName + ") LinphoneSDK";

        mCore.setUserAgent(
                userAgent,
                "Linphone SDK 4.2"
                        + " ("
                        + "Linphone SDK Branch 4.2"
                        + ")");

        // mCore.setChatDatabasePath(mChatDatabaseFile);
        mCore.setCallLogsDatabasePath(mCallLogDatabaseFile);
        mCore.setFriendsDatabasePath(mFriendsDatabaseFile);
        mCore.setUserCertificatesPath(mUserCertsPath);
        // mCore.setCallErrorTone(Reason.NotFound, mErrorToneFile);
        enableDeviceRingtone(mPrefs.isDeviceRingtoneEnabled());

        int availableCores = Runtime.getRuntime().availableProcessors();
        Log.w("[Manager] MediaStreamer : " + availableCores + " cores detected and configured");

        mCore.migrateLogsFromRcToDb();

        // Migrate existing linphone accounts to have conference factory uri and LIME X3Dh url set
        String uri = "sip:conference-factory@sip.linphone.org";
        for (ProxyConfig lpc : mCore.getProxyConfigList()) {
            if (lpc.getIdentityAddress().getDomain().equals("sip.linphone.org")) {
                if (lpc.getConferenceFactoryUri() == null) {
                    lpc.edit();
                    Log.i(
                            "[Manager] Setting conference factory on proxy config "
                                    + lpc.getIdentityAddress().asString()
                                    + " to default value: "
                                    + uri);
                    lpc.setConferenceFactoryUri(uri);
                    lpc.done();
                }

                if (mCore.limeX3DhAvailable()) {
                    String url = mCore.getLimeX3DhServerUrl();
                    if (url == null || url.isEmpty()) {
                        url = "https://lime.linphone.org/lime-server/lime-server.php";
                        Log.i("[Manager] Setting LIME X3Dh server url to default value: " + url);
                        mCore.setLimeX3DhServerUrl(url);
                    }
                }
            }
        }
            
        // TODO
        // Pato watch out for this
//        if (mContext.getResources().getBoolean(R.bool.enable_push_id)) {
//            PushNotificationUtils.init(mContext);
//        }

        mProximityWakelock =
                mPowerManager.newWakeLock(
                        PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,
                        mContext.getPackageName() + ";manager_proximity_sensor");

        resetCameraFromPreferences();

        mAccountCreator = mCore.createAccountCreator(LinphonePreferences.instance().getXmlrpcUrl());
        mCallGsmON = false;

        Log.i("[Manager] Core configured");
    }

    public void resetCameraFromPreferences() {
        Core core = getCore();
        if (core == null) return;

        boolean useFrontCam = LinphonePreferences.instance().useFrontCam();
        String firstDevice = null;
        for (String camera : core.getVideoDevicesList()) {
            if (firstDevice == null) {
                firstDevice = camera;
            }

            if (useFrontCam) {
                if (camera.contains("Front")) {
                    Log.i("[Manager] Found front facing camera: " + camera);
                    core.setVideoDevice(camera);
                    return;
                }
            }
        }

        Log.i("[Manager] Using first camera available: " + firstDevice);
        core.setVideoDevice(firstDevice);
    }

    /* Account linking */

    public AccountCreator getAccountCreator() {
        if (mAccountCreator == null) {
            Log.w("[Manager] Account creator shouldn't be null !");
            mAccountCreator =
                    mCore.createAccountCreator(LinphonePreferences.instance().getXmlrpcUrl());
        }
        return mAccountCreator;
    }

    /* Presence stuff */

    private boolean isPresenceModelActivitySet() {
        if (mCore != null) {
            return mCore.getPresenceModel() != null
                    && mCore.getPresenceModel().getActivity() != null;
        }
        return false;
    }

    public void changeStatusToOnline() {
        if (mCore == null) return;
        PresenceModel model = mCore.createPresenceModel();
        model.setBasicStatus(PresenceBasicStatus.Open);
        mCore.setPresenceModel(model);
    }

    public void changeStatusToOnThePhone() {
        if (mCore == null) return;

        if (isPresenceModelActivitySet()
                && mCore.getPresenceModel().getActivity().getType()
                        != PresenceActivity.Type.OnThePhone) {
            mCore.getPresenceModel().getActivity().setType(PresenceActivity.Type.OnThePhone);
        } else if (!isPresenceModelActivitySet()) {
            PresenceModel model =
                    mCore.createPresenceModelWithActivity(PresenceActivity.Type.OnThePhone, null);
            mCore.setPresenceModel(model);
        }
    }

    private void changeStatusToOffline() {
        if (mCore != null) {
            PresenceModel model = mCore.getPresenceModel();
            model.setBasicStatus(PresenceBasicStatus.Closed);
            mCore.setPresenceModel(model);
        }
    }

    /* Tunnel stuff */

    public void initTunnelFromConf() {
        if (!mCore.tunnelAvailable()) return;

        @SuppressLint("MissingPermission") NetworkInfo info = mConnectivityManager.getActiveNetworkInfo();
        Tunnel tunnel = mCore.getTunnel();
        tunnel.cleanServers();
        TunnelConfig config = mPrefs.getTunnelConfig();
        if (config.getHost() != null) {
            tunnel.addServer(config);
            manageTunnelServer(info);
        }
    }

    private boolean isTunnelNeeded(NetworkInfo info) {
        if (info == null) {
            Log.i("[Manager] No connectivity: tunnel should be disabled");
            return false;
        }

        String pref = mPrefs.getTunnelMode();

        if ("always".equals(pref)) {
            return true;
        }

        if (info.getType() != ConnectivityManager.TYPE_WIFI
                && "3G_only".equals(pref)) {
            Log.i("[Manager] Need tunnel: 'no wifi' connection");
            return true;
        }

        return false;
    }

    private void manageTunnelServer(NetworkInfo info) {
        if (mCore == null) return;
        if (!mCore.tunnelAvailable()) return;
        Tunnel tunnel = mCore.getTunnel();

        Log.i("[Manager] Managing tunnel");
        if (isTunnelNeeded(info)) {
            Log.i("[Manager] Tunnel need to be activated");
            tunnel.setMode(Tunnel.Mode.Enable);
        } else {
            Log.i("[Manager] Tunnel should not be used");
            String pref = mPrefs.getTunnelMode();
            tunnel.setMode(Tunnel.Mode.Disable);
            if ("always".equals(pref)) {
                tunnel.setMode(Tunnel.Mode.Auto);
            }
        }
    }

    /* Proximity sensor stuff */

    public void enableProximitySensing(boolean enable) {
        if (enable) {
            if (!mProximitySensingEnabled) {
                mSensorManager.registerListener(
                        this, mProximity, SensorManager.SENSOR_DELAY_NORMAL);
                mProximitySensingEnabled = true;
            }
        } else {
            if (mProximitySensingEnabled) {
                mSensorManager.unregisterListener(this);
                mProximitySensingEnabled = false;
                // Don't forgeting to release wakelock if held
                if (mProximityWakelock.isHeld()) {
                    mProximityWakelock.release();
                }
            }
        }
    }

    private Boolean isProximitySensorNearby(final SensorEvent event) {
        float threshold = 4.001f; // <= 4 cm is near

        final float distanceInCm = event.values[0];
        final float maxDistance = event.sensor.getMaximumRange();
        Log.d(
                "[Manager] Proximity sensor report ["
                        + distanceInCm
                        + "] , for max range ["
                        + maxDistance
                        + "]");

        if (maxDistance <= threshold) {
            // Case binary 0/1 and short sensors
            threshold = maxDistance;
        }
        return distanceInCm < threshold;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.timestamp == 0) return;
        if (isProximitySensorNearby(event)) {
            if (!mProximityWakelock.isHeld()) {
                mProximityWakelock.acquire();
            }
        } else {
            if (mProximityWakelock.isHeld()) {
                mProximityWakelock.release();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    /* Other stuff */

    public void enableDeviceRingtone(boolean use) {
        if (use) {
            mCore.setRing(null);
        } else {
            mCore.setRing(mRingSoundFile);
        }
    }

    public boolean getCallGsmON() {
        return mCallGsmON;
    }

    public void setCallGsmON(boolean on) {
        mCallGsmON = on;
        if (on && mCore != null) {
            mCore.pauseAllCalls();
        }
    }

    private String getString(int key) {
        return mContext.getString(key);
    }

    public boolean hasLastCallSasBeenRejected() {
        return mHasLastCallSasBeenRejected;
    }

    public void lastCallSasRejected(boolean rejected) {
        mHasLastCallSasBeenRejected = rejected;
    }
}
