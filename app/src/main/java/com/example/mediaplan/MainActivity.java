package com.example.mediaplan;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.mediaplan.aac.AacActivity;
import com.example.mediaplan.ogg.OggActivity;
import com.example.mediaplan.ogg.OggLoader;
import com.example.mediaplan.opus.OpusActivity;
import com.example.mediaplan.opus.OpusLoader;
import com.example.mediaplan.pcm.PcmActivity;
import com.example.mediaplan.wav.WavActivity;

import java.util.LinkedList;
import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, EasyPermissions.PermissionCallbacks {


    public static final int PERMISSION_CODE = 1000;
    Button btn2;
    Button btn3;
    Button btn4;
    Button btn5;
    Button btn6;
    String[] perms = {Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        methodRequiresTwoPermission();
        btn2 = findViewById(R.id.btn2);
        btn3 = findViewById(R.id.btn3);
        btn4 = findViewById(R.id.btn4);
        btn5 = findViewById(R.id.btn5);
        btn6 = findViewById(R.id.btn6);
        btn2.setOnClickListener(this);
        btn3.setOnClickListener(this);
        btn4.setOnClickListener(this);
        btn5.setOnClickListener(this);
        btn6.setOnClickListener(this);
        
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {

    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        finish();
    }

    @Override
    public void onClick(View v) {
        Intent i;
        switch (v.getId()){
            case R.id.btn2:
                i = new Intent(this, PcmActivity.class);
                startActivity(i);
                break;
            case R.id.btn3:
                i = new Intent(this, WavActivity.class);
                startActivity(i);
                break;
            case R.id.btn4:
                i = new Intent(this, AacActivity.class);
                startActivity(i);
                break;

            case R.id.btn5:
                i = new Intent(this, OpusActivity.class);
                startActivity(i);
                break;

            case R.id.btn6:
                i = new Intent(this, OggActivity.class);
                startActivity(i);
                break;
        }
    }

    @AfterPermissionGranted(PERMISSION_CODE)
    private void methodRequiresTwoPermission() {

        if (EasyPermissions.hasPermissions(this, perms)) {
            Toast.makeText(MainActivity.this,"已有权限",Toast.LENGTH_SHORT).show();

        } else {
            // Do not have permissions, request them now
            EasyPermissions.requestPermissions(this, "权限",
                    PERMISSION_CODE, perms);
        }
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    public int count(View v){
        if (v instanceof ViewGroup) {
            int n = ((ViewGroup) v).getChildCount();
            int total = 0;
            for (int i = 0; i < n; i++) {
                total = count(((ViewGroup) v).getChildAt(i))+1;
            }

            return total;
        } else {
            return 1;
        }
    }
}