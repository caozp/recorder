package com.example.mediaplan.wav;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mediaplan.R;
import com.example.mediaplan.pcm.PcmActivity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

public class WavActivity extends AppCompatActivity {
    public static final int AUDIO_RATE = 44100;//采样率
    public static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_STEREO;
    public static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    String filename;// wav文件名

    AudioRecord audioRecord;
    int bufferSize;

    Button btnStart;
    Button btnStop;

    boolean isReocrding = false;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wav);

        btnStop =findViewById(R.id.stop);
        btnStart = findViewById(R.id.record);

        filename = getExternalFilesDir("record").getPath()+ File.separator+"test.wav";


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
        WavThread t = new WavThread();
        t.start();
    }

    public void stopRecord(){
        isReocrding = false;

        audioRecord.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        audioRecord.release();
    }

    class WavThread extends Thread{

        @Override
        public void run() {
            super.run();
            audioRecord.startRecording();
            isReocrding = true;
            File file = new File(filename);
            try {
                FileOutputStream outputStream = new FileOutputStream(file);

                byte[] header = new byte[44];
                outputStream.write(header);

                byte[] buf = new byte[bufferSize];
                int len = 0;
                while (isReocrding) {
                    int n = audioRecord.read(buf, 0, buf.length);
                    outputStream.write(buf);
                    len += n;
                }

                outputStream.flush();

                outputStream.close();

                RandomAccessFile raf = new RandomAccessFile(file,"rw");
                raf.seek(0);
                byte[] wavHeader = generateWavFileHeader(len,AUDIO_RATE,audioRecord.getChannelCount());
                raf.write(wavHeader);
                raf.close();

            } catch (FileNotFoundException E){
                Log.d("exception",E.getMessage());
            } catch (IOException E){
                Log.d("exception",E.getMessage());
            }
        }
    }


    private static byte[] generateWavFileHeader(long pcmAudioByteCount, long longSampleRate, int channels) {
        long totalDataLen = pcmAudioByteCount + 36; // 不包含前8个字节的WAV文件总长度
        long byteRate = longSampleRate * 2 * channels;
        byte[] header = new byte[44];
        header[0] = 'R'; // RIFF
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';

        header[4] = (byte) (totalDataLen & 0xff);//数据大小
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);

        header[8] = 'W';//WAVE
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        //FMT Chunk
        header[12] = 'f'; // 'fmt '
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';//过渡字节
        //数据大小
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        //编码方式 1为PCM编码格式
        header[20] = 1; // format = 1
        header[21] = 0;
        //通道数
        header[22] = (byte) channels;
        header[23] = 0;
        //采样率，每个通道的播放速度
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        //音频数据传送速率,采样率*通道数*采样深度/8
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        // 确定系统一次要处理多少个这样字节的数据，确定缓冲区，通道数*采样位数
        header[32] = (byte) (2 * channels);
        header[33] = 0;
        //每个样本的数据位数
        header[34] = 16;
        header[35] = 0;
        //Data chunk
        header[36] = 'd';//data
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (pcmAudioByteCount & 0xff);
        header[41] = (byte) ((pcmAudioByteCount >> 8) & 0xff);
        header[42] = (byte) ((pcmAudioByteCount >> 16) & 0xff);
        header[43] = (byte) ((pcmAudioByteCount >> 24) & 0xff);
        return header;
    }
}
