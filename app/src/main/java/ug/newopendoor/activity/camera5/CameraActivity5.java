package ug.newopendoor.activity.camera5;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.cmm.rkadcreader.adcNative;
import com.cmm.rkgpiocontrol.rkGpioControlNative;
import com.decard.NDKMethod.BasicOper;
import com.decard.entitys.IDCard;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import butterknife.BindView;
import butterknife.ButterKnife;
import ug.newopendoor.R;
import ug.newopendoor.service.CommonService;
import ug.newopendoor.usbtest.ComBean;
import ug.newopendoor.usbtest.ConvertUtils;
import ug.newopendoor.usbtest.SPUtils;
import ug.newopendoor.usbtest.SerialHelper;
import ug.newopendoor.usbtest.Utils;
import ug.newopendoor.util.FileUtil;
import ug.newopendoor.util.MyUtil;
import ug.newopendoor.util.RoundImageView;
import ug.newopendoor.util.SoundPoolUtil;


/**
 * Created by dhht on 16/9/29.
 */

public class CameraActivity5 extends Activity implements SurfaceHolder.Callback, CameraContract5.View {
    private CameraContract5.Presenter presenter;
    @BindView(R.id.camera_sf)
    SurfaceView camera_sf;
//    @BindView(R.id.img1)
//    RoundImageView img1;
    @BindView(R.id.img_server)
    RoundImageView img_server;

    @BindView(R.id.state_tip)
    TextView flag_tag;
    @BindView(R.id.ad)
    ImageView ad;
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

    private SPUtils settingSp;
    private String USB = "";
    private boolean isOpenDoor = false;
    private boolean isLight = false;

    private CommonService myService;
    private CommonService.MyBinder myBinder;
    private Handler handler = new Handler();

    private boolean uitralight = true;
    private boolean scan = true;
    private boolean idcard = true;
    private boolean m1 = true;
    //串口
    SerialControl ComA;
    DispQueueThread DispQueue;
    private boolean isReading = false;
    private String device_id;
    /**
     * 3 身份证,1 Ultralight,4 M1,2串口
     */
    private int type;
    private String ticketNum;
    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            myBinder = (CommonService.MyBinder) service;
            myService = myBinder.getService();
            myBinder.setIntentData(m1, uitralight, idcard);
            myService.setOnProgressListener(new CommonService.OnDataListener() {
                @Override
                public void onIDCardMsg(IDCard idCardData) {//身份证
                    BasicOper.dc_beep(5);
                    if (!isReading) {
                        isReading = true;
                        type = 3;
                        if(idCardData != null){
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    tv_name.setText(idCardData.getName());
                                    tv_idcard.setText(idCardData.getId());
                                    img_server.setImageBitmap(ConvertUtils.bytes2Bitmap(ConvertUtils.hexString2Bytes(idCardData.getPhotoDataHexStr())));
                                }
                            });
                        }
                        ticketNum = idCardData.getId().trim();
                        takePhoto();
                    }
                }

                @Override
                public void onBackMsg(int mType, String result) {
                    BasicOper.dc_beep(5);
                    if (!isReading) {
                        isReading = true;
                        type = mType;
                        if (mType == 1) {
                            ticketNum = result.trim() + "00";
                        } else {
                            ticketNum = result.trim();
                        }
                        takePhoto();
                    }
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_camera5);
        ButterKnife.bind(this);
        new CameraPressenter5(this);
        holder = camera_sf.getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        device_id = MyUtil.getDeviceID(this);//获取设备号
        Intent intent = getIntent();
        uitralight = intent.getBooleanExtra("uitralight", true);//芯片
        scan = intent.getBooleanExtra("scan", true);//扫描二维码
        idcard = intent.getBooleanExtra("idcard", true);//身份证
        m1 = intent.getBooleanExtra("isHaveThree", true);//m1卡
        Utils.init(getApplicationContext());
        settingSp = new SPUtils(getString(R.string.settingSp));
        USB = settingSp.getString(getString(R.string.usbKey), getString(R.string.androidUsb));
        rkGpioControlNative.init();
        //串口
        ComA = new SerialControl();
        DispQueue = new DispQueueThread();
        DispQueue.start();
        if (scan) {
            openErWeiMa();
        }

        Intent bindIntent1 = new Intent(this, CommonService.class);
        bindService(bindIntent1, connection, BIND_AUTO_CREATE);
