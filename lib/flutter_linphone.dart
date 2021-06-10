import 'dart:async';

import 'package:flutter/services.dart';

class FlutterLinphone {
  static const MethodChannel _channel = const MethodChannel('flutter_linphone');

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  static Future<bool> sipPermissions() async {
    bool response = await _channel.invokeMethod('request_permissions');
    return response;
  }

  static Future<bool> sipConnect() async {
    final bool response = await _channel.invokeMethod('sip_init_connection');
    return response;
  }

  static Future<bool> sipDisConnect() async {
    final bool response = await _channel.invokeMethod('sip_disc_connection');
    return response;
  }

  static Future<void> sipAudioCall() async {
    return await _channel.invokeMethod('sip_call');
  }

  static Future<void> sipAudioHangUp() async{
    return await _channel.invokeMethod('sip_hangup');
  }
}
