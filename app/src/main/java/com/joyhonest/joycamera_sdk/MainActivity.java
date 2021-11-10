package com.joyhonest.joycamera_sdk;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.SeekBar;

import com.joyhonest.joycamera.sdk.*;
import com.joyhonest.joycamera_sdk.databinding.ActivityMainBinding;

import org.simple.eventbus.*;

import java.io.File;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    // Used to load the 'joycamera' library on application startup.
    private static String TAG = "MainAcrivity";
    private ActivityMainBinding binding;
    int nMode = 0;
    boolean bFlip = false;
    boolean bMirror = false;
    boolean bSqu = false;
    boolean bCir = false;
    int   nRota = 0;
    boolean  b3Dnoiser = false;
    boolean  bRotaFilter = false;

    private PermissionAsker mAsker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Window window =getWindow();
        window.setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        binding.Mode.setOnClickListener(this);
        binding.ReadList.setOnClickListener(this);

        binding.SQU.setOnClickListener(this);
        binding.Start.setOnClickListener(this);
        binding.Stop.setOnClickListener(this);
        binding.Flip.setOnClickListener(this);
        binding.Mirror.setOnClickListener(this);
        binding.Rota.setOnClickListener(this);
        binding.Cir.setOnClickListener(this);
        binding.Denoiser.setOnClickListener(this);
        binding.RotaFilter.setOnClickListener(this);
        binding.seekBar.setProgress(0);
        binding.seekBar.setMax(3900);
        binding.seekBar.setBackgroundColor(0xFFFFFFFF);
        binding.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int nProgress = binding.seekBar.getProgress();
                //float da = ((float)nProgress)/binding.seekBar.getMax();
//                    da *=2;
//                    da-=1;
//                    wifiCamera.naSetBrightness(da);
                wifiCamera.naSetZoomFocus(nProgress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        wifiCamera.naSetFlip(bFlip);
        wifiCamera.naSetMirror(bMirror);


        mAsker = new PermissionAsker(10, new Runnable() {
            @Override
            public void run() {
                Log.e(TAG,"Can Record Audio");
            }
        }, new Runnable() {
            @Override
            public void run() {
                Log.e(TAG,"Can't Record Audio");
            }
        });


        EventBus.getDefault().register(this);


    }

    @Override
    protected void onResume() {
        super.onResume();
        //mAsker.askPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);//,Manifest.permission.RECORD_AUDIO);
        mAsker.askPermission(this, Manifest.permission.RECORD_AUDIO);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        mAsker.onRequestPermissionsResult(grantResults);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onClick(View v) {

        if(binding.ReadList == v)
        {
            File file = getExternalFilesDir("JoyCamera_Test");
            String sLocalPath = file.getAbsolutePath();
            sLocalPath = sLocalPath+"/abc123.mp4";
            //sLocalPath = sLocalPath+"/abc.jpg";




            wifiCamera.naStartRecord(sLocalPath,wifiCamera.TYPE_ONLY_PHONE,wifiCamera.TYPE_DEST_SNADBOX,true);

/*            wifiCamera.naSnapPhoto(sLocalPath,wifiCamera.TYPE_ONLY_PHONE,wifiCamera.TYPE_DEST_SNADBOX);
            if(nMode ==1)
            {
                //wifiCamera.naGetSdFliesList(1,0,20);
                File file = getExternalFilesDir("JoyCamera_Test");
                String sLocalPath = file.getAbsolutePath();
                sLocalPath = sLocalPath+"abc.mov";
                //wifiCamera.naStartDonwPlay("MOVI0001.mov",2084352);
                wifiCamera.naStartDonwLoad("MOVI0001.mov",2084352,sLocalPath);
            }

 */
            //wifiCamera.naSetRecordWH(640/2,360/2);
            wifiCamera.naStartAutoFocus(true);
        }
        if( binding.Mode == v)
        {
            wifiCamera.naSetRecordWH(640/2,480/2);
//            nMode = nMode!=0?0:1;
//            wifiCamera.naSetDeviceMode(nMode);
//            binding.Mode.setText("Mode"+nMode);
        }
        if(v == binding.Start)
        {
            wifiCamera.naInit("");
            nMode = 0;
            binding.Mode.setText("Mode"+nMode);
        }
        if(v== binding.Stop)
        {
            wifiCamera.naStop();
        }
        if(v==binding.Flip)
        {
            bFlip = !bFlip;
            wifiCamera.naSetFlip(bFlip);
        }
        if(v==binding.Mirror)
        {
            bMirror = !bMirror;
            wifiCamera.naSetMirror(bMirror);
            //wifiCamera.naSetCircul(bMirror);
        }
        if(v == binding.Rota)
        {
            if(nRota==0)
            {
                nRota = 90;
            }
            else if(nRota == 90)
            {
                nRota = 180;
            }
            else if(nRota == 180)
            {
                nRota = 270;
            }
            else if(nRota==270)
            {
                nRota = 0;
            }
            wifiCamera.naSetCameraDataRota(nRota);
            binding.Rota.setText("Rota "+nRota);
//            bSqu = !bSqu;
//            binding.Rota.setText("abc81");
//            wifiCamera.naSetsquare(bSqu);
        }
        if(binding.SQU == v)
        {
            bSqu = !bSqu;
            wifiCamera.naSetsquare(bSqu);
        }
        if(v == binding.Cir)
        {
            bCir = !bCir;
            wifiCamera.naSetCircul(bCir);
        }
        if(v == binding.Denoiser)
        {
            b3Dnoiser = !b3Dnoiser;
            wifiCamera.naSet3DDenoiser(b3Dnoiser);
        }
        if(v == binding.RotaFilter)
        {
            bRotaFilter = !bRotaFilter;
            //wifiCamera.naSetEnableRotate(bRotaFilter);
            wifiCamera.naEnableSensor(bRotaFilter);
//            new Handler().postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    wifiCamera.naSetFilterRotate(45.0f);
//                }
//            },300);

        }


    }

    @Subscriber(tag = "onCameraStatusChange")
    private void onCameraStatusChange(Integer nStatus_)
    {
        int nStatus = nStatus_;
        if((nStatus & 0x01) !=0)
        {
            Log.e(TAG,"Camera opened");
        }
        else
        {
            Log.e(TAG,"Camera Closeed");
            binding.DispImageView.setImageBitmap(null);
        }
    }

    @Subscriber(tag = "onGetFrame")
    private void onGetFrame(Bitmap bmp)
    {
        binding.DispImageView.setImageBitmap(bmp);

    }

    @Subscriber(tag = "onGetSdFliesList")
    private  void  onGetSdFliesList(GP4225_Device.GetFiles fileslist)
    {
        List<GP4225_Device.JoyFile> files = fileslist.files;
        for(GP4225_Device.JoyFile file :files)
        {
            Log.e(TAG,"file name = "+file.sFileName +" len = "+file.nLength);
        }
    }

    @Subscriber(tag = "DownloadFile")
    private void DownloadFile(jh_dowload_callback dowload)
    {
        String ss = "";
        if (dowload.nError != 0) {
            ss =    dowload.sFileName;
        } else {
            ss = dowload.sFileName + "  DownLod " + dowload.nPercentage/10.0f + "‰";
        }
        Log.e(TAG," "+ss);
    }

}