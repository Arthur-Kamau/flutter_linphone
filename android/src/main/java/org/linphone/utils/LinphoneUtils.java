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
package org.linphone.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.telephony.TelephonyManager;

import org.linphone.LinphoneContext;
import org.linphone.LinphoneManager;
import org.linphone.core.Address;
import org.linphone.core.Call;
import org.linphone.core.Factory;
import org.linphone.core.LogCollectionState;
import org.linphone.core.ProxyConfig;
import org.linphone.settings.LinphonePreferences;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/** Helpers. */
public final class LinphoneUtils {
    private static final Handler sHandler = new Handler(Looper.getMainLooper());

    private LinphoneUtils() {}

    public static void configureLoggingService(boolean isDebugEnabled, String appName) {
        if (!LinphonePreferences.instance().useJavaLogger()) {
            Factory.instance().enableLogCollection(LogCollectionState.Enabled);
            Factory.instance().setDebugMode(isDebugEnabled, appName);
        } else {
            Factory.instance().setDebugMode(isDebugEnabled, appName);
            Factory.instance()
                    .enableLogCollection(LogCollectionState.EnabledWithoutPreviousLogHandler);
            if (isDebugEnabled) {
                if (LinphoneContext.isReady()) {
                    Factory.instance()
                            .getLoggingService()
                            .addListener(LinphoneContext.instance().getJavaLoggingService());
                }
            } else {
                if (LinphoneContext.isReady()) {
                    Factory.instance()
                            .getLoggingService()
                            .removeListener(LinphoneContext.instance().getJavaLoggingService());
                }
            }
        }
    }

    public static void dispatchOnUIThread(Runnable r) {
        sHandler.post(r);
    }

    public static void dispatchOnUIThreadAfter(Runnable r, long after) {
        sHandler.postDelayed(r, after);
    }

    public static void removeFromUIThreadDispatcher(Runnable r) {
        sHandler.removeCallbacks(r);
    }

    private static boolean isSipAddress(String numberOrAddress) {
        Factory.instance().createAddress(numberOrAddress);
        return true;
    }

    public static boolean isNumberAddress(String numberOrAddress) {
        ProxyConfig proxy = LinphoneManager.getCore().createProxyConfig();
        return proxy.normalizePhoneNumber(numberOrAddress) != null;
    }

    public static boolean isStrictSipAddress(String numberOrAddress) {
        return isSipAddress(numberOrAddress) && numberOrAddress.startsWith("sip:");
    }

    public static String getDisplayableAddress(Address addr) {
        return "sip:" + addr.getUsername() + "@" + addr.getDomain();
    }

    public static String getAddressDisplayName(String uri) {
        Address lAddress;
        lAddress = Factory.instance().createAddress(uri);
        return getAddressDisplayName(lAddress);
    }

    public static String getAddressDisplayName(Address address) {
        if (address == null) return null;

        String displayName = address.getDisplayName();
        if (displayName == null || displayName.isEmpty()) {
            displayName = address.getUsername();
        }
        if (displayName == null || displayName.isEmpty()) {
            displayName = address.asStringUriOnly();
        }
        return displayName;
    }

    public static String timestampToHumanDate(Context context, long timestamp, int format) {
        return timestampToHumanDate(context, timestamp, context.getString(format));
    }

    public static String timestampToHumanDate(Context context, long timestamp, String format) {
        try {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(timestamp * 1000); // Core returns timestamps in seconds...

            SimpleDateFormat dateFormat;
            if (isToday(cal)) {
                dateFormat =
                        new SimpleDateFormat(
                                "HH:mm",
                                Locale.getDefault());
            } else {
                dateFormat = new SimpleDateFormat(format, Locale.getDefault());
            }

            return dateFormat.format(cal.getTime());
        } catch (NumberFormatException nfe) {
            return String.valueOf(timestamp);
        }
    }

    private static boolean isToday(Calendar cal) {
        return isSameDay(cal, Calendar.getInstance());
    }

    private static boolean isSameDay(Calendar cal1, Calendar cal2) {
        if (cal1 == null || cal2 == null) {
            return false;
        }

        return (cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA)
                && cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
                && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR));
    }

    private static boolean isCallRunning(Call call) {
        if (call == null) {
            return false;
        }

        Call.State state = call.getState();

        return state == Call.State.Connected
                || state == Call.State.Updating
                || state == Call.State.UpdatedByRemote
                || state == Call.State.StreamsRunning
                || state == Call.State.Resuming;
    }

    public static boolean isCallEstablished(Call call) {
        if (call == null) {
            return false;
        }

        Call.State state = call.getState();

        return isCallRunning(call)
                || state == Call.State.Paused
                || state == Call.State.PausedByRemote
                || state == Call.State.Pausing;
    }

    public static boolean isHighBandwidthConnection(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        @SuppressLint("MissingPermission") NetworkInfo info = cm.getActiveNetworkInfo();
        return (info != null
                && info.isConnected()
                && isConnectionFast(info.getType(), info.getSubtype()));
    }

    private static boolean isConnectionFast(int type, int subType) {
        if (type == ConnectivityManager.TYPE_MOBILE) {
            switch (subType) {
                case TelephonyManager.NETWORK_TYPE_EDGE:
                case TelephonyManager.NETWORK_TYPE_GPRS:
                case TelephonyManager.NETWORK_TYPE_IDEN:
                    return false;
            }
        }
        // in doubt, assume connection is good.
        return true;
    }


}
