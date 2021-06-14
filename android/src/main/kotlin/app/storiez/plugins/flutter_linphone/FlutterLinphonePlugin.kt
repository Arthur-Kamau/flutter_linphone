package app.storiez.plugins.flutter_linphone

import android.os.Handler
import android.os.Looper
import androidx.annotation.NonNull
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import org.linphone.core.*
import java.util.*


/** FlutterLinphonePlugin */
class FlutterLinphonePlugin : FlutterPlugin, MethodCallHandler, ActivityAware {
    private lateinit var channel: MethodChannel
    private var sipManager: SipManager? = null
    private var activityPluginBinding: ActivityPluginBinding? = null


    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "flutter_linphone")
        channel.setMethodCallHandler(this)
        channel.invokeMethod("method_state_changed", "UNKNOWN")
        if (sipManager == null) {
            sipManager = SipManager()
        }
        sipManager!!.setup(flutterPluginBinding.applicationContext,channel)
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        when (call.method) {
            "request_permissions" -> {
                val (boolean, exception) = sipManager!!.sipPermission(
                    activityPluginBinding!!
                )
                if (exception == null) {
                    result.success(boolean)
                } else {
                    result.error(null, exception.toString(), null)
                }
            }
            "sip_init" -> {
                if (sipManager == null) {
                    println("Fuckkk")
                }
                val exception = sipManager!!.startLinphoneService()
                if (exception == null) {
                    result.success(true)
                } else {
                    result.error(null, exception.toString(), null)
                }
            }
            "sip_init_connection" -> {
                val username: String = call.argument("username")!!
                val domain: String = call.argument("domain")!!
                val password: String = call.argument("password")!!
                val port: Int = call.argument("port")!!
                val (boolean, exception) = sipManager!!.initConnect(
                    username, domain, password, port
                )
                if (exception == null) {
                    result.success(boolean)
                } else {
                    result.error(null, exception.toString(), null)
                }
            }
            "sip_audio_call" -> {
                val username: String = call.argument("mssidn")!!
                val domain: String = call.argument("domain")!!
                val port: Int = call.argument("port")!!
                val (boolean, exception) = sipManager!!.sipAudioCallStart(
                    username, domain, port
                )
                if (exception == null) {
                    result.success(boolean)
                } else {
                    result.error(null, exception.toString(), null)
                }
            }
            "sip_audio_hangup" -> {
                val exception = sipManager!!.sipAudioCallHangUp()
                if (exception == null) {
                    result.success(true)
                } else {
                    result.error(null, exception.toString(), null)
                }
            }
            "sip_call_state" -> {
                val (state, exception) = sipManager!!.sipAudioCallState()
                if (exception == null) {
                    result.success(state)
                } else {
                    result.error(null, exception.toString(), null)
                }
            }
            "sip_call_duration" -> {
                val (duration, exception) = sipManager!!.sipAudioCallDuration()
                if (exception == null) {
                    result.success(duration)
                } else {
                    result.error(null, exception.toString(), null)
                }
            }
            "sip_call_mute" -> {
                val exception = sipManager!!.sipAudioCallMute()
                if (exception == null) {
                    result.success("Okay")
                } else {
                    result.error(null, exception.toString(), null)
                }
            }
            "sip_call_hold" -> {
                val exception = sipManager!!.sipAudioCallHold()
                if (exception == null) {
                    result.success("Okay")
                } else {
                    result.error(null, exception.toString(), null)
                }
            }
            "sip_disc_connection" -> {
                val (boolean, exception) = sipManager!!.discConnect()
                if (exception == null) {
                    result.success(boolean)
                } else {
                    result.error(null, exception.toString(), null)
                }
            }
            else -> result.notImplemented()
        }
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
        this.activityPluginBinding = null;
        sipManager!!.destoryManager()
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activityPluginBinding = binding;
        if (sipManager == null) {
            sipManager = SipManager()
        }
        sipManager!!.setup(binding.activity.applicationContext,channel)
    }

    override fun onDetachedFromActivityForConfigChanges() {}
    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        activityPluginBinding = binding;
    }

    override fun onDetachedFromActivity() {}

}
