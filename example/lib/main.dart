import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:flutter_linphone/flutter_linphone.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _platformVersion = 'Unknown';

  @override
  void initState() {
    super.initState();
    initPlatformState();
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> initPlatformState() async {
    String platformVersion;
    // Platform messages may fail, so we use a try/catch PlatformException.
    try {
      platformVersion = await FlutterLinphone.platformVersion;
    } on PlatformException {
      platformVersion = 'Failed to get platform version.';
    }

    // If the widget was removed from the tree while the asynchronous platform
    // message was in flight, we want to discard the reply rather than calling
    // setState to update our non-existent appearance.
    if (!mounted) return;

    setState(() {
      _platformVersion = platformVersion;
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Center(
          child: Column(
            children: [
              Text('Running on: $_platformVersion\n'),
              Wrap(
                spacing: 5,
                children: [
                  ElevatedButton(
                    child: Text("1. Get permissions"),
                    onPressed: () async {
                      bool res = await FlutterLinphone.sipPermissions();
                      print("Sip permissions response: $res");
                    },
                  ),
                  ElevatedButton(
                    child: Text("1.1 Sip init"),
                    onPressed: () async {
                      await FlutterLinphone.sipInit();
                      // print("Sip permissions response: $res");
                    },
                  ),
                  ElevatedButton(
                    child: Text("2. SIP connect"),
                    onPressed: () async {
                      // String username = "0730303046";//"0730303120";
                      // String password =
                      //     "15ccd10395975a6a583fb20ada1ad0b5";
                      // String domain = "64.225.106.148";
                      bool res = await FlutterLinphone.sipConnect();
                      print("Sip connect response: $res");
                    },
                  ),
                  ElevatedButton(
                    child: Text("3.1 Sip Start Audio Call"),
                    onPressed: () async {
                      await FlutterLinphone.sipAudioCall();
                    },
                  ),
                  ElevatedButton(
                    child: Text("3.2 Sip End Audio Call"),
                    onPressed: () async {
                      await FlutterLinphone.sipAudioHangUp();
                    },
                  ),
                  ElevatedButton(
                    child: Text("4. SIP disconnect"),
                    onPressed: () async {
                      bool res = await FlutterLinphone.sipDisConnect();
                      print("Sip disconnect response: $res");
                    },
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}
