package app.storiez.plugins.flutter_linphone

import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding


class Utils {
    fun checkPermissions(
        permissions: Array<String>,
        activityPluginBinding: ActivityPluginBinding
    ): Boolean {
        var result: Int
        val listPermissionsNeeded: MutableList<String> = ArrayList()
        for (p in permissions) {
            result = ContextCompat.checkSelfPermission(
                activityPluginBinding.activity.applicationContext,
                p
            )
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p)
            }
        }
        if (listPermissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                activityPluginBinding.activity,
                listPermissionsNeeded.toTypedArray(),
                1111
            )
            return true
        }
        return false
    }
}