//        RequestOptions options = new RequestOptions()
//                .error(R.drawable.ad);
//        Glide.with(CameraActivity5.this).load(ConnectUrl.URL_IMG).apply(options).into(ad);


    }

    //打开设备
    public void onOpenConnectPort() {
        BasicOper.dc_AUSB_ReqPermission(this);
        int portSate = BasicOper.dc_open(USB, this, "", 0);
        if (portSate >= 0) {
            BasicOper.dc_beep(5);
        }
    }

    //关闭设备
    public void onDisConnectPort() {
         BasicOper.dc_exit();
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
                upload();
            }
        }
    };

    /**
     * 上传信息
     */
    private void upload() {
         Log.i("xxx","type >>" + type +"" +" ticketNum>>" + ticketNum);
        File file = new File(filePath);
        if (!file.exists()) {
            uploadFinish();
            return;
        }
        boolean isNetAble = MyUtil.isNetworkAvailable(this);
        if (!isNetAble) {
            Toast.makeText(this, "网路无法连接！", Toast.LENGTH_LONG).show();
            uploadFinish();
            return;
        }

        presenter.load(device_id, type, ticketNum, file);
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
        onOpenConnectPort();
    }

    @Override
    protected void onPause() {
        super.onPause();
        onDisConnectPort();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (myBinder != null) {
            myBinder.stopThread();
        }
        unbindService(connection);
        closeCamera();
        adcNative.close(0);
        adcNative.close(2);
        rkGpioControlNative.close();
        closeErWeiMa();
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


    //打开串口
    public void openErWeiMa() {
        ComA.setPort("/dev/ttyS4");
        ComA.setBaudRate("115200");
        OpenComPort(ComA);
    }

    private void OpenComPort(SerialHelper ComPort) {
        try {
            ComPort.open();
        } catch (SecurityException e) {
            Log.i("xxx", "SecurityException" + e.toString());
        } catch (IOException e) {
            Log.i("xxx", "IOException" + e.toString());
        } catch (InvalidParameterException e) {
            Log.i("xxx", "InvalidParameterException" + e.toString());
        }
    }

    public void closeErWeiMa() {
        CloseComPort(ComA);
    }

    private void CloseComPort(SerialHelper ComPort) {
        if (ComPort != null) {
            ComPort.stopSend();
            ComPort.close();
        }
    }

    @Override
    public void setPresenter(CameraContract5.Presenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void doError() {
        flag_tag.setText("验证失败");
        flag_tag.setTextColor(getResources().getColor(R.color.red));
        rkGpioControlNative.ControlGpio(20, 0);//亮灯
        isLight = true;
        SoundPoolUtil.play(3);
        uploadFinish();
    }

    @Override
    public void doFaceError() {
        flag_tag.setText("人脸验证失败");
        flag_tag.setTextColor(getResources().getColor(R.color.red));
        rkGpioControlNative.ControlGpio(20, 0);//亮灯
        isLight = true;
        SoundPoolUtil.play(1);
        uploadFinish();
    }

    @Override
    public void doSuccess(String Face_path) {
        if (!TextUtils.isEmpty(Face_path)) {
            RequestOptions options = new RequestOptions()
                    .error(R.drawable.left_img);
            Glide.with(CameraActivity5.this).load(Face_path).apply(options).into(img_server);
        }
        isOpenDoor = true;
        rkGpioControlNative.ControlGpio(1, 0);//开门
        SoundPoolUtil.play(4);
        flag_tag.setText("验证成功");
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
                startPreview();
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


    private class SerialControl extends SerialHelper {

        public SerialControl() {
        }

        @Override
        protected void onDataReceived(final ComBean ComRecData) {
            DispQueue.AddQueue(ComRecData);
        }
    }

    private class DispQueueThread extends Thread {
        private Queue<ComBean> QueueList = new LinkedList<ComBean>();

        @Override
        public void run() {
            super.run();
            while (!isInterrupted()) {
                final ComBean ComData;
                while ((ComData = QueueList.poll()) != null) {
                    if (!isReading) {
                        isReading = true;
                        try {
                            ticketNum = new String(ComData.bRec).trim();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        type = 2;
                        takePhoto();
                    }
                    try {
                        Thread.sleep(800);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }

        public synchronized void AddQueue(ComBean ComData) {
            QueueList.add(ComData);
        }
    }

}
