package app.storiez.plugins.flutter_linphone

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.annotation.NonNull
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import org.linphone.LinphoneContext
import org.linphone.LinphoneManager
import org.linphone.core.AccountCreator
import org.linphone.core.Call
import org.linphone.core.TransportType
import org.linphone.service.LinphoneService
import org.linphone.settings.LinphonePreferences
import java.util.*


/** FlutterLinphonePlugin */
class FlutterLinphonePlugin : FlutterPlugin, MethodCallHandler, ActivityAware {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private lateinit var channel: MethodChannel
    private var utils: Utils? = null
    private var activityPluginBinding: ActivityPluginBinding? = null
    var linphoneContext: LinphoneContext? = null

    init {
//        System.loadLibrary("androidbridge")
    }

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        println("onAttachedToEngine");
        if (utils == null) {
            utils = Utils()
        }

        ensureServiceIsRunning(flutterPluginBinding.applicationContext);

        if (LinphoneContext.isReady()) {
            println("LinphoneContext ............... is ready ")
        } else {
            println("LinphoneContext ...............WTF ---------- >> FIX ME ")
        }

        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "flutter_linphone")
        channel.setMethodCallHandler(this)
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        if (call.method == "getPlatformVersion") {
            result.success("Android ${android.os.Build.VERSION.RELEASE}")
        } else if (call.method == "request_permissions") {
            try {
                val permissionArrays = arrayOf<String>(
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.USE_SIP,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.MODIFY_AUDIO_SETTINGS,
                    Manifest.permission.ACCESS_NETWORK_STATE,
                    Manifest.permission.CHANGE_NETWORK_STATE,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.CHANGE_WIFI_STATE
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val isSuccess: Boolean =
                        activityPluginBinding?.let {
                            utils!!.checkPermissions(
                                permissionArrays,
                                it
                            )
                        } == true
                    if (isSuccess) {
                        result.success(true)
                    } else {
                        result.success(false)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                result.error(null, e.toString(), null)
            }
        } else if (call.method == "sip_init_connection") {
            try {
                val username: String = "0730303107"//call.argument("mssidn")!!
                val domain: String = "46.101.245.128"//call.argument("domain")!!
                val password: String =
                    "d40ba9ed761bfc9d923371d7c0af6dc8"//call.argument("password")!!
                configureAccount(
                    mUsername = username,
                    mPassword = password,
                    mDisplayName = username,
                    mDomain = domain
                )
                result.success(true)
            } catch (e: Exception) {
                println(e.toString())
                result.success(false)
            }
        } else if (call.method == "sip_call") {
            try {
                val username: String = "0727751832"//call.argument("mssidn")!!
                val domain: String = "46.101.245.128:6000"//call.argument("domain")!!
                println("================================")
                println("===============MSSIDN $username  domain $domain=================")
                println("================================")
                val sipUser = "sip:$username@$domain"
                newOutgoingCall(sipUser)
                result.success(true)
            } catch (e: Exception) {
                println(e.toString())
                result.success(false)
            }
        } else if (call.method == "sip_hangup") {
            try {
                decline()
                result.success(true)
            } catch (e: Exception) {
                println(e.toString())
                result.success(false)
            }
        } else if (call.method == "sip_disc_connection") {
            try {
                logout();
                result.success(true);
            } catch (e: Exception) {
                println(e.toString())
                result.success(false)
            }
        } else result.notImplemented()
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
        this.activityPluginBinding = null;
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activityPluginBinding = binding;
        if (utils == null) {
            utils = Utils()
        }
    }

    override fun onDetachedFromActivityForConfigChanges() {
        TODO("Not yet implemented")
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        activityPluginBinding = binding;
    }

    override fun onDetachedFromActivity() {
        TODO("Not yet implemented")
    }

    private fun ensureServiceIsRunning(context: Context) {
        if (!LinphoneService.isReady()) {
            if (!LinphoneContext.isReady()) {
                LinphoneContext(context);
                LinphoneContext.instance().start(false)
                println("[Generic Activity] Context created & started")
            }
            println("[Generic Activity] Starting Service")
//            try {
//                startService(Intent().setClass(this, LinphoneService::class.java))
//            } catch (ise: IllegalStateException) {
//                Log.e("[Generic Activity] Couldn't start service, exception: ", ise)
//            }
        }
    }


    // linphone methods
    private fun configureAccount(
        mUsername: String,
        mDomain: String,
        mPassword: String,
        mDisplayName: String
    ) {
        val core = LinphoneManager.getCore()
        if (core != null) {
            println("[Generic Connection Assistant] Reloading configuration with default")
            reloadDefaultAccountCreatorConfig()
        }
        val accountCreator: AccountCreator = getAccountCreator()!!
        accountCreator.username = mUsername
        accountCreator.domain = "$mDomain:6000"
        accountCreator.password = mPassword
        accountCreator.displayName = mDisplayName
        accountCreator.transport = TransportType.Udp

        createProxyConfig()
    }

    private fun reloadDefaultAccountCreatorConfig() {
        println("[Assistant] Reloading configuration with default")
        reloadAccountCreatorConfig(LinphonePreferences.instance().defaultDynamicConfigFile)
    }

    private fun getAccountCreator(): AccountCreator? {
        return LinphoneManager.getInstance().accountCreator
    }

    private fun reloadAccountCreatorConfig(path: String) {
        val core = LinphoneManager.getCore()
        if (core != null) {
            core.loadConfigFromXml(path)
            val accountCreator = getAccountCreator()
            accountCreator!!.reset()
            accountCreator.language = Locale.getDefault().language
        }
    }

    private fun createProxyConfig() {
        val core = LinphoneManager.getCore()

        val proxyConfig = getAccountCreator()!!.createProxyConfig()

        // If this isn't a sip.linphone.org account, disable push notifications and enable
        // service notification, otherwise incoming calls won't work (most probably)
        if (proxyConfig != null) {
            proxyConfig.isPushNotificationAllowed = false
        }
        println(
            "[Assistant] Unknown domain used, push probably won't work, enable service mode"
        )
//            LinphonePreferences.instance().serviceNotificationVisibility = true
//            LinphoneContext.instance().notificationManager.startForeground()


        LinphonePreferences.instance().isPushNotificationEnabled = false

        if (proxyConfig == null) {
            println("[Assistant] Account creator couldn't create proxy config")
            // TODO: display error message
        } else {
            println("[Assistant] Account creator CREATED proxy config")
//            if (proxyConfig.dialPrefix == null) {
//                val dialPlan = getDialPlanForCurrentCountry()
//                if (dialPlan != null) {
//                    proxyConfig.dialPrefix = dialPlan.countryCallingCode
//                }
//            }
            LinphonePreferences.instance().firstLaunchSuccessful() //!!!! Pato watch out for this
//            goToLinphoneActivity()
        }
    }

    private fun newOutgoingCall(to: String?) {
        LinphoneManager.getCallManager().newOutgoingCall(to, "Sample display name")
    }

    private fun decline() {
        var mCall: Call? = null
        // Only one call ringing at a time is allowed
        if (LinphoneManager.getCore() != null) {
            for (call in LinphoneManager.getCore().calls) {
                val cstate = call.state
                if (Call.State.OutgoingInit == cstate || Call.State.OutgoingProgress == cstate || Call.State.OutgoingRinging == cstate || Call.State.OutgoingEarlyMedia == cstate) {
                    mCall = call
                    break
                }
            }
        }
        if (mCall == null) {
            println("[Call Outgoing Activity] Couldn't find outgoing call")

            return
        } else {
            println("[Call Outgoing Activity] Couldn't find outgoing call")
            LinphoneManager.getCallManager().terminateCurrentCallOrConferenceOrAll()
            mCall?.terminate()
        }
    }

    private fun logout() {
        LinphoneManager.getInstance().accountCreator.reset();
        linphoneContext?.destroy()
    }
}
