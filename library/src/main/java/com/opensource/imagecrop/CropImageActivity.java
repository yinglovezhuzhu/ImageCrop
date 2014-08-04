/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.opensource.imagecrop;

import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.opensource.imagecrop.utils.FileUtil;
import com.opensource.imagecrop.widget.CropImageView;
import com.opensource.imagecrop.widget.HighlightView;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

/**
 * The activity can crop specific region of interest from an image.
 */
public class CropImageActivity extends MonitoredActivity {

    private static final String TAG = "CropImageActivity";

	public static final int CROP_MSG = 10;	
    public static final int CROP_MSG_INTERNAL = 100;    
    
    // These are various options can be specified in the intent.
                                    // panda.
    private final Handler mHandler = new Handler();

    private boolean mCircleCrop = false;
    // These options specifiy the output image size and whether we should
    // scale the output to fit it (or just crop it).
    private int mAspectX;
    private int mAspectY; // CR: two definitions per line == sad
    private int mOutputX;
    private int mOutputY;
    private boolean mScale;
    private boolean mScaleUp = true;
    private Uri mSaveUri = null;
    private Bitmap.CompressFormat mOutputFormat = Bitmap.CompressFormat.JPEG; // only
                                                                              // used
                                                                              // with
                                                                              // mSaveUri

    boolean mSaving; // Whether the "save" button is already clicked.

    private CropImageView mImageView;
    private ContentResolver mContentResolver;

    private Bitmap mBitmap;
    private Uri mInputUri;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mContentResolver = getContentResolver();
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_cropimage);

        mImageView = (CropImageView) findViewById(R.id.image);

        // CR: remove TODO's.
        // TODO: we may need to show this indicator for the main gallery
        // application
        // MenuHelper.showStorageToast(this);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();

        if (extras != null) {
            if (extras.getString(CropConfig.EXTRA_CIRCLE_CROP) != null) {
                mCircleCrop = true;
                mAspectX = 1;
                mAspectY = 1;
            }
            mSaveUri = extras.getParcelable(CropConfig.EXTRA_OUTPUT);
            if (mSaveUri != null) {
                String outputFormatString = extras.getString(CropConfig.EXTRA_OUTPUT_FORMAT);
                if (outputFormatString != null) {
                    mOutputFormat = Bitmap.CompressFormat.valueOf(outputFormatString);
                }
            }
            mBitmap = extras.getParcelable(CropConfig.EXTRA_DATA);
            mAspectX = extras.getInt(CropConfig.EXTRA_ASPECT_X);
            mAspectY = extras.getInt(CropConfig.EXTRA_ASPECT_Y);
            mOutputX = extras.getInt(CropConfig.EXTRA_OUTPUT_X);
            mOutputY = extras.getInt(CropConfig.EXTRA_OUTPUT_Y);
            mScale = extras.getBoolean(CropConfig.EXTRA_SCALE, true);
            mScaleUp = extras.getBoolean(CropConfig.EXTRA_SCALE_UP_IF_NEEDED, true);
        }

        if (mBitmap == null) {
            // Create a MediaItem representing the URI.
            mInputUri = intent.getData();

            File imageFile = FileUtil.parseUriToFile(this, mInputUri);


            if(null != imageFile) {
                String imagePath = imageFile.getPath();
                Log.i(TAG, "Parse Uri to file, file path : " + imagePath);
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                options.inJustDecodeBounds = false;
                options.inSampleSize = 4;
                mBitmap = BitmapFactory.decodeFile(imagePath, options);

            }
        }

        if (mBitmap == null) {
            Log.e(TAG, "Cannot load bitmap, exiting.");
            finish();
            return;
        }

        // Make UI fullscreen.
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        findViewById(R.id.discard).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });

        findViewById(R.id.save).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onSaveClicked();
            }
        });


        mImageView.setImageBitmapResetBase(mBitmap, true);

        makeCropView();

