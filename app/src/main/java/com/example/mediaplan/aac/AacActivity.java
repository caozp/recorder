package com.example.mediaplan.aac;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mediaplan.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class AacActivity extends AppCompatActivity {

    public static final int AUDIO_RATE = 44100;//采样率
    public static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_STEREO;
    public static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    String filename;// wav文件名

    AudioRecord audioRecord;
    int bufferSize;

    Button btnStart;
    Button btnStop;

    boolean isReocrding = false;

    MediaCodec mediaCodec;
    MediaCodec.BufferInfo encodeBufferInfo;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aac);

        btnStop =findViewById(R.id.stop);
        btnStart = findViewById(R.id.record);

        filename = getExternalFilesDir("record").getPath()+ File.separator+"test.aac";


        initRecord();//初始化

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnStop.setEnabled(true);
                btnStart.setEnabled(false);
                startRecord();
            }
        });

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnStop.setEnabled(false);
                btnStart.setEnabled(true);
                stopRecord();
            }
        });
    }

    private void initMediaCodec() throws IOException {
        MediaFormat encodeFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, 44100, 2);
        encodeFormat.setInteger(MediaFormat.KEY_BIT_RATE, 44100);//比特率
        encodeFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        encodeFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 100 * 1024);
        mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);//创建一个解码器
        mediaCodec.configure(encodeFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

        encodeBufferInfo = new MediaCodec.BufferInfo();
    }

    private void initRecord(){
        bufferSize = AudioRecord.getMinBufferSize(AUDIO_RATE,CHANNEL_CONFIG,AUDIO_FORMAT);
        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                AUDIO_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
        );
    }

    public void startRecord(){
        AacThread aacThread = new AacThread();
        aacThread.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (audioRecord != null) {
            audioRecord.release();
        }
        if (mediaCodec != null) {
            mediaCodec.release();
        }
    }

    public void stopRecord(){
        isReocrding = false;

        audioRecord.stop();
        mediaCodec.stop();
    }

    class AacThread extends Thread{

        public AacThread() {
            try {
                initMediaCodec();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        @Override
        public void run() {
            super.run();
            audioRecord.startRecording();
            isReocrding = true;
            File file = new File(filename);
            mediaCodec.start();
            try {
                FileOutputStream outputStream = new FileOutputStream(file);
                byte[] buf = new byte[1024];

                while (isReocrding) {
                    audioRecord.read(buf, 0, buf.length);
                    onFrame(buf,outputStream);
                }

                outputStream.flush();
                outputStream.close();
            } catch (FileNotFoundException E){
                Log.d("exception",E.getMessage());
            } catch (IOException E){
                Log.d("exception",E.getMessage());
            }
        }

        public void onFrame(byte[] data, OutputStream out){
            int inputIndex = mediaCodec.dequeueInputBuffer(-1);//获取输入缓存的index
            if (inputIndex >= 0) {
                ByteBuffer inputByteBuf = mediaCodec.getInputBuffer(inputIndex);
                inputByteBuf.clear();
                inputByteBuf.put(data);//添加数据
                inputByteBuf.limit(data.length);//限制ByteBuffer的访问长度
                mediaCodec.queueInputBuffer(inputIndex, 0, data.length, 0, 0);//把输入缓存塞回去给MediaCodec
            }

            int outputIndex = mediaCodec.dequeueOutputBuffer(encodeBufferInfo, 0);//获取输出缓存的index
            while (outputIndex >= 0) {
                //获取缓存信息的长度
                int byteBufSize = encodeBufferInfo.size;
                //添加ADTS头部后的长度
                int bytePacketSize = byteBufSize + 7;
                //拿到输出Buffer
                ByteBuffer  outPutBuf = mediaCodec.getOutputBuffer(outputIndex);
                outPutBuf.position(encodeBufferInfo.offset);
                outPutBuf.limit(encodeBufferInfo.offset+encodeBufferInfo.size);

                byte[]  targetByte = new byte[bytePacketSize];
                //添加ADTS头部
                addADTStoPacket(targetByte, bytePacketSize);
            /*
            get（byte[] dst,int offset,int length）:ByteBuffer从position位置开始读，读取length个byte，并写入dst下
            标从offset到offset + length的区域
             */
                outPutBuf.get(targetByte,7,byteBufSize);

                outPutBuf.position(encodeBufferInfo.offset);

                try {
                    out.write(targetByte);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //释放
                mediaCodec.releaseOutputBuffer(outputIndex,false);
                outputIndex = mediaCodec.dequeueOutputBuffer(encodeBufferInfo, 0);
            }

        }
    }

    /**
     * 写入ADTS头部数据
     *
     * @param packet
     * @param packetLen
     */
    public static void addADTStoPacket(byte[] packet, int packetLen) {
        int profile = 2; // AAC LC
        int freqIdx = 4; // 44.1KHz
        int chanCfg = 2; // CPE

        //syncword，比如为1，即0xff
        packet[0] = (byte) 0xFF;
        packet[1] = (byte) 0xF9;
        packet[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (chanCfg >> 2));
        packet[3] = (byte) (((chanCfg & 3) << 6) + (packetLen >> 11));
        packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
        packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
        packet[6] = (byte) 0xFC;

        Log.d("czp1",packetLen+"");
        Log.d("czp2",packet[2]+"");
        Log.d("czp3",packet[3]+"");
        Log.d("czp4",packet[4]+"");
        Log.d("czp5",packet[5]+"");
    }
}
