package app.storiez.plugins.flutter_linphone

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Handler
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodChannel
import org.linphone.LinphoneContext
import org.linphone.LinphoneManager
import org.linphone.core.*
import org.linphone.service.LinphoneService
import org.linphone.settings.LinphonePreferences
import java.util.*

class SipManager {
    private lateinit var channel: MethodChannel
    private var mListener: CoreListenerStub? = null
    var events: EventChannel.EventSink? = null
    private var context: Context? = null
    private var utils: Utils? = null

    //
    private val methodName: String = "method_state_changed"

    fun setup(context: Context, methodChannel: MethodChannel) {
        println("This was called \uD83E\uDD17\uD83E\uDD17\uD83E\uDD17\uD83E\uDD17\uD83E\uDD17\uD83E\uDD17")
        this.context = context
        this.channel = methodChannel

        if (utils == null) {
            utils = Utils()
        }
        mListener = object : CoreListenerStub() {
            override fun onCallStateChanged(
                core: Core, call: Call, state: Call.State, message: String
            ) {
                when (state) {
                    Call.State.Error -> {
                        channel.invokeMethod(
                            methodName,
                            "CallState.Error".toUpperCase(
                                Locale.ROOT
                            )
                        )

                        // Convert Core message for internalization
                        when (call.errorInfo.reason) {
                            Reason.Declined -> {
                                println("Call error -> Reason declined")
                            }
                            Reason.NotFound -> {
                                println("Call error -> Reason Not found")
                            }
                            Reason.NotAcceptable -> {
                                println("Call error -> Reason Not acceptable")
                            }
                            Reason.Busy -> {
                                println("Call error -> Reason busy")
                            }
                            else -> {
                                println("Call error")
                            }
                        }
                    }

                    else -> {
                        channel.invokeMethod(
                            methodName,
                            ("CallState.${state.toString()}").toUpperCase(
                                Locale.ROOT
                            )
                        )
                    }
                }
            }

            override fun onRegistrationStateChanged(
                core: Core,
                proxyConfig: ProxyConfig,
                state: RegistrationState,
                message: String
            ) {
                println("Registration state -> $state")
                println("Registration message -> $message")
                channel.invokeMethod(
                    methodName,
                    "RegistrationState.$state".toUpperCase(
                        Locale.ROOT
                    )
                )
            }

        }
        LinphoneManager.getCore()?.addListener(this.mListener)
    }

    fun sipPermission(activityPluginBinding: ActivityPluginBinding): Pair<Boolean, Exception?> {
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
                    activityPluginBinding.let {
                        utils!!.checkPermissions(
                            permissionArrays,
                            it
                        )
                    }
                return (isSuccess to null)
            } else {
                return (false to null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return (false to e)
        }
    }

    fun startLinphoneService(): Exception? {
        return try {
            if (!LinphoneService.isReady()) {
                LinphoneContext(this.context)
                LinphoneContext.instance().start(false)
            }
            null
        } catch (e: Exception) {
            e
        }
    }

