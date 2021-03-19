package com.example.mediaplan.ogg;

public class OggLoader {

    static {
        System.loadLibrary("xiphogg");
    }

    public static native long init();

    public static native byte[] streamPacketin(long state,byte[] data,int number,long granulepos);

}
