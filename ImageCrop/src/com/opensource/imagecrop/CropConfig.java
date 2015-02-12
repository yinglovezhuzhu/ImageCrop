package com.opensource.imagecrop;

/**
 * Use:
 * Created by yinglovezhuzhu@gmail.com on 2014-08-04.
 */
public class CropConfig {

	/**
	 * 裁剪圆形
	 */
    public static final String EXTRA_CIRCLE_CROP = "circleCrop";
    
    /**
     * 裁剪输出文件,Uri类型，请注意，这个参数不能也{@link #EXTRA_RETURN_DATA}一起使用，否则无效，
     * {@link #EXTRA_RETURN_DATA}拥有更高的优先级。
     */
    public static final String EXTRA_OUTPUT = "output";
    
    /**
     * 输出文件格式
     */
    public static final String EXTRA_OUTPUT_FORMAT = "outputFormat";
    
    /**
     * 裁剪的图片数据，Uri类型
     */
    public static final String EXTRA_DATA = "data";
    
    /**
     * 是否在ActivityResult中返回图片数据，boolean类型，返回的数据是Bitmap类型，
     * 返回数据中的key为{@link #EXTRA_DATA} value=data
     */
    public static final String EXTRA_RETURN_DATA = "return-data";
    
    /**
     * 裁剪图片的X比例，int类型
     */
    public static final String EXTRA_ASPECT_X = "aspectX";
    
    /**
     * 裁剪的Y比例，int类型
     */
    public static final String EXTRA_ASPECT_Y = "aspectY";
    
    /**
     * 输出图片的X像素值，int类型
     */
    public static final String EXTRA_OUTPUT_X = "outputX";
    
    /**
     * 输出图片的Y像素值，int类型
     */
    public static final String EXTRA_OUTPUT_Y = "outputY";
    
    /**
     * 是否需要缩放，boolean类型
     */
    public static final String EXTRA_SCALE = "scale";
    
    /**
     * 在输入图片的大小不满足输出图片的大小时，是否需要向上缩放，boolean类型，
     * 如果设置为true，图片可能会变模糊
     */
    public static final String EXTRA_SCALE_UP_IF_NEEDED = "scaleUpIfNeeded";
    
    /**
     * 
     */
    public static final String EXTRA_RECT = "rect";

    /**
     * 
     */
    public static final String ACTION_INLINE_DATA = "inline-data";

}
