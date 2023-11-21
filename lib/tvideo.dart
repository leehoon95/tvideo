import 'tvideo_platform_interface.dart';
import 'dart:typed_data';

class Tvideo {
  static Future<String?> getPlatformVersion() {
    return TvideoPlatform.instance.getPlatformVersion();
  }

  static Future<int?> createCodec(int width, int height, Uint8List data) {
    return TvideoPlatform.instance.createCodec(width, height, data);
  }

  static Future<int?> getTextureId() {
    return TvideoPlatform.instance.getTextureId();
  }

  static Future<int?> putData(Uint8List data) {
    return TvideoPlatform.instance.putData(data);
  }

  static Future<void> release() {
    return TvideoPlatform.instance.release();
  }

  static Future<bool?> isCodecInitialized() {
    return TvideoPlatform.instance.isCodecInitialized();
  }
}
