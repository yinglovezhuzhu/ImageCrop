package com.opensource.imagecrop.demo;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.opensource.imagecrop.CropConfig;
import com.opensource.imagecrop.CropImageActivity;


public class MainActivity extends Activity implements View.OnClickListener{

    private static final String TAG = "MainActivity";

    private static final int REQUEST_CODE_PICK_IMAGE = 0x100;
    private static final int REQUEST_CODE_CROP_IMAGE = 0x101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.btn_crop).setOnClickListener(this);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_PICK_IMAGE:
                if(resultCode == RESULT_OK && null != data) {
                    Uri uri = data.getData();
                    if(uri == null) {
                        return;
                    }
                    Log.i(TAG, "Pick image uri: " + uri.getPath());
                    gotoCrop(uri);
                }
                break;
            case REQUEST_CODE_CROP_IMAGE:
                if(resultCode == RESULT_OK && null != data) {

                }
                break;
            default:
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_crop:
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE);
                break;
            default:
                break;
        }
    }

    private void gotoCrop(Uri data) {
        Intent intent = new Intent(this, CropImageActivity.class);
        intent.setData(data);
        Bundle extras = new Bundle();
//        extras.putString(CropConfig.EXTRA_CIRCLE_CROP, "Circle");
        extras.putInt(CropConfig.EXTRA_ASPECT_X, 1);
        extras.putInt(CropConfig.EXTRA_ASPECT_Y, 1);
        intent.putExtras(extras);
        startActivity(intent);
    }
}
