import 'package:flutter/services.dart';

import 'libserialport_listener_platform_interface.dart';

class MethodChannelLibserialportListener
    implements LibserialportListenerPlatform {
  final methodChannel = const MethodChannel('libserialport_listener');

  @override
  Future<void> startListerning(
      {required String path, Function(String event)? onEvent}) async {
    methodChannel.setMethodCallHandler((call) async {
      onEvent?.call(call.arguments.toString());
    });
    methodChannel.invokeMethod('start_listener', {
      'device_path': path,
    });
  }

  @override
  Future<List<String>> getDevicesPath() async {
    return (await methodChannel.invokeMethod('get_devices_path'))
        .map<String>((d) => d.toString())
        .toList();
  }
}
