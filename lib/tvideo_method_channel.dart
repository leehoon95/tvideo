import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'tvideo_platform_interface.dart';

/// An implementation of [TvideoPlatform] that uses method channels.
class MethodChannelTvideo extends TvideoPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('tvideo');

  @override
  Future<String?> getPlatformVersion() async {
    final version =
        await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }

  @override
  Future<int?> createCodec(int width, int height, Uint8List data) async {
    final res = await methodChannel.invokeMethod<int>('createCodec',
        <String, dynamic>{'width': width, 'height': height, 'data': data});
    return res;
  }

  @override
  Future<int?> getTextureId() async {
    final res = await methodChannel.invokeMethod<int>('getTextureId');
    return res;
  }

  @override
  Future<int?> putData(Uint8List data) async {
    final res = await methodChannel
        .invokeMethod<int>('putData', <String, dynamic>{'data': data});
    return res;
  }

  @override
  Future<bool?> isCodecInitialized() async {
    final res = await methodChannel.invokeMethod<bool>('isCodecInitialized');
    return res;
  }
}
