package ug.newopendoor.activity.camera8;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
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
import android.widget.LinearLayout;
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
import ug.newopendoor.rx.RxBus;
import ug.newopendoor.service.Service2;
import ug.newopendoor.usbtest.ConvertUtils;
import ug.newopendoor.util.FileUtil;
import ug.newopendoor.util.MyUtil;
import ug.newopendoor.util.RoundImageView;
import ug.newopendoor.util.SoundPoolUtil;
import ug.newopendoor.util.Ticket;


/**
 * Created by dhht on 16/9/29.
 */

public class CameraActivity8 extends Activity implements SurfaceHolder.Callback, CameraContract8.View {
    private CameraContract8.Presenter presenter;
    @BindView(R.id.camera_sf)
    SurfaceView camera_sf;
    @BindView(R.id.img_server)
    RoundImageView img_server;

    @BindView(R.id.state_tip)
    TextView flag_tag;
    @BindView(R.id.ll_info)
    LinearLayout ll_info;
    @BindView(R.id.tv_ticket)
    TextView tv_ticket;

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
    private String ticketNum ="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_camera);
        ButterKnife.bind(this);
        new CameraPresenter8(this);
        holder = camera_sf.getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        device_id = MyUtil.getDeviceID(this);//获取设备号

        RxBus.getDefault().toObserverable(Ticket.class).subscribe((Ticket myMessage) -> {
            if (!isReading) {
                 BasicOper.dc_beep(5);
                ticketNum = myMessage.getNum().trim();
                if(!TextUtils.isEmpty(ticketNum)){
                    if (ticketNum.equals("0001|操作失败") || ticketNum.equals("FFFF|操作失败") || ticketNum.equals("1001|设备未打开")) {
                        stopService(new Intent(this,Service2.class));
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                startService(new Intent(CameraActivity8.this, Service2.class));
                            }
                        },8000);
                        return;
                    }
                    int n;
                    if(ticketNum.length()<16){
                       n= 16-ticketNum.length();
                       for (int i = 0;i<n;i++){
                           ticketNum = ticketNum + "0";
                       }
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tv_ticket.setText(ticketNum);
                            flag_tag.setText("正在验证");
                            flag_tag.setTextColor(getResources().getColor(R.color.white));
                        }
                    });
                    isReading = true;
                    takePhoto();
                }
            }
        });
    }

    private void takePhoto() {
        camera.takePicture(null, null, jpeg);
    }

    private Camera.PictureCallback jpeg = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
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
        Log.i("sss","ticketNum>>>票号：  " + ticketNum );
        boolean isNetAble = MyUtil.isNetworkAvailable(this);
        if (!isNetAble) {
            Toast.makeText(this, getResources().getText(R.string.error_net), Toast.LENGTH_LONG).show();
            uploadFinish();
            return;
        }

        presenter.load(device_id, ticketNum, file);
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
        startService(new Intent(this, Service2.class));
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

    /* 开始预览预览 */
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
    public void setPresenter(CameraContract8.Presenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void doCommonError(String text,int num,String Face_path) {
        flag_tag.setText(text);//没有检测到人脸
        SoundPoolUtil.play(num);
        if (!TextUtils.isEmpty(Face_path)) {
            RequestOptions options = new RequestOptions().error(R.drawable.left_img);
            if (!TextUtils.isEmpty(Face_path)) {
                Glide.with(CameraActivity8.this).load(Face_path).apply(options).into(img_server);
            }
        }
        flag_tag.setTextColor(getResources().getColor(R.color.red));
        rkGpioControlNative.ControlGpio(20, 0);//亮灯
        isLight = true;
        uploadFinish();
    }

    @Override
    public void doSuccess(String Face_path, String ticket_no,String seat_info) {
        if (!TextUtils.isEmpty(Face_path)) {
            RequestOptions options = new RequestOptions()
                    .error(R.drawable.left_img);
            if (!TextUtils.isEmpty(Face_path)) {
                Glide.with(CameraActivity8.this).load(Face_path).apply(options).into(img_server);
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
                tv_ticket.setText("");
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
