import 'dart:typed_data';

import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'tvideo_method_channel.dart';

abstract class TvideoPlatform extends PlatformInterface {
  /// Constructs a TvideoPlatform.
  TvideoPlatform() : super(token: _token);

  static final Object _token = Object();

  static TvideoPlatform _instance = MethodChannelTvideo();

  /// The default instance of [TvideoPlatform] to use.
  ///
  /// Defaults to [MethodChannelTvideo].
  static TvideoPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [TvideoPlatform] when
  /// they register themselves.
  static set instance(TvideoPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }

  Future<int?> createCodec(int width, int height, Uint8List data) {
    throw UnimplementedError('createCodec() has not been implemented.');
  }

  Future<int?> getTextureId() {
    throw UnimplementedError('getTextureId() has not been implemented.');
  }

  Future<int?> putData(Uint8List data) {
    throw UnimplementedError('putData() has not been implemented.');
  }

  Future<void> release() {
    throw UnimplementedError('release() has not been implemented.');
  }

  Future<bool?> isCodecInitialized() {
    throw UnimplementedError('isCodecInitialized() has not been implemented.');
  }
}
