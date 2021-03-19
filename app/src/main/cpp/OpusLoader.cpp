//
// Created by 45959 on 2020/10/25.
//
#include "com_example_mediaplan_opus_OpusLoader.h"
#include <android/log.h>
#include "opus/include/opus.h"

JNIEXPORT jlong JNICALL Java_com_example_mediaplan_opus_OpusLoader_createOpusEncoder
        (JNIEnv *env, jclass jcls, jint sampleRateInHz, jint channelConfig){
    OpusEncoder *encoder;
    int error;
    encoder = opus_encoder_create(sampleRateInHz,channelConfig,OPUS_APPLICATION_VOIP,&error);

    if(encoder) {
        opus_encoder_ctl(encoder,OPUS_SET_VBR(0));
        opus_encoder_ctl(encoder,OPUS_SET_PACKET_LOSS_PERC(0));
        opus_encoder_ctl(encoder,OPUS_SET_INBAND_FEC(0));
        opus_encoder_ctl(encoder,OPUS_SET_DTX(0));
        //OPUS_SET_LSB_DEPTH(16) would be appropriate
        //  * for 16-bit linear pcm input with opus_encode_float()
        opus_encoder_ctl(encoder,OPUS_SET_LSB_DEPTH(16));
        opus_encoder_ctl(encoder,OPUS_SET_BITRATE(32000));
    } else {
        __android_log_print(ANDROID_LOG_INFO,"JNIOPUS","init opus fail");
    }

    __android_log_print(ANDROID_LOG_INFO,"JNIOPUS","init opus success");
    return (jlong)encoder;
}


JNIEXPORT void JNICALL Java_com_example_mediaplan_opus_OpusLoader_destroyOpusEncoder
        (JNIEnv *env, jclass jcls, jlong encoder){
    OpusEncoder *opusEncoder = (OpusEncoder*)encoder;
    if(!opusEncoder){
        __android_log_print(ANDROID_LOG_INFO,"JNIOPUS","opusEncoder is null,destroy opusencoder fail");
        return;
    }
    opus_encoder_destroy(opusEncoder);
};


JNIEXPORT jint JNICALL Java_com_example_mediaplan_opus_OpusLoader_encodeFrame
        (JNIEnv *env, jclass jcls, jlong opusEncoder, jshortArray samples, jint offset, jbyteArray out) {
    OpusEncoder *pEnc = (OpusEncoder *) opusEncoder;
    if (!pEnc || !samples || !out)
        return 0;
    jshort *pSamples = env->GetShortArrayElements(samples, 0);
    jsize nSampleSize = env->GetArrayLength(samples);
    jbyte *pBytes = env->GetByteArrayElements(out, 0);
    jsize nByteSize = env->GetArrayLength(out);
    int nRet = opus_encode(pEnc, pSamples + offset, nSampleSize, (unsigned char *) pBytes,
                           nByteSize);
    env->ReleaseShortArrayElements(samples, pSamples, 0);
    env->ReleaseByteArrayElements(out, pBytes, 0);
    return nRet;

};

JNIEXPORT jlong JNICALL Java_com_example_mediaplan_opus_OpusLoader_createOpusDecoder
        (JNIEnv *env, jclass jcls, jint sampleRate, jint channelConfig){
    int error;
    OpusDecoder* decoder;

    decoder = opus_decoder_create(sampleRate,channelConfig,&error);

    if(!decoder) {
        __android_log_print(ANDROID_LOG_INFO,"JNIOPUS","init opus decoder fail");
    }

    return (jlong)decoder;
}

JNIEXPORT void JNICALL Java_com_example_mediaplan_opus_OpusLoader_destroyOpusDecoder
        (JNIEnv *env, jclass jcls, jlong decoder) {
    OpusDecoder *pDec  =  (OpusDecoder *)decoder;
    if (!pDec)
        return;

    opus_decoder_destroy(pDec);
}

JNIEXPORT jint JNICALL Java_com_example_mediaplan_opus_OpusLoader_decodeFrame
        (JNIEnv *env, jclass jcls, jlong opusDecoder, jbyteArray framedata,jshortArray outpcm){

    OpusDecoder *pDec = (OpusDecoder *) opusDecoder;
    if (!pDec || !outpcm || !framedata)
        return 0;
    jshort *pSamples = env->GetShortArrayElements(outpcm, 0);
    jbyte *pBytes = env->GetByteArrayElements(framedata, 0);
    jsize nByteSize = env->GetArrayLength(framedata);
    jsize nShortSize = env->GetArrayLength(outpcm);
    if (nByteSize <= 0 || nShortSize <= 0) {
        return -1;
    }
    int nRet = opus_decode(pDec, (unsigned char *) pBytes, nByteSize, pSamples, nShortSize, 0);
    env->ReleaseShortArrayElements(outpcm, pSamples, 0);
    env->ReleaseByteArrayElements(framedata, pBytes, 0);
    return nRet;
}
