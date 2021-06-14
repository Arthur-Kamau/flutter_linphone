import 'package:flutter/material.dart';
import 'package:flutter_linphone/flutter_linphone.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  FlutterLinphone flutterLinphone = FlutterLinphone();
  String callState = "";
  int callDuration;
  @override
  void initState() {
    super.initState();
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
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Text("Call state: $callState"),
              Text("Call duration: $callDuration"),
              StreamBuilder(
                  stream: flutterLinphone.onSipStateChanged,
                  builder: (context, snapshot) {
                    if (snapshot.hasData) {
                      return Text("Call XstateX: ${snapshot.data}");
                    } else {
                      return Text("??");
                    }
                  }),
              SizedBox(height: 30),
              Wrap(
                spacing: 5,
                children: [
                  ElevatedButton(
                    child: Text("1. Get permissions"),
                    onPressed: () async {
                      bool res = await flutterLinphone.sipPermissions();
                      print("Sip permissions response: $res");
                    },
                  ),
                  ElevatedButton(
                    child: Text("1.1 Sip init"),
                    onPressed: () async {
                      try {
                        await flutterLinphone.sipInit();
                      } catch (e) {}
                      // print("Sip permissions response: $res");
                    },
                  ),
                  ElevatedButton(
                    child: Text("2. SIP connect"),
                    onPressed: () async {
                      String username = "0730303107";
                      String password = "d40ba9ed761bfc9d923371d7c0af6dc8";
                      String domain = "46.101.245.128";
                      try {
                        bool res = await flutterLinphone.sipConnect(
                          username: username,
                          password: password,
                          domain: domain,
                          port: 6000,
                        );
                        print("Sip connect response: $res");
                      } catch (e) {}
                    },
                  ),
                  ElevatedButton(
                    child: Text("3.1 Sip Start Audio Call"),
                    onPressed: () async {
                      try {
                        String username = "0727751832";
                        String domain = "46.101.245.128";
                        int port = 6000;
                        bool res = await flutterLinphone.sipAudioCall(
                          username: username,
                          domain: domain,
                          port: port,
                        );
                        print("Sip call response: $res");
                      } catch (e) {}
                    },
                  ),
                  ElevatedButton(
                    child: Text("3.2 Sip End Audio Call"),
                    onPressed: () async {
                      try {
                        bool res = await flutterLinphone.sipAudioHangUp();
                        print("Sip end call response: $res");
                      } catch (e) {}
                    },
                  ),
                  ElevatedButton(
                    child: Text("3.3 Sip Audio Call State"),
                    onPressed: () async {
                      try {
                        String res = await flutterLinphone.sipAudioCallState();
                        setState(() {
                          callState = res;
                        });
                      } catch (e) {}
                    },
                  ),
                  ElevatedButton(
                    child: Text("3.4 Sip Audio Call Duration"),
                    onPressed: () async {
                      try {
                        int res = await flutterLinphone.sipAudioCallDuration();
                        setState(() {
                          callDuration = res;
                        });
                      } catch (e) {}
                    },
                  ),
                  ElevatedButton(
                    child: Text("4. SIP disconnect"),
                    onPressed: () async {
                      bool res = await flutterLinphone.sipDisConnect();
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
