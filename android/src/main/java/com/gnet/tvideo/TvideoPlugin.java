package com.gnet.tvideo;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import io.flutter.Log;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.view.TextureRegistry;

import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.Build;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;


/**
 * TvideoPlugin
 */
public class TvideoPlugin implements FlutterPlugin, MethodCallHandler {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private MethodChannel channel;
    private long stagingTextureID = -1;
    private Surface stagingSurface = null;
    private MediaFormat format = null;
    private MediaCodec codec = null;
    private byte[] stagingData = null;
    private boolean firstPlay = false;
    private long timestamp = 0;
    private int width = 0;
    private int height = 0;
    private int fps = 0;
    private boolean hevc = false;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "tvideo");
        channel.setMethodCallHandler(this);

        TextureRegistry tr = flutterPluginBinding.getTextureRegistry();
        TextureRegistry.SurfaceTextureEntry ste = tr.createSurfaceTexture();
        stagingTextureID = ste.id();
        stagingSurface = new Surface(ste.surfaceTexture());
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        if (call.method.equals("getPlatformVersion")) {

            result.success("Android: " + android.os.Build.VERSION.RELEASE);
        } else if (call.method.equals("createCodec")) {
            width = call.argument("width");
            height = call.argument("height");
            fps = call.argument("codecNumber");
            byte[] byteData = call.argument("data");

            int res = createNewCodec(
                    call.argument("width"),
                    call.argument("height"),
                    call.argument("codecNumber"),
                    call.argument("data")
            );

        } else if (call.method.equals("getTextureId")) {
            if (stagingTextureID == -1) {
                result.error("error", "stagingTextureID: " + stagingTextureID, null);
                return;
            }

            result.success(stagingTextureID);
        } else if (call.method.equals("putData")) {
            // 성능 측정 필요
            byte[] byteData = call.argument("data");

            setStagingData(byteData);
            Log.i("tvideo", "Receivec data, len: " + byteData.length);

            result.success(0);
        } else if (call.method.equals("release")) {


            result.success(0);
        } else if (call.method.equals("isCodecInitialized")) {
            if (codec != null && format != null)
                result.success(true);
            else
                result.success(false);
        } else {
            result.notImplemented();
        }
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
    }


    void setStagingData(byte[] data) {
        synchronized (stagingData) {
            stagingData = data;
        }
    }

    void reset() {
        width = height = fps = 0;
        hevc = false;
        format = null;
        codec = null;
    }

    int createNewCodec(int width, int height, int fps, byte[] data) {
        if (width <= 0 || height <= 0 || fps == 0 || data.length == 0)
            return -1;

        reset();

        this.width = width;
        this.height = height;
        this.fps = fps;

        String codecString;

        if (data[4] == 0x67 || data[4] == 0x68)
            codecString = MediaFormat.MIMETYPE_VIDEO_AVC; // h264
        else if (data[4] == 0x40)
            codecString = MediaFormat.MIMETYPE_VIDEO_HEVC; // h265
        else {
            return -2;
        }

        format = MediaFormat.createVideoFormat(codecString, width, height);

        if (hevc)
            codecString = MediaFormat.MIMETYPE_VIDEO_HEVC;

        try {
            codec = MediaCodec.createDecoderByType(codecString);

            if (codec != null) {
                codec.setCallback(new MediaCodec.Callback() {
                    @Override
                    public void onInputBufferAvailable(@NonNull MediaCodec mediaCodec, int i) {
                        if (stagingData == null || stagingData.length == 0)
                            mediaCodec.queueInputBuffer(i, 0, 0, 0, 0);

                        ByteBuffer bb = mediaCodec.getInputBuffer(i);
                        bb.clear();
                        bb.put(stagingData);

                        mediaCodec.queueInputBuffer(i, 0, stagingData.length, 0, 0);
                    }

                    @Override
                    public void onOutputBufferAvailable(@NonNull MediaCodec mediaCodec, int i, @NonNull MediaCodec.BufferInfo bufferInfo) {
                        if (firstPlay) {
                            timestamp = System.nanoTime();
                            timestamp += 33333333;// 33.333333 ms => ;

                            firstPlay = false;
                        } else
                            timestamp += 33333333;

                        mediaCodec.releaseOutputBuffer(i, timestamp);
                    }

                    @Override
                    public void onError(@NonNull MediaCodec mediaCodec, @NonNull MediaCodec.CodecException e) {
                        android.util.Log.e("motrex_bb", "MediaCodec::onError " + e.getMessage());
                    }

                    @Override
                    public void onOutputFormatChanged(@NonNull MediaCodec mediaCodec, @NonNull MediaFormat mediaFormat) {
                        android.util.Log.i("motrex_bb", mediaFormat.toString());
                    }
                });
            }
        } catch (IOException e) {
            //result.error("-1","Fail to init codec", null);
            //throw new RuntimeException(e);
            format = format = null;
            return -2;
        }

        format.setInteger(MediaFormat.KEY_LOW_LATENCY, 1);
        codec.configure(format, stagingSurface, null, 0);
        //codec.start();

        return 0;
    }






    /**
     * Returns the first codec capable of encoding the specified MIME type, or
     * null if no match was found.
     */
    private MediaCodecInfo selectCodec(String mimeType) {
        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);

            if (!codecInfo.isEncoder()) {
                continue;
            }

            for (String type : codecInfo.getSupportedTypes()) {
                if (type.equalsIgnoreCase(mimeType)) {
                    Log.i("selectCodec", "SelectCodec : " + codecInfo.getName());
                    return codecInfo;
                }
            }
        }
        return null;
    }

    /**
     * Retruns a color format that is supported by the codec and by this test
     * code. If no match is found, this throws a test failure -- the set of
     * formats known to the test should be expanded for new platforms.
     */
    protected int selectColorFormat(String mimeType) {
        MediaCodecInfo codecInfo = selectCodec(mimeType);
        if (codecInfo == null) {
            throw new RuntimeException("Unable to find an appropriate codec for " + mimeType);
        }

        MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType(mimeType);
        for (int i = 0; i < capabilities.colorFormats.length; i++) {
            int colorFormat = capabilities.colorFormats[i];
            if (isRecognizedFormat(colorFormat)) {
                Log.d("ColorFomar", "Find a good color format for " + codecInfo.getName() + " / " + mimeType);
                return colorFormat;
            }
        }
        return -1;
    }

    /**
     * Returns true if this is a color format that this test code understands
     * (i.e. we know how to read and generate frames in this format).
     */
    private boolean isRecognizedFormat(int colorFormat) {
        switch (colorFormat) {
            // these are the formats we know how to handle for this test
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar:
                return true;
            default:
                return false;
        }
    }
}

