import 'libserialport_listener_platform_interface.dart';

class LibserialportListener {
  static Future<void> startListerning(
      {required String path, Function(String event)? onEvent}) async {
    await LibserialportListenerPlatform.instance
        .startListerning(path: path, onEvent: onEvent);
  }

  static Future<List<String>> getDevicesPath() async {
    return LibserialportListenerPlatform.instance.getDevicesPath();
  }
}
