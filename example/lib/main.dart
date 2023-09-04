import 'package:flutter/material.dart';
import 'dart:async';

import 'package:libserialport_listener/libserialport_listener.dart';
import 'package:root_access/root_access.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  List<String> _devices = [];
  bool isReady = false;

  @override
  void initState() {
    super.initState();
    init();
  }

  Future<void> init() async {
    try {
      await RootAccess.requestRootAccess;
      _devices = await LibserialportListener.getDevicesPath();
      setState(() {
        isReady = true;
      });
    } catch (e) {
      print(e);
    }
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
          appBar: AppBar(
            title: const Text('Plugin example app'),
          ),
          body: (() {
            if (!isReady) {
              return const Center(
                child: Text('Loading'),
              );
            } else {
              if (_devices.isEmpty) {
                return const Center(
                  child: Text('Devices list is empty'),
                );
              } else {
                return Center(
                  child: SingleChildScrollView(
                    child: Column(
                      children: _devices.map<Widget>((d) => Text(d)).toList(),
                    ),
                  ),
                );
              }
            }
          }())),
    );
  }
}
