package com.exc.zhen.orienteering;

/**
 * Created by ZHEN on 2015/10/11 0011.
 * 照相预览界面
 */
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.camera2.CameraManager;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class MySurfaceView extends SurfaceView implements SurfaceHolder.Callback{

    public MySurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MySurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private Context supercontext;

    @SuppressWarnings("deprecation")
    private Camera camera = null;
    private SurfaceHolder surfaceHolder = null;
    private Camera.Parameters parameters = null;

    @SuppressWarnings("deprecation")
    public MySurfaceView(Context context, Camera camera) {
        super(context);
        supercontext = context;
        this.camera = camera;
        surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);
        //noinspection deprecation
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }
    public MySurfaceView(Context context) {
        super(context);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try{
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
        //根本没有可处理的SurfaceView
        if (surfaceHolder.getSurface() == null){
            return ;
        }

        //先停止Camera的预览
        try{
            camera.stopPreview();
        }catch(Exception e){
            e.printStackTrace();
        }

        //这里可以做一些我们要做的变换。
//        parameters = camera.getParameters(); // 获取各项参数
//        parameters.setPictureFormat(PixelFormat.JPEG); // 设置图片格式
//        parameters.setPreviewSize(width, height); // 设置预览大小
//        parameters.setPreviewFrameRate(5);  //设置每秒显示4帧
//        parameters.setPictureSize(width, height); // 设置保存的图片尺寸
//        parameters.setJpegQuality(80); // 设置照片质量
//        camera.setParameters(parameters);
        //重新开启Camera的预览功能
        try{
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }
}
