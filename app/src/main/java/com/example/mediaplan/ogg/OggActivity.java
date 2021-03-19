package com.example.mediaplan.ogg;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mediaplan.R;
import com.example.mediaplan.opus.OpusLoader;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import static com.google.android.exoplayer2.C.CONTENT_TYPE_MUSIC;
import static com.google.android.exoplayer2.C.USAGE_MEDIA;

public class OggActivity extends AppCompatActivity {
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
        setContentView(R.layout.activity_ogg);

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

        filename = getExternalFilesDir("record").getPath()+ File.separator+"test.ogg";
        initRecord();
    }

    public void playRecord(){
        File f = new File(filename);
        if(!f.exists()){
            return;
        }

        Uri u = Uri.fromFile(new File(getExternalFilesDir("record").getPath()+File.separator+"test.ogg"));

        SimpleExoPlayer player = ExoPlayerFactory.newSimpleInstance(new DefaultRenderersFactory(this),new DefaultTrackSelector(),new DefaultLoadControl());
        final AudioAttributes attributes = new AudioAttributes.Builder()
                .setContentType(CONTENT_TYPE_MUSIC)
                .setUsage(USAGE_MEDIA)
                .build();

        player.setAudioAttributes(attributes);
        DataSource.Factory dataSourceFactory =
                new DefaultDataSourceFactory(
                        this, Util.getUserAgent(this, "uamp"), null);
        // Produces Extractor instances for parsing the media data.
        ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
        // The MediaSource represents the media to be played.
        ExtractorMediaSource.Factory extractorMediaFactory =
                new ExtractorMediaSource.Factory(dataSourceFactory);
        extractorMediaFactory.setExtractorsFactory(extractorsFactory);
        MediaSource mediaSource =
                extractorMediaFactory.createMediaSource(u);

        // Prepares media to play (happens on background thread) and triggers
        // {@code onPlayerStateChanged} callback when the stream is ready to play.

        // Prepares media to play (happens on background thread) and triggers
        // {@code onPlayerStateChanged} callback when the stream is ready to play.
        player.prepare(mediaSource);
        player.setPlayWhenReady(true);
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
        OggThread t= new OggThread();
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

    class OggThread extends Thread {
        long encoder;
        long oggos;
        public OggThread() {
            encoder= OpusLoader.createOpusEncoder(16000,1);
            oggos = OggLoader.init();
        }

        @Override
        public void run() {
            super.run();
            isReocrding = true;
            File file = new File(filename);
            try {
                FileOutputStream outputStream = new FileOutputStream(file);

                createHeader(oggos,outputStream);
                short[] buf = new short[frameSize];
                audioRecord.startRecording();
                int number = 2;
                long granulepos = 0;
                while (isReocrding) {
                    audioRecord.read(buf, 0, buf.length);
                    byte[] out = new byte[80];

                    int encodeSize = OpusLoader.encodeFrame(encoder,buf,0,out);
                    if (encodeSize > 0) {
                        byte[] opusencoded = new byte[encodeSize];
                        System.arraycopy(out,0,opusencoded,0,encodeSize);
                        granulepos += 640; //samples = ori_len/2*(OPUS_HZ/samplerate)
                        byte[] oggbytes = OggLoader.streamPacketin(oggos, opusencoded, number, granulepos);
                        number++;
                        outputStream.write(oggbytes);
                    }
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

    public void createHeader(long oggos,OutputStream outputStream) throws IOException{
        byte[] b = new byte[19];
        byte[] h = "OpusHead".getBytes();
        System.arraycopy(h,0,b,0,h.length);
        b[8] = 1;
        b[9] = (byte)(channelConfig & 0xff);
        b[10] = 0;
        b[11] = 0;
        b[12] = (byte)(sampleRate & 0xff);
        b[13] = (byte)(sampleRate>>8 &0xff);
        b[14] = (byte)(sampleRate>>16 &0xff);
        b[15] = (byte)(sampleRate>>24 &0xff);
        b[16] = 0;
        b[17] = 0;
        b[18] = 0;
        //Log.d("czp", Arrays.toString(b));
        byte[] bb = OggLoader.streamPacketin(oggos,b,0,0);
        outputStream.write(bb);
        Log.d("czp", Arrays.toString(bb));

        String c = "OpusTags";
        String d="opus rtp packet dump";

        byte[] e = new byte[c.length()+4+d.length()+4];

        System.arraycopy(c.getBytes(),0,e,0,c.length());
        e[8] = (byte)(d.length()&0xff);
        e[9] = (byte)(d.length()>>8&0xff);
        e[10] = (byte)(d.length()>>16&0xff);
        e[11] = (byte)(d.length()>>24&0xff);

        System.arraycopy(d.getBytes(),0,e,12,d.length());

        e[12+d.length()] = (byte)0;
        e[13+d.length()] = 0;
        e[14+d.length()] = 0;
        e[15+d.length()] = 0;
        Log.d("czp", Arrays.toString(e));
        byte[] dd = OggLoader.streamPacketin(oggos,e,1,0);
        outputStream.write(dd);
        Log.d("czp", Arrays.toString(dd));
    };
}