//        mImageView.invalidate();
//        if (mImageView.mHighlightViews.size() == 1) {
//            mImageView.mCropView = mImageView.mHighlightViews.get(0);
//            mImageView.mCropView.setFocus(true);
//        }
    }

    /**
     * Save button clicked
     */
    private void onSaveClicked() {
        if (mSaving)
            return;

        if (mImageView.getCropView() == null) {
            return;
        }

        mSaving = true;
        mImageView.setSaving(mSaving);

        Rect r = mImageView.getCropRect();

        int width = r.width(); // CR: final == happy panda!
        int height = r.height();

        // If we are circle cropping, we want alpha channel, which is the
        // third param here.
        Bitmap croppedImage = Bitmap.createBitmap(width, height, mCircleCrop ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565);
        {
            Canvas canvas = new Canvas(croppedImage);
            Rect dstRect = new Rect(0, 0, width, height);
            canvas.drawBitmap(mBitmap, r, dstRect, null);
        }

        if (mCircleCrop) {
            // OK, so what's all this about?
            // Bitmaps are inherently rectangular but we want to return
            // something that's basically a circle. So we fill in the
            // area around the circle with alpha. Note the all important
            // PortDuff.Mode.CLEARes.
            Canvas c = new Canvas(croppedImage);
            Path p = new Path();
            p.addCircle(width / 2F, height / 2F, width / 2F, Path.Direction.CW);
            c.clipPath(p, Region.Op.DIFFERENCE);
            c.drawColor(0x00000000, PorterDuff.Mode.CLEAR);
        }

        // If the output is required to a specific size then scale or fill.
        if (mOutputX != 0 && mOutputY != 0) {
            if (mScale) {
                // Scale the image to the required dimensions.
                Bitmap old = croppedImage;
                croppedImage = Util.transform(new Matrix(), croppedImage, mOutputX, mOutputY, mScaleUp);
                if (old != croppedImage) {
                    old.recycle();
                }
            } else {

                /*
                 * Don't scale the image crop it to the size requested. Create
                 * an new image with the cropped image in the center and the
                 * extra space filled.
                 */

                // Don't scale the image but instead fill it so it's the
                // required dimension
                Bitmap b = Bitmap.createBitmap(mOutputX, mOutputY, Bitmap.Config.RGB_565);
                Canvas canvas = new Canvas(b);

                Rect srcRect = mImageView.getCropRect();
                Rect dstRect = new Rect(0, 0, mOutputX, mOutputY);

                int dx = (srcRect.width() - dstRect.width()) / 2;
                int dy = (srcRect.height() - dstRect.height()) / 2;

                // If the srcRect is too big, use the center part of it.
                srcRect.inset(Math.max(0, dx), Math.max(0, dy));

                // If the dstRect is too big, use the center part of it.
                dstRect.inset(Math.max(0, -dx), Math.max(0, -dy));

                // Draw the cropped bitmap in the center.
                canvas.drawBitmap(mBitmap, srcRect, dstRect, null);

                // Set the cropped bitmap as the new bitmap.
                croppedImage.recycle();
                croppedImage = b;
            }
        }

        // Return the cropped image directly or save it to the specified URI.
        Bundle myExtras = getIntent().getExtras();
        if (myExtras != null && (myExtras.getParcelable("data") != null || myExtras.getBoolean("return-data"))) {
            Bundle extras = new Bundle();
            extras.putParcelable("data", croppedImage);
            setResult(RESULT_OK, (new Intent()).setAction("inline-data").putExtras(extras));
            finish();
        } else {
            final Bitmap b = croppedImage;
            final Runnable save = new Runnable() {
                public void run() {
                    saveOutput(b);
                }
            };
            Util.startBackgroundJob(this, null, getResources().getString(R.string.saving_image), save, mHandler);
        }
    }

    /**
     * Save the cropped image to file<br/>
     * <br/><p/>If the output file has been set, it won't insert the image message inout ContentProvider<br/>
     * @param croppedImage
     */
    private void saveOutput(Bitmap croppedImage) {
        if (mSaveUri != null) {
            OutputStream outputStream = null;
            try {
                outputStream = mContentResolver.openOutputStream(mSaveUri);
                if (outputStream != null) {
                    croppedImage.compress(mOutputFormat, 75, outputStream);
                }
                // TODO ExifInterface write
            } catch (IOException ex) {
                Log.e(TAG, "Cannot open file: " + mSaveUri, ex);
            } finally {
                Util.closeSilently(outputStream);
            }
            Bundle extras = new Bundle();
            setResult(RESULT_OK, new Intent(mSaveUri.toString()).putExtras(extras));
        } else {
            Bundle extras = new Bundle();
            extras.putString(CropConfig.EXTRA_RECT, mImageView.getCropRect().toString());
            File oldFile = FileUtil.parseUriToFile(this, mInputUri);
            File directory = new File(oldFile.getParent());
            int x = 0;
            String fileName = oldFile.getName();
            fileName = fileName.substring(0, fileName.lastIndexOf("."));

            // Try file-1.jpg, file-2.jpg, ... until we find a filename
            // which
            // does not exist yet.
            while (true) {
                x += 1;
                String candidate = directory.toString() + "/" + fileName + "-" + x + ".jpg";
                boolean exists = (new File(candidate)).exists();
                if (!exists) { // CR: inline the expression for exists
                               // here--it's clear enough.
                    break;
                }
            }

            String title = fileName + "-" + x;
            String finalFileName = title + ".jpg";
            int[] degree = new int[1];
            Double latitude = null;
            Double longitude = null;
            Uri newUri = FileUtil.addImage(mContentResolver, title,
                    System.currentTimeMillis() / 1000, System.currentTimeMillis(), latitude,
                    longitude, directory.toString(), finalFileName,
                    croppedImage, null, degree);
            if (newUri != null) {
                setResult(RESULT_OK, new Intent().setAction(newUri.toString()).putExtras(extras));
            } else {
                setResult(RESULT_OK, new Intent().setAction(null));
            }
        }
        croppedImage.recycle();
        finish();
    }

    /**
     * Create a HightlightView to show how to crop the image.
     */
    private void makeCropView() {
        HighlightView hv = new HighlightView(mImageView);

        int width = mBitmap.getWidth();
        int height = mBitmap.getHeight();

        Rect imageRect = new Rect(0, 0, width, height);

        // CR: sentences!
        // make the default size about 4/5 of the width or height
        int cropWidth = Math.min(width, height) * 4 / 5;
        int cropHeight = cropWidth;

        if (mAspectX != 0 && mAspectY != 0) {
            if (mAspectX > mAspectY) {
                cropHeight = cropWidth * mAspectY / mAspectX;
            } else {
                cropWidth = cropHeight * mAspectX / mAspectY;
            }
        }

        int x = (width - cropWidth) / 2;
        int y = (height - cropHeight) / 2;

        RectF cropRect = new RectF(x, y, x + cropWidth, y + cropHeight);
        hv.setup(mImageView.getImageMatrix(), imageRect, cropRect, mCircleCrop, mAspectX != 0 && mAspectY != 0);
        hv.setFocus(true);
        mImageView.setCropView(hv);
    }

