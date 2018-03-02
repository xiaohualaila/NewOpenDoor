package ug.newopendoor.activity.camera6;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.cmm.rkgpiocontrol.rkGpioControlNative;
import com.decard.NDKMethod.BasicOper;
import com.decard.entitys.IDCard;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import ug.newopendoor.R;
import ug.newopendoor.retrofit.ConnectUrl;
import ug.newopendoor.rx.RxBus;
import ug.newopendoor.service.Service2;
import ug.newopendoor.usbtest.ConvertUtils;
import ug.newopendoor.util.FileUtil;
import ug.newopendoor.util.MyUtil;
import ug.newopendoor.util.RoundImageView;
import ug.newopendoor.util.SharedPreferencesUtil;
import ug.newopendoor.util.SoundPoolUtil;
import ug.newopendoor.util.Ticket;


/**
 * Created by dhht on 16/9/29.
 */

public class CameraActivity6 extends Activity implements SurfaceHolder.Callback, CameraContract6.View {
    private CameraContract6.Presenter presenter;
    @BindView(R.id.camera_sf)
    SurfaceView camera_sf;
    @BindView(R.id.img_server)
    RoundImageView img_server;

    @BindView(R.id.state_tip)
    TextView flag_tag;
    @BindView(R.id.tv_name)
    TextView tv_name;
    @BindView(R.id.tv_idcard)
    TextView tv_idcard;
    private Camera camera;
    private String filePath;
    private SurfaceHolder holder;
    private boolean isFrontCamera = true;
    private int width = 640;
    private int height = 480;

    private boolean isOpenDoor = false;
    private boolean isLight = false;
    private Handler handler = new Handler();

    private boolean isReading = false;
    private String device_id;

    /**
     * 3 身份证,1 Ultralight,4 M1,2串口
     */
    private int type;
    private String ticketNum ="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_camera5);
        ButterKnife.bind(this);
        new CameraPressenter6(this);
        holder = camera_sf.getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        device_id = MyUtil.getDeviceID(this);//获取设备号

