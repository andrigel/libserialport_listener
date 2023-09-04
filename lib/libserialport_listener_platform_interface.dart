import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'libserialport_listener_method_channel.dart';

abstract class LibserialportListenerPlatform extends PlatformInterface {
  LibserialportListenerPlatform() : super(token: _token);

  static final Object _token = Object();

  static LibserialportListenerPlatform _instance =
      MethodChannelLibserialportListener();

  static LibserialportListenerPlatform get instance => _instance;

  static set instance(LibserialportListenerPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<void> startListerning(
      {required String path, Function(String event)? onEvent}) {
    throw UnimplementedError('startListerning() has not been implemented.');
  }

  Future<List<String>> getDevicesPath() async {
    throw UnimplementedError('getDevicesPath() has not been implemented.');
  }
}
