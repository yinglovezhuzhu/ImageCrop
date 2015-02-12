package com.opensource.imagecrop.demo;

import java.io.File;
import java.util.Set;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.opensource.imagecrop.CropConfig;
import com.opensource.imagecrop.CropImageActivity;


public class MainActivity extends Activity implements View.OnClickListener{

    private static final String TAG = "MainActivity";

    private static final int REQUEST_CODE_PICK_IMAGE = 0x100;
    private static final int REQUEST_CODE_CROP_IMAGE = 0x101;
    
    private ImageView mIvImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mIvImage = (ImageView) findViewById(R.id.iv_image);
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
//                	Bundle extras = data.getExtras();
//                	if(null != extras && extras.containsKey("data")) {
//                		Bitmap bm = extras.getParcelable("data");
//                		mIvImage.setImageBitmap(bm);
//                	}
                	if(null != file) {
                		Bitmap bm = BitmapFactory.decodeFile(file.getPath());
                		mIvImage.setImageBitmap(bm);
                	}
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
    

    File file = null;

    private void gotoCrop(Uri data) {
        Intent intent = new Intent(this, CropImageActivity.class);
        intent.setData(data);
        Bundle extras = new Bundle();
//        extras.putString(CropConfig.EXTRA_CIRCLE_CROP, "Circle");
        extras.putInt(CropConfig.EXTRA_ASPECT_X, 1);
        extras.putInt(CropConfig.EXTRA_ASPECT_Y, 1);
        extras.putInt(CropConfig.EXTRA_OUTPUT_X, 400);
        extras.putInt(CropConfig.EXTRA_OUTPUT_Y, 400);
        extras.putBoolean(CropConfig.EXTRA_SCALE, true);
        extras.putBoolean(CropConfig.EXTRA_SCALE_UP_IF_NEEDED, true);
//        extras.putBoolean(CropConfig.EXTRA_RETURN_DATA, true);
        
        file = new File(Environment.getExternalStorageDirectory(), System.currentTimeMillis() + ".jpg");
        extras.putParcelable(CropConfig.EXTRA_OUTPUT, Uri.fromFile(file));
        intent.putExtras(extras);
//        Intent intent = new Intent("com.android.camera.action.CROP");
//		intent.setDataAndType(data, "image/*");
//		// 下面这个crop=true是设置在开启的Intent中设置显示的VIEW可裁剪
//		intent.putExtra("crop", "true");
//		// aspectX aspectY 是宽高的比例
//		intent.putExtra("aspectX", 1);
//		intent.putExtra("aspectY", 1);
//		// outputX outputY 是裁剪图片宽高
//		intent.putExtra("outputX", 400);
//		intent.putExtra("outputY", 400);
//		intent.putExtra("return-data", true);
		
        startActivityForResult(intent, REQUEST_CODE_CROP_IMAGE);
    }
}