        RxBus.getDefault().toObserverable(Ticket.class).subscribe(myMessage -> {
            if (!isReading) {
                isReading = true;
                type = myMessage.getType();
                if (type == 1) {
                    ticketNum = myMessage.getNum().trim() + "00";
                } else {
                    ticketNum = myMessage.getNum().trim();
                }
                takePhoto();
            }
        });
        RxBus.getDefault().toObserverable(IDCard.class).subscribe(idCard -> {
            BasicOper.dc_beep(5);
            if (!isReading) {
                isReading = true;
                type = 3;
                if(idCard != null){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tv_name.setText(idCard.getName());
                            tv_idcard.setText(idCard.getId());
                            img_server.setImageBitmap(ConvertUtils.bytes2Bitmap(ConvertUtils.hexString2Bytes(idCard.getPhotoDataHexStr())));
                        }
                    });
                }
                ticketNum = idCard.getId().trim();
                takePhoto();
            }
        });

    }

    private void takePhoto() {
        camera.takePicture(null, null, jpeg);
    }

    private Camera.PictureCallback jpeg = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            stopPreview();
            filePath = FileUtil.getPath() + File.separator + FileUtil.getTime() + ".jpeg";
            Matrix matrix = new Matrix();
            matrix.reset();
            matrix.postRotate(0);
            BitmapFactory.Options factory = new BitmapFactory.Options();
            factory = setOptions(factory);
            Bitmap bm = BitmapFactory.decodeByteArray(data, 0, data.length, factory);
            Bitmap bm1 = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true);
            BufferedOutputStream bos = null;
            try {
                bos = new BufferedOutputStream(new FileOutputStream(new File(filePath)));
                bm1.compress(Bitmap.CompressFormat.JPEG, 30, bos);
                bos.flush();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (bos != null) {
                    try {
                        bos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                bm.recycle();
                bm1.recycle();
              //  startCameraPreview();
                upload();
            }
        }
    };

    /**
     * 上传信息
     */
    private void upload() {
        File file = new File(filePath);
        if (!file.exists()) {
            uploadFinish();
            return;
        }
        boolean isNetAble = MyUtil.isNetworkAvailable(this);
        if (!isNetAble) {
            Toast.makeText(this, getResources().getText(R.string.error_net), Toast.LENGTH_LONG).show();
            uploadFinish();
            return;
        }
        //  String url =  SharedPreferencesUtil.getStringByKey("ip_address",this);
        String url = ConnectUrl.URL;
        presenter.load(device_id, type, ticketNum, file,url);
    }

    public static BitmapFactory.Options setOptions(BitmapFactory.Options opts) {
        opts.inJustDecodeBounds = false;
        opts.inPurgeable = true;
        opts.inInputShareable = true;
        opts.inPreferredConfig = Bitmap.Config.RGB_565;
        opts.inSampleSize = 1;
        return opts;
    }

    @Override
    protected void onResume() {
        super.onResume();
        camera = openCamera();
        Intent intent = new Intent(this, Service2.class);
        startService(intent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopService(new Intent(this,Service2.class));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeCamera();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            camera.setPreviewDisplay(holder);
        } catch (IOException exception) {

        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        startPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stopPreview();
    }

    private Camera openCamera() {
        if (camera == null) {
            try {
                camera = Camera.open();
            } catch (Exception e) {
                camera = null;
                e.printStackTrace();
            }
        }
        return camera;
    }

    private void startPreview() {
        Camera.Parameters para;
        if (null != camera) {
            para = camera.getParameters();
        } else {
            return;
        }
        para.setPreviewSize(width, height);
        setPictureSize(para, 640, 480);
        para.setPictureFormat(ImageFormat.JPEG);//设置图片格式
        setCameraDisplayOrientation(isFrontCamera ? 0 : 1, camera);
        camera.setParameters(para);
        camera.startPreview();
    }

    /* 停止预览 */
    private void stopPreview() {
        if (camera != null) {
            try {
                camera.stopPreview();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    /* 开始预览 */
    private void startCameraPreview() {
        if (camera != null) {
            try {
                camera.startPreview();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    public void setCameraDisplayOrientation(int cameraId, Camera camera) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int rotation = this.getWindowManager().getDefaultDisplay().getRotation();
        rotation = 0;
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;
        } else {
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    private void setPictureSize(Camera.Parameters para, int width, int height) {
        int absWidth = 0;
        int absHeight = 0;
        List<Camera.Size> supportedPictureSizes = para.getSupportedPictureSizes();
        for (Camera.Size size : supportedPictureSizes) {
            if (Math.abs(width - size.width) < Math.abs(width - absWidth)) {
                absWidth = size.width;
            }
            if (Math.abs(height - size.height) < Math.abs(height - absHeight)) {
                absHeight = size.height;
            }
        }
        para.setPictureSize(absWidth, absHeight);
    }

    private void closeCamera() {
        if (null != camera) {
            try {
                camera.setPreviewDisplay(null);
                camera.setPreviewCallback(null);
                camera.release();
                camera = null;
            } catch (IOException e) {
                e.printStackTrace();
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void setPresenter(CameraContract6.Presenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void requestFail() {
        flag_tag.setText(getResources().getText(R.string.error_net));
      //  SoundPoolUtil.play(3);
        doErrorRequest();
    }

    @Override
    public void doError() {
        flag_tag.setText(getResources().getText(R.string.error_ticket));
        SoundPoolUtil.play(3);
        doErrorRequest();
    }

    @Override
    public void doFaceError() {
        flag_tag.setText(getResources().getText(R.string.error_face));
        SoundPoolUtil.play(1);
        doErrorRequest();
    }

    public void doErrorRequest(){
        flag_tag.setTextColor(getResources().getColor(R.color.red));
        rkGpioControlNative.ControlGpio(20, 0);//亮灯
        isLight = true;
        uploadFinish();
    }

    @Override
    public void doSuccess(String Face_path) {
        if (!TextUtils.isEmpty(Face_path)) {
            RequestOptions options = new RequestOptions()
                    .error(R.drawable.left_img);
            if (!TextUtils.isEmpty(Face_path)) {
                Glide.with(CameraActivity6.this).load(Face_path).apply(options).into(img_server);
            }
        }
        isOpenDoor = true;
        rkGpioControlNative.ControlGpio(1, 0);//开门
        SoundPoolUtil.play(4);
        flag_tag.setText(getResources().getText(R.string.right_ticket));
        flag_tag.setTextColor(getResources().getColor(R.color.green));
        uploadFinish();
    }

    private void uploadFinish() {

        if (isOpenDoor) {
            isOpenDoor = false;
            handler.postDelayed(runnable, 500);
        }

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                startCameraPreview();
                img_server.setImageResource(R.drawable.left_img);
                flag_tag.setText("");
                tv_idcard.setText("");
                tv_name.setText("");
                File file = new File(filePath);
                if (file.exists()) {
                    file.delete();
                }
                //变灯
                if (isLight) {
                    rkGpioControlNative.ControlGpio(20, 1);
                    isLight = false;
                }
                isReading = false;
                ticketNum = "";
            }
        }, 2500);

    }

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            rkGpioControlNative.ControlGpio(1, 1);//关门
        }
    };


}
