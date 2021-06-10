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

import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import java.util.List;

import org.linphone.core.tools.Log;
import org.linphone.settings.LinphonePreferences;

public class DeviceUtils {
    private static final Intent[] POWERMANAGER_INTENTS = {
        new Intent()
                .setComponent(
                        new ComponentName(
                                "com.miui.securitycenter",
                                "com.miui.permcenter.autostart.AutoStartManagementActivity")),
        new Intent()
                .setComponent(
                        new ComponentName(
                                "com.letv.android.letvsafe",
                                "com.letv.android.letvsafe.AutobootManageActivity")),
        new Intent()
                .setComponent(
                        new ComponentName(
                                "com.huawei.systemmanager",
                                "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity")),
        new Intent()
                .setComponent(
                        new ComponentName(
                                "com.huawei.systemmanager",
                                "com.huawei.systemmanager.optimize.process.ProtectActivity")),
        new Intent()
                .setComponent(
                        new ComponentName(
                                "com.coloros.safecenter",
                                "com.coloros.safecenter.permission.startup.StartupAppListActivity")),
        new Intent()
                .setComponent(
                        new ComponentName(
                                "com.coloros.safecenter",
                                "com.coloros.safecenter.startupapp.StartupAppListActivity")),
        new Intent()
                .setComponent(
                        new ComponentName(
                                "com.oppo.safe",
                                "com.oppo.safe.permission.startup.StartupAppListActivity")),
        new Intent()
                .setComponent(
                        new ComponentName(
                                "com.iqoo.secure",
                                "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")),
        new Intent()
                .setComponent(
                        new ComponentName(
                                "com.iqoo.secure",
                                "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager")),
        new Intent()
                .setComponent(
                        new ComponentName(
                                "com.vivo.permissionmanager",
                                "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")),
        new Intent()
                .setComponent(
                        new ComponentName(
                                "com.samsung.android.lool",
                                "com.samsung.android.sm.ui.battery.BatteryActivity")),
        new Intent()
                .setComponent(
                        new ComponentName(
                                "com.htc.pitroad",
                                "com.htc.pitroad.landingpage.activity.LandingPageActivity")),
        new Intent()
                .setComponent(
                        new ComponentName(
                                "com.asus.mobilemanager", "com.asus.mobilemanager.MainActivity")),
        new Intent()
                .setComponent(
                        new ComponentName(
                                "com.asus.mobilemanager",
                                "com.asus.mobilemanager.autostart.AutoStartActivity")),
        new Intent()
                .setComponent(
                        new ComponentName(
                                "com.asus.mobilemanager",
                                "com.asus.mobilemanager.entry.FunctionActivity"))
                .setData(Uri.parse("mobilemanager://function/entry/AutoStart")),
        new Intent()
                .setComponent(
                        new ComponentName(
                                "com.dewav.dwappmanager",
                                "com.dewav.dwappmanager.memory.SmartClearupWhiteList"))
    };

    public static Intent getDevicePowerManagerIntent(Context context) {
        for (Intent intent : POWERMANAGER_INTENTS) {
            if (DeviceUtils.isIntentCallable(context, intent)) {
                return intent;
            }
        }
        return null;
    }

    public static boolean hasDevicePowerManager(Context context) {
        return getDevicePowerManagerIntent(context) != null;
    }

    private static boolean isIntentCallable(Context context, Intent intent) {
        List<ResolveInfo> list =
                context.getPackageManager()
                        .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }
}
