package com.example.mediaplan.opus;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mediaplan.R;
import com.example.mediaplan.pcm.PcmActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class OpusActivity extends AppCompatActivity {

    final int sampleRate = 16000;
    final int channelConfig = 1;
    final int frameSize = (int) (sampleRate * 0.02f);
    int bufferSize;

    Button btnStart;
    Button btnStop;
    Button btnPlay;

    boolean isReocrding = false;

    AudioRecord audioRecord;
    String filename;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_opus);

        btnPlay = findViewById(R.id.play);
        btnStop =findViewById(R.id.stop);
        btnStart = findViewById(R.id.record);

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

        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isReocrding) {
                    playRecord();
                }
            }
        });

        filename = getExternalFilesDir("record").getPath()+File.separator+"test.opus";
        initRecord();
    }

    public void playRecord(){
        File f = new File(filename);
        if(!f.exists()){
            return;
        }

        PlayOpusThread thread = new PlayOpusThread();
        thread.start();
    }

    private void initRecord(){
        bufferSize = AudioRecord.getMinBufferSize(sampleRate,channelConfig, AudioFormat.ENCODING_PCM_16BIT);
        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
        );
    }

    public void startRecord(){
        OpusThread t = new OpusThread();
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

    class OpusThread extends Thread {
        long encoder;


        public OpusThread() {
            encoder= OpusLoader.createOpusEncoder(16000,1);
        }

        @Override
        public void run() {
            super.run();
            isReocrding = true;
            File file = new File(filename);
            try {
                FileOutputStream outputStream = new FileOutputStream(file);
                short[] buf = new short[frameSize];
                audioRecord.startRecording();
                while (isReocrding) {
                    audioRecord.read(buf, 0, buf.length);
                    byte[] out = new byte[80];
                    OpusLoader.encodeFrame(encoder,buf,0,out);

                    outputStream.write(out);
                }
                outputStream.flush();
                outputStream.close();
            } catch (FileNotFoundException E){
                Log.d("exception",E.getMessage());
            } catch (IOException E){
                Log.d("exception",E.getMessage());
            }

            OpusLoader.destroyOpusEncoder(encoder);
        }
    }


    class PlayOpusThread extends Thread{

        AudioAttributes attributes;
        AudioTrack audioTrack;
        AudioFormat format;
        long decoder;
        public PlayOpusThread(){

            attributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();
            format = new AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setChannelMask(channelConfig)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build();


            audioTrack = new AudioTrack(
                    attributes,
                    format,
                    bufferSize,
                    AudioTrack.MODE_STREAM,
                    AudioManager.AUDIO_SESSION_ID_GENERATE   //音频识别ID
            );

            decoder = OpusLoader.createOpusDecoder(sampleRate,channelConfig);
        }

        @Override
        public void run() {
            super.run();

            try {
                File f = new File(filename);
                FileInputStream fis = new FileInputStream(f);
                audioTrack.play();

                byte[] buf = new byte[80];
                int len =0;
                short[] out = new short[320];
                while((len = fis.read(buf)) != -1){
                    OpusLoader.decodeFrame(decoder,buf,out);
                    audioTrack.write(out,0,out.length);
                }
                audioTrack.stop();
                audioTrack.release();

                fis.close();
                OpusLoader.destroyOpusDecoder(decoder);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e){
                e.printStackTrace();
            }

        }
    }

    public static short[] bytetoShortArray(byte[] bytes) {
        short[] s = new short[bytes.length/2];
        ByteBuffer.wrap(bytes).order(ByteOrder.nativeOrder()).asShortBuffer().get(s);
        return s;
    }
}
