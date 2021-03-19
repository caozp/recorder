package com.example.mediaplan.opus;

public class OpusLoader {

    static {
        System.loadLibrary("xiphopus");
    }

    public static native long createOpusEncoder(int sampleRateInHz,int channelConfig);


    public static native void destroyOpusEncoder(long opusEncoder);

    public static native int encodeFrame(long opusEncoder,short[] pcm,int offset,byte[] out);


    public static native long createOpusDecoder(int sampleRateInHz,int channelConfig);

    public static native void destroyOpusDecoder(long opusDecoder);

    public static native int decodeFrame(long opusDecoder,byte[] framedata,short[] pcmdata);
}