//    static private final HashMap<Context, MediaScannerConnection> mConnectionMap = new HashMap<Context, MediaScannerConnection>();
//
//    static public void launchCropperOrFinish(final Context context, final MediaItem item) {
//        final Bundle myExtras = ((Activity) context).getIntent().getExtras();
//        String cropValue = myExtras != null ? myExtras.getString("crop") : null;
//        final String contentUri = item.mInputUri;
//        if (contentUri == null)
//            return;
//        if (cropValue != null) {
//            Bundle newExtras = new Bundle();
//            if (cropValue.equals("circle")) {
//                newExtras.putString("circleCrop", "true");
//            }
//            Intent cropIntent = new Intent();
//            cropIntent.setData(Uri.parse(contentUri));
//            cropIntent.setClass(context, CropImageActivity.class);
//            cropIntent.putExtras(newExtras);
//            // Pass through any extras that were passed in.
//            cropIntent.putExtras(myExtras);
//            ((Activity) context).startActivityForResult(cropIntent, CropImageActivity.CROP_MSG);
//        } else {
//            if (contentUri.startsWith("http://")) {
//                // This is a http uri, we must save it locally first and
//                // generate a content uri from it.
//                final ProgressDialog dialog = ProgressDialog.show(context, context.getResources().getString(R.string.initializing),
//                        context.getResources().getString(R.string.running_face_detection), true, false);
//                if (contentUri != null) {
//                    MediaScannerConnection.MediaScannerConnectionClient client = new MediaScannerConnection.MediaScannerConnectionClient() {
//                        public void onMediaScannerConnected() {
//                            MediaScannerConnection connection = mConnectionMap.get(context);
//                            if (connection != null) {
////    							try {
////    								final String downloadDirectoryPath = LocalDataSource.DOWNLOAD_BUCKET_NAME;
////    								File downloadDirectory = new File(downloadDirectoryPath);
////    								downloadDirectory.mkdirs();
////    								final String path = UriTexture.writeHttpDataInDirectory(context, contentUri,
////    										downloadDirectoryPath);
////    								if (path != null) {
////    									connection.scanFile(path, item.mMimeType);
////    								} else {
////    									shutdown("");
////    								}
////    							} catch (Exception e) {
////    								shutdown("");
////    							}
//                            }
//                        }

