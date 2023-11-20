import 'package:flutter_test/flutter_test.dart';
import 'package:tvideo/tvideo.dart';
import 'package:tvideo/tvideo_platform_interface.dart';
import 'package:tvideo/tvideo_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockTvideoPlatform
    with MockPlatformInterfaceMixin
    implements TvideoPlatform {

  @override
  Future<String?> getPlatformVersion() => Future.value('42');
}

void main() {
  final TvideoPlatform initialPlatform = TvideoPlatform.instance;

  test('$MethodChannelTvideo is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelTvideo>());
  });

  test('getPlatformVersion', () async {
    Tvideo tvideoPlugin = Tvideo();
    MockTvideoPlatform fakePlatform = MockTvideoPlatform();
    TvideoPlatform.instance = fakePlatform;

    expect(await tvideoPlugin.getPlatformVersion(), '42');
  });
}