    fun initConnect(
        username: String,
        domain: String,
        password: String,
        port: Int
    ): Pair<Boolean, Exception?> {
        try {
            println("called")
            val core = LinphoneManager.getCore()
            println("called: ${core != null}")
            if (core != null) {
                println("[Generic Connection Assistant] Reloading configuration with default")
                println("[Assistant] Reloading configuration with default")
                val path = LinphonePreferences.instance().defaultDynamicConfigFile
                core.loadConfigFromXml(path)
                val accountCreator = LinphoneManager.getInstance().accountCreator
                accountCreator!!.reset()
                accountCreator.language = Locale.getDefault().language
            }

            val accountCreator: AccountCreator = LinphoneManager.getInstance().accountCreator!!
            accountCreator.username = username
            accountCreator.domain = "$domain:$port"
            accountCreator.password = password
            accountCreator.displayName = username
            accountCreator.transport = TransportType.Udp // TODO

            val proxyConfig = LinphoneManager.getInstance().accountCreator!!.createProxyConfig()
            core.defaultProxyConfig = proxyConfig
            // If this isn't a sip.linphone.org account, disable push notifications and enable
            // service notification, otherwise incoming calls won't work (most probably)
            if (proxyConfig != null) {
                proxyConfig.isPushNotificationAllowed = false
            }
            println(
                "[Assistant] Unknown domain used, push probably won't work, enable service mode"
            )
            LinphonePreferences.instance().isPushNotificationEnabled = false
            LinphoneManager.getCore()?.addListener(this.mListener)
            if (proxyConfig == null) {
                println("[Assistant] Account creator couldn't create proxy config")
                // TODO: display error message
                return (false to Exception("Unable to create proxy Config"))
            } else {
                println("[Assistant] Account creator CREATED proxy config")
                LinphoneContext.instance().addCoreStartedListener {

                }
                LinphonePreferences.instance()
                    .firstLaunchSuccessful() //TODO: Patrick watch out for this
                return (true to null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return (false to e)
        }
    }

    fun discConnect(): Pair<Boolean, Exception?> {
        try {
            val core = LinphoneManager.getCore()
            val proxyConfig = LinphoneManager.getCore().defaultProxyConfig
            println(proxyConfig!!.domain)

            // Check if there any current ongoing and end them
            val callManager = LinphoneManager.getCallManager()
            callManager.terminateCurrentCallOrConferenceOrAll();

            // remove proxyConfig
            if (core != null) {
                println("Core is not null")
                val authInfo = proxyConfig.findAuthInfo()
                println("AuthInfo: ${authInfo!!.username}")
                core.removeProxyConfig(proxyConfig);
                println("Proxy After: ${LinphoneManager.getCore().defaultProxyConfig}")
                core.removeAuthInfo(authInfo)
            }

            // check if the linphoneService was running
            // if true we stop it and destroy it from the android system
            // if false do nothing

            LinphoneContext.instance().destroy()
            println("Linphone service is killed")
            return (true to null)
        } catch (e: Exception) {
            return (false to e)
        }
    }

    fun sipAudioCallStart(
        username: String, domain: String, port: Int
    ): Pair<Boolean, Exception?> {
        val sipUser = "sip:$username@$domain:$port"
        try {
            LinphoneManager.getCallManager().newOutgoingCall(sipUser, username)
            return (true to null)
        } catch (e: Exception) {
            e.printStackTrace()
            return (false to e)
        }
    }

    fun sipAudioCallHold(): Exception? {
        try {
            val calls = LinphoneManager.getCore().calls
            if (calls.isNotEmpty()) {
                // means there is a call in progress
                for (call in calls) {
                    var callState = call.state
                    if (callState == Call.State.Paused) {
                        call.resume()
                    } else {
                        call.pause()
                    }
                }
            } else {
                println("No calls are in progress")
            }
            return null
        } catch (e: Exception) {
            e.printStackTrace()
            return e
        }
    }

    fun sipAudioCallMute(): Exception? {
        try {
            val calls = LinphoneManager.getCore().calls
            if (calls.isNotEmpty()) {
                // means there is a call in progress
                for (call in calls) {
                    call.microphoneMuted = !call.microphoneMuted
                }
            } else {
                println("No calls are in progress")
            }
            return null
        } catch (e: Exception) {
            e.printStackTrace()
            return e
        }
    }

    fun sipAudioCallHangUp(): Exception? {
        try {
            val callManager = LinphoneManager.getCallManager()
            callManager.terminateCurrentCallOrConferenceOrAll()
            return null
        } catch (e: Exception) {
            e.printStackTrace()
            return e
        }
    }

    fun sipAudioCallSpeakerMode(): Exception? {
        // TODO
        return null
    }

    fun sipAudioCallState(): Pair<String?, Exception?> {
        try {
            val calls = LinphoneManager.getCore().calls
            if (calls.isNotEmpty()) {
                // means there is a call in progress
                val call = calls[0];
                return (call.state.toString() to null)
            } else {
                return ("No calls are in progress" to null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return (null to e)
        }
    }

    fun sipAudioCallDuration(): Pair<Int?, Exception?> {
        try {
            val calls = LinphoneManager.getCore().calls
            return if (calls.isNotEmpty()) {
                // means there is a call in progress
                val call = calls[0];
                (call.duration to null)
            } else {
                (0 to null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return (null to e)
        }
    }

    // TODO
    fun sipVideoCallStart() {}
    fun sipVideoCallChangeVideo() {}
    fun sipVideoCallTurnOffCamera() {}
    fun sipVideoCallEndCall() {}

    // gets
    fun getCoreListener(): CoreListenerStub? {
        return mListener;
    }

    fun getApplicationContext(): Context? {
        return context;
    }

    // MasterPiece
    fun destoryManager() {
        try {
            utils = null;
            LinphoneManager.getCore()?.removeListener(this.mListener)
        } catch (e: Exception) {

        }
    }
}