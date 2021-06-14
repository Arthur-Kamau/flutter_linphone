import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class FlutterLinphone {
  static final flutterLinphone = FlutterLinphone();
  StreamController<String> _sipStateController =
      StreamController<String>.broadcast();
  static const MethodChannel _channel = const MethodChannel('flutter_linphone');

  static final FlutterLinphone _flutterLinphone = FlutterLinphone._internal();
  factory FlutterLinphone() {
    return _flutterLinphone;
  }
  FlutterLinphone._internal() {
    _channel.setMethodCallHandler((MethodCall call) async {
      try {
        _doHandlePlatformCall(call);
      } catch (exception) {
        print('Unexpected error: $exception');
      }
    });
  }

  Future<void> _doHandlePlatformCall(MethodCall call) async {
    final String callArgs = call.arguments as String;
    //   final remoteUri = callArgs['remote_uri'];
    switch (call.method) {
      case 'method_state_changed':
        if (callArgs == "REGISTRATIONSTATE.CLEARED") {
          List<Future> futures = [];
          if (!_sipStateController.isClosed)
            futures.add(_sipStateController.close());
          await Future.wait(futures);
        }
        flutterLinphone._sipStateController.add(callArgs);
        break;

      default:
        print('Unknown method ${call.method} ');
    }
  }

  Stream get onSipStateChanged {
    if (_sipStateController.isClosed) {
      _sipStateController = StreamController<String>.broadcast();
      _channel.setMethodCallHandler((MethodCall call) async {
        try {
          _doHandlePlatformCall(call);
        } catch (exception) {
          print('Unexpected error: $exception');
        }
      });
    }
    return _sipStateController.stream;
  }

  Future<void> sipInit() async {
    return await _channel.invokeMethod('sip_init');
  }

  /// sipPermissions
  Future<bool> sipPermissions() async {
    return await _channel.invokeMethod('request_permissions');
  }

  /// sipInitPlugin
  /// must be called otherwise some core functionalities
  /// will not work
  Future<bool> sipInitPlugin() async {
    return await _channel.invokeMethod('sip_init');
  }

  /// sipConnect
  Future<bool> sipConnect({
    @required String username,
    @required String password,
    @required String domain,
    int port = 5060,
  }) async {
    final bool response = await _channel.invokeMethod(
      'sip_init_connection',
      <String, dynamic>{
        'username': username,
        'password': password,
        'domain': domain,
        'port': port,
      },
    );
    return response;
  }

  /// sipAudioCall
  Future<bool> sipAudioCall({
    @required String username,
    @required String domain,
    int port = 5060,
  }) async {
    return await _channel.invokeMethod(
      'sip_audio_call',
      <String, dynamic>{
        'mssidn': username,
        'domain': domain,
        'port': port,
      },
    );
  }

  /// sipAudioHangUp
  Future<bool> sipAudioHangUp() async {
    return await _channel.invokeMethod('sip_audio_hangup');
  }

  /// sipAudioCallState
  Future<String> sipAudioCallState() async {
    return await _channel.invokeMethod('sip_call_state');
  }

  /// sipAudioCallDuration
  Future<int> sipAudioCallDuration() async {
    return await _channel.invokeMethod('sip_call_duration');
  }

  /// sipAudioCallMute
  Future<String> sipAudioCallMute() async {
    return await _channel.invokeMethod('sip_call_mute');
  }

  /// sipAudioCallHold
  Future<String> sipAudioCallHold() async {
    return await _channel.invokeMethod('sip_call_hold');
  }

  /// sipDisConnect
  Future<bool> sipDisConnect() async {
    return await _channel.invokeMethod('sip_disc_connection');
  }
}