//                        public void onScanCompleted(String path, Uri uri) {
//                            shutdown(uri.toString());
//                        }
//
//                        public void shutdown(String uri) {
//                            dialog.dismiss();
//                            performReturn(context, myExtras, uri.toString());
//                            MediaScannerConnection connection = mConnectionMap.get(context);
//                            if (connection != null) {
//                                connection.disconnect();
//                                mConnectionMap.put(context, null);
//                            }
//                        }
//                    };
//                    MediaScannerConnection connection = new MediaScannerConnection(context, client);
//                    mConnectionMap.put(context, connection);
//                    connection.connect();
//                }
//            } else {
//                performReturn(context, myExtras, contentUri);
//            }
//        }
//    }
//
//    static private void performReturn(Context context, Bundle myExtras, String contentUri) {
//        Intent result = new Intent(null, Uri.parse(contentUri));
//        boolean resultSet = false;
//        if (myExtras != null) {
//            final Uri outputUri = (Uri)myExtras.getParcelable(MediaStore.EXTRA_OUTPUT);
//            if (outputUri != null) {
//                Bundle extras = new Bundle();
//                OutputStream outputStream = null;
//                try {
//                    outputStream = context.getContentResolver().openOutputStream(outputUri);
//                    if (outputStream != null) {
//                        InputStream inputStream = context.getContentResolver().openInputStream(Uri.parse(contentUri));
//                        Utils.copyStream(inputStream, outputStream);
//                        Util.closeSilently(inputStream);
//                    }
//                    ((Activity) context).setResult(Activity.RESULT_OK, new Intent(outputUri.toString())
//                            .putExtras(extras));
//                    resultSet = true;
//                } catch (Exception ex) {
//                    Log.e(TAG, "Cannot save to uri " + outputUri.toString());
//                } finally {
//                    Util.closeSilently(outputStream);
//                }
//            }
//        }
//        if (!resultSet && myExtras != null && myExtras.getBoolean("return-data")) {
//            // The size of a transaction should be below 100K.
//            Bitmap bitmap = null;
//            try {
//                bitmap = UriTexture.createFromUri(context, contentUri, 1024, 1024, 0, null);
//            } catch (IOException e) {
//                ;
//            } catch (URISyntaxException e) {
//                ;
//            }
//            if (bitmap != null) {
//                result.putExtra("data", bitmap);
//            }
//        }
//        if (!resultSet)
//            ((Activity) context).setResult(Activity.RESULT_OK, result);
//        ((Activity) context).finish();
//    }

}