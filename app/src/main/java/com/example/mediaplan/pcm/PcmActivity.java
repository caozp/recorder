package com.example.mediaplan.pcm;

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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class PcmActivity extends AppCompatActivity {

    public static final int AUDIO_RATE = 44100;//采样率
    public static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_STEREO;
    public static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    String pcmfilename;// pcm文件名

    AudioRecord audioRecord;
    int bufferSize;

    Button btnStart;
    Button btnStop;
    Button btnPlay;

    boolean isReocrding = false;


    AudioAttributes attributes;
    AudioTrack audioTrack;
    AudioFormat format;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actvitiy_pcm);

        btnPlay = findViewById(R.id.play);
        btnStop =findViewById(R.id.stop);
        btnStart = findViewById(R.id.record);

        pcmfilename = getExternalFilesDir("record").getPath()+ File.separator+"test.pcm";


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

        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playRecord();
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
        PcmThread t = new PcmThread();
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

    public void playRecord(){
        File file = new File(pcmfilename);
        if (file.exists()){
            PlayPcmThread t = new PlayPcmThread();
            t.start();
        } else{
            Toast.makeText(this,"文件不存在",Toast.LENGTH_SHORT).show();
        }
    }

    class PcmThread extends Thread{

        @Override
        public void run() {
            super.run();
            audioRecord.startRecording();
            isReocrding = true;
            File file = new File(pcmfilename);
            try {
                FileOutputStream outputStream = new FileOutputStream(file);
                byte[] buf = new byte[bufferSize];

                while (isReocrding) {
                    audioRecord.read(buf, 0, buf.length);
                    outputStream.write(buf);
                }

                outputStream.flush();
                outputStream.close();
            } catch (FileNotFoundException E){
                Log.d("exception",E.getMessage());
            } catch (IOException E){
                Log.d("exception",E.getMessage());
            }
        }
    }

    class PlayPcmThread extends Thread{

        public PlayPcmThread() {
            attributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();
            format = new AudioFormat.Builder()
                    .setSampleRate(AUDIO_RATE)
                    .setChannelMask(CHANNEL_CONFIG)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build();


            audioTrack = new AudioTrack(
                    attributes,
                    format,
                    bufferSize,
                    AudioTrack.MODE_STREAM,
                    AudioManager.AUDIO_SESSION_ID_GENERATE   //音频识别ID
            );
        }

        @Override
        public void run() {
            super.run();
            try {
                File f = new File(pcmfilename);
                FileInputStream fis = new FileInputStream(f);
                audioTrack.play();

                byte[] buf = new byte[bufferSize];
                int len =0;

                while((len = fis.read(buf)) != -1){
                    audioTrack.write(buf,0,len);
                }
                audioTrack.stop();
                audioTrack.release();

                fis.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e){
                e.printStackTrace();
            }

        }
    }
}
