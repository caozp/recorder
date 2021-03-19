//
// Created by 45959 on 2020/10/22.
//
#include <jni.h>
#include <android/log.h>
#include <stdio.h>
#include <string.h>
#include <malloc.h>
#include "com_example_mediaplan_ogg_OggLoader.h"
#include "ogg/include/ogg/ogg.h"

JNIEXPORT jlong JNICALL Java_com_example_mediaplan_ogg_OggLoader_init
(JNIEnv *env, jclass jcls){
ogg_stream_state *os = NULL;
os = static_cast<ogg_stream_state *>(_ogg_malloc(sizeof(ogg_stream_state)));
int result = ogg_stream_init(os,10);
if (result<0){
__android_log_print(ANDROID_LOG_INFO,"JNICALL","FAILED");
return -1;
}
__android_log_print(ANDROID_LOG_INFO,"JNICALL","SUCCESS");
    return (jlong)os;
}

JNIEXPORT jbyteArray JNICALL Java_com_example_mediaplan_ogg_OggLoader_streamPacketin
        (JNIEnv *env, jclass jcls, jlong state, jbyteArray data,jint number,jlong granulepos){
    ogg_stream_state *os = (ogg_stream_state*)state;

    jbyte * inputdata = env->GetByteArrayElements(data,0);
    jint size = env->GetArrayLength(data);

    ogg_packet *packet = NULL;
    packet = static_cast<ogg_packet *>(_ogg_malloc(sizeof(ogg_packet)));
    packet->packet = (unsigned char*)inputdata;
    packet->bytes = size;
    if (number == 0) {
        packet->b_o_s = 1;
    } else {
        packet->b_o_s = 0;
    }
    packet->e_o_s = 0;
    packet->packetno = number;
    packet->granulepos = granulepos;

    ogg_stream_packetin(os,packet);
    ogg_page *page = NULL;
    page = static_cast<ogg_page *>(_ogg_malloc(sizeof(ogg_page)));
    while(ogg_stream_flush(os,page)!=-1){
        int l = page->body_len+page->header_len;
        jbyteArray jbyteArray1 = env->NewByteArray(l);
        jbyte* pArray;
        pArray = (jbyte*)calloc(l, sizeof(jbyte));

        for(int i = 0;i<page->header_len;i++){
            *(pArray+i) = *(page->header+i);
        }

        for(int j = 0;j<page->body_len;j++){
            *(pArray+page->header_len+j) = *(page->body+j);
        }

        env->SetByteArrayRegion(jbyteArray1,0,l,pArray);
        return jbyteArray1;
    }
};