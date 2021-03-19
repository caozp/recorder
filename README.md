# RecordVoice

1. 通过AudioTrack录制pcm数据
2. 为pcm加上头数据，转换成wav格式
3. 通过MediaCodec编码录制AAC
4. pcm进行opus压缩
5. opus压缩后再录制成ogg