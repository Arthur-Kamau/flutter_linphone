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

import static android.content.Intent.ACTION_MAIN;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.ArrayList;

import org.linphone.core.Call;
import org.linphone.core.ConfiguringState;
import org.linphone.core.Core;
import org.linphone.core.CoreListenerStub;
import org.linphone.core.Factory;
import org.linphone.core.GlobalState;
import org.linphone.core.LogLevel;
import org.linphone.core.LoggingService;
import org.linphone.core.LoggingServiceListener;
import org.linphone.core.tools.Log;
import org.linphone.mediastream.Version;
import org.linphone.service.LinphoneService;
import org.linphone.settings.LinphonePreferences;
import org.linphone.utils.DeviceUtils;
import org.linphone.utils.LinphoneUtils;


public class LinphoneContext {
    private static LinphoneContext sInstance = null;

    private Context mContext;

    private final LoggingServiceListener mJavaLoggingService =
            new LoggingServiceListener() {
                @Override
                public void onLogMessageWritten(
                        LoggingService logService, String domain, LogLevel lev, String message) {
                    switch (lev) {
                        case Debug:
                            android.util.Log.d(domain, message);
                            break;
                        case Message:
                            android.util.Log.i(domain, message);
                            break;
                        case Warning:
                            android.util.Log.w(domain, message);
                            break;
                        case Error:
                            android.util.Log.e(domain, message);
                            break;
                        case Fatal:
                        default:
                            android.util.Log.wtf(domain, message);
                            break;
                    }
                }
            };
    private CoreListenerStub mListener;
    private LinphoneManager mLinphoneManager;
    private final ArrayList<CoreStartedListener> mCoreStartedListeners;

    public static boolean isReady() {
        return sInstance != null;
    }

    public static LinphoneContext instance() {
        if (sInstance == null) {
            throw new RuntimeException("[Context] Linphone Context not available!");
        }
        return sInstance;
    }

    public LinphoneContext(Context context) {
        mContext = context;
        mCoreStartedListeners = new ArrayList<>();
        LinphonePreferences.instance().setContext(context);
        LinphoneUtils.configureLoggingService(false, "linphone");
        sInstance = this;
        System.out.println("[Context] Ready");

        mListener =
                new CoreListenerStub() {
                    @Override
                    public void onGlobalStateChanged(Core core, GlobalState state, String message) {
                        System.out.println("[Context] Global state is:");
                        System.out.println(state.toString());

                        if (state == GlobalState.On) {
                            for (CoreStartedListener listener : mCoreStartedListeners) {
                                listener.onCoreStarted();
                            }
                        }
                    }

                    @Override
                    public void onConfiguringStatus(
                            Core core, ConfiguringState status, String message) {
                        System.out.println("[Context] Configuring state is:");
                        System.out.println(status.toString());

                        if (status == ConfiguringState.Successful) {
                            LinphonePreferences.instance()
                                    .setPushNotificationEnabled(false);
                        }else{
                            System.out.println("Configuring State failed");
                        }
                    }

                    @Override
                    public void onCallStateChanged(
                            Core core, Call call, Call.State state, String message) {
                        Log.i("[Context] Call state is [", state, "]");

                        // TODO
                        // Pato watched out on this ...
//                        if (mContext.getResources().getBoolean(R.bool.enable_call_notification)) {
//                            mNotificationManager.displayCallNotification(call);
//                        }

                        if (state == Call.State.IncomingReceived
                                || state == Call.State.IncomingEarlyMedia) {
                            // Starting SDK 24 (Android 7.0) we rely on the fullscreen intent of the
                            // call incoming notification
                            if (Version.sdkStrictlyBelow(Version.API24_NOUGAT_70)) {
                                if (!mLinphoneManager.getCallGsmON()) onIncomingReceived();
                            }

                            // In case of push notification Service won't be started until here
                            if (!LinphoneService.isReady()) {
                                Log.i("[Context] Service not running, starting it");
                                Intent intent = new Intent(ACTION_MAIN);
                                intent.setClass(mContext, LinphoneService.class);
                                mContext.startService(intent);
                            }
                        } else if (state == Call.State.OutgoingInit) {
                            onOutgoingStarted();
                        } else if (state == Call.State.Connected) {
                            onCallStarted();
                        } else if (state == Call.State.End
                                || state == Call.State.Released
                                || state == Call.State.Error) {
                            if (LinphoneService.isReady()) {
                            }

                            if (state == Call.State.Released
                                    && call.getCallLog().getStatus() == Call.Status.Missed) {
                            }
                        }
                    }
                };
//
        mLinphoneManager = new LinphoneManager(context);
    }

    public void start(boolean isPush) {
        Log.i("[Context] Starting, push status is ", isPush);
        mLinphoneManager.startLibLinphone(isPush, mListener);


    }

    public void destroy() {
        Log.i("[Context] Destroying");
        Core core = LinphoneManager.getCore();
        if (core != null) {
            core.removeListener(mListener);
            System.out.println("Core is killed");
            core = null; // To allow the gc calls below to free the Core
        }


        // Destroy the LinphoneManager second to last to ensure any getCore() call will work
        if (mLinphoneManager != null) {
            mLinphoneManager.destroy();
            System.out.println("Manager is killed");
        }

        // Wait for every other object to be destroyed to make LinphoneService.instance() invalid
        sInstance = null;

        if (LinphonePreferences.instance().useJavaLogger()) {
            Factory.instance().getLoggingService().removeListener(mJavaLoggingService);
        }
        LinphonePreferences.instance().destroy();
    }

    public void updateContext(Context context) {
        mContext = context;
    }

    public Context getApplicationContext() {
        return mContext;
    }

    /* Managers accessors */

    public LoggingServiceListener getJavaLoggingService() {
        return mJavaLoggingService;
    }

    public LinphoneManager getLinphoneManager() {
        return mLinphoneManager;
    }


    public void addCoreStartedListener(CoreStartedListener listener) {
        mCoreStartedListeners.add(listener);
    }

    public void removeCoreStartedListener(CoreStartedListener listener) {
        mCoreStartedListeners.remove(listener);
    }

    /* Log device related information */

    private void dumpDeviceInformation() {
        Log.i("==== Phone information dump ====");
        Log.i("DEVICE=" + Build.DEVICE);
        Log.i("MODEL=" + Build.MODEL);
        Log.i("MANUFACTURER=" + Build.MANUFACTURER);
        Log.i("ANDROID SDK=" + Build.VERSION.SDK_INT);
        StringBuilder sb = new StringBuilder();
        sb.append("ABIs=");
        for (String abi : Version.getCpuAbis()) {
            sb.append(abi).append(", ");
        }
        Log.i(sb.substring(0, sb.length() - 2));
    }

    /* Call activities */

    private void onIncomingReceived() {
//        Intent intent = new Intent(mContext, CallIncomingActivity.class);
//        // This flag is required to start an Activity from a Service context
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        mContext.startActivity(intent);
    }

    private void onOutgoingStarted() {
//        Intent intent = new Intent(mContext, CallOutgoingActivity.class);
//        // This flag is required to start an Activity from a Service context
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        mContext.startActivity(intent);
    }

    private void onCallStarted() {
//        Intent intent = new Intent(mContext, CallActivity.class);
//        // This flag is required to start an Activity from a Service context
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        mContext.startActivity(intent);
    }

    public interface CoreStartedListener {
        void onCoreStarted();
    }
}
