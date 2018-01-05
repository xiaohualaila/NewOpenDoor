package ug.newopendoor.activity.camera2;

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
import android.widget.TextView;
import android.widget.Toast;
import com.bumptech.glide.Glide;
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
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import butterknife.BindView;
import butterknife.ButterKnife;
import ug.newopendoor.R;
import ug.newopendoor.activity.MainActivity;
import ug.newopendoor.service.CommonService;
import ug.newopendoor.service.ScreenService;
import ug.newopendoor.usbtest.ComBean;
import ug.newopendoor.usbtest.ConstUtils;
import ug.newopendoor.usbtest.M1CardListener;
import ug.newopendoor.usbtest.M1CardModel;
import ug.newopendoor.usbtest.MDSEUtils;
import ug.newopendoor.usbtest.SPUtils;
import ug.newopendoor.usbtest.SectorDataBean;
import ug.newopendoor.usbtest.SerialHelper;
import ug.newopendoor.usbtest.UltralightCardListener;
import ug.newopendoor.usbtest.UltralightCardModel;
import ug.newopendoor.usbtest.Utils;
import ug.newopendoor.util.FileUtil;
import ug.newopendoor.util.MyUtil;
import ug.newopendoor.util.SoundPoolUtil;


/**
 * Created by dhht on 16/9/29.
 */

public class CameraActivity2 extends Activity implements SurfaceHolder.Callback,CameraContract2.View,UltralightCardListener,M1CardListener {
    private CameraContract2.Presenter presenter;
    @BindView(R.id.camera_sf)
    SurfaceView camera_sf;
    @BindView(R.id.text_card)
    TextView text_card;
    @BindView(R.id.img1)
    ImageView img1;
    @BindView(R.id.img_server)
    ImageView img_server;

    @BindView(R.id.flag_tag)
    ImageView flag_tag;

    private Camera camera;
    private String filePath;
    private SurfaceHolder holder;
    private boolean isFrontCamera = true;
    private int width = 640;
    private int height = 480;

    private SPUtils settingSp;
    private String USB="";
    private boolean isOpenDoor = false;
    private CommonService myService;
    private CommonService.MyBinder myBinder;
    private Handler handler = new Handler();

    private boolean uitralight = true;
    private boolean scan = true;
    private boolean idcard = false;
    private boolean isHaveThree = false;
    //串口
    SerialControl ComA;
    DispQueueThread DispQueue;
    private boolean isReading = false;
    private String device_id;

    private SoundPoolUtil soundPoolUtil;

    ////////////////////////////////////屏保部分代码
    private Handler mHandler01 = new Handler();
    private Handler mHandler02 = new Handler();

    /* 上一次User有动作的Time Stamp */
    private Date lastUpdateTime;
    /* 计算User有几秒没有动作的 */
    private long timePeriod;

    /* 静止超过N秒将自动进入屏保 */
    private float mHoldStillTime = 30;
    /*标识当前是否进入了屏保*/
    private boolean isRunScreenSaver;

    /*时间间隔*/
    private long intervalScreenSaver = 1000;
    private long intervalKeypadeSaver = 1000;
    //////////////////////////////////////////
    /**
     * 3 身份证,1 Ultralight,4 M1,2串口
     */
    private int type;
    private String ticketNum;


    ///
     private boolean isAuto = true;
    private static Lock lock = new ReentrantLock();
    private Thread thread;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_camera2);
        ButterKnife.bind(this);
        new CameraPressenter2(this);
        holder = camera_sf.getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        startService(new Intent(this, ScreenService.class));
        device_id = MyUtil.getDeviceID(this);//获取设备号
        Intent intent = getIntent();
        uitralight = intent.getBooleanExtra("uitralight",true);
        scan = intent.getBooleanExtra("scan",true);
        idcard = intent.getBooleanExtra("idcard",false);
        isHaveThree = intent.getBooleanExtra("isHaveThree",false);
        Utils.init(getApplicationContext());
        settingSp = new SPUtils(getString(R.string.settingSp));
        USB = settingSp.getString(getString(R.string.usbKey), getString(R.string.androidUsb));
        rkGpioControlNative.init();
        //串口
        ComA = new SerialControl();
        DispQueue = new DispQueueThread();
        DispQueue.start();

        soundPoolUtil = SoundPoolUtil.getInstance(this);
        if(scan){
            openErWeiMa();
        }

          /* 初始取得User可触碰屏幕的时间 */
        lastUpdateTime = new Date(System.currentTimeMillis());

    }

    //打开设备
    public void onOpenConnectPort(){
        BasicOper.dc_AUSB_ReqPermission(this);
        int portSate = BasicOper.dc_open(USB, this, "", 0);
        if (portSate >= 0) {
            BasicOper.dc_beep(5);
            Log.d("sss", "portSate:" + portSate + "设备已连接");
        }else {
            Toast.makeText(this,"设备没有连接上！",Toast.LENGTH_LONG).show();
        }
    }

    //关闭设备
    public void onDisConnectPort() {
        int close_status = BasicOper.dc_exit();
        if(close_status>=0){
         //   Log.i("sss","设备关闭");
        }else {
          //  Log.i("sss","Port has closed");
        }
    }

    private void takePhoto(){
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
            Bitmap bm = BitmapFactory.decodeByteArray(data, 0, data.length,factory);
            Bitmap bm1 = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true);
            BufferedOutputStream bos = null;
            try {
                File file = new File(filePath);
                if(!file.exists()){
                    file.createNewFile();
                }
                bos = new BufferedOutputStream(new FileOutputStream(new File(filePath)));
                bm1.compress(Bitmap.CompressFormat.JPEG,30, bos);
                bos.flush();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (bos != null){
                    try {
                        bos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                bm.recycle();
                bm1.recycle();
                startPreview();
                Glide.with(CameraActivity2.this).load(filePath).error(R.drawable.left_img).into(img1);
                upload();
            }
        }
    };

    /**
     * 上传信息
     */
    private void  upload(){
       // Log.i("xxxx","type >>" + type +"" +" ticketNum>>" + ticketNum);
        boolean isNetAble = MyUtil.isNetworkAvailable(this);
        if(!isNetAble){
            Toast.makeText(this,"网路无法连接！",Toast.LENGTH_LONG).show();
            uploadFinish();
            return;
        }
        File  file = new File(filePath);
        if(!file.exists() ){
            uploadFinish();
            return;
        }
        presenter.load(device_id,type,ticketNum,file);
    }

    /**
     * 0.5秒关门
     */
    private void uploadFinish() {
        isReading =false;
        ticketNum = "";
        File file = new File(filePath);
        if(file.exists()){
            file.delete();
        }
        if(isOpenDoor){
            isOpenDoor = false;
            handler.postDelayed(runnable,500);
        }
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                text_card.setText("");
                flag_tag.setImageResource(R.drawable.welcome);
                img1.setImageResource(R.drawable.left_img);
            }
        },1000);
    }

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
                rkGpioControlNative.ControlGpio(1, 1);//关门
        }
    };

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
        ///////////////////////////////////
        mHandler01.postAtTime(mTask01, intervalKeypadeSaver);

        onOpenConnectPort();
        //身份证
        isAuto = true;
        thread = new Thread(task);
        thread.start();
        //UltralightCard
        model = new UltralightCardModel(this);
        //M1
        model2 = new M1CardModel(this);

    }

    @Override
    protected void onPause() {
        super.onPause();
          /*activity不可见的时候取消线程*/
        mHandler01.removeCallbacks(mTask01);
        mHandler02.removeCallbacks(mTask02);



        isAuto =false;
        thread.interrupt();
        onDisConnectPort();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        closeCamera();
        adcNative.close(0);
        adcNative.close(2);
        rkGpioControlNative.close();
        closeErWeiMa();
        stopService( new Intent(this, ScreenService.class));


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
        setPictureSize(para,640 , 480);
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
        para.setPictureSize(absWidth,absHeight);
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

    private void OpenComPort(SerialHelper ComPort){
        try
        {
            ComPort.open();
        } catch (SecurityException e) {
            Log.i("xxx","SecurityException" + e.toString());
        } catch (IOException e) {
            Log.i("xxx","IOException" + e.toString());
        } catch (InvalidParameterException e) {
            Log.i("xxx","InvalidParameterException" + e.toString());
        }
    }

    public void closeErWeiMa() {
        CloseComPort(ComA);
    }

    private void CloseComPort(SerialHelper ComPort){
        if (ComPort!=null){
            ComPort.stopSend();
            ComPort.close();
        }
    }

    @Override
    public void setPresenter(CameraContract2.Presenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void doError() {
        text_card.setText("请求失败！");
        flag_tag.setImageResource(R.drawable.not_pass);
        soundPoolUtil.play(3);
        uploadFinish();
    }

    @Override
    public void doSuccess(String Face_path) {
        if(!TextUtils.isEmpty(Face_path)){
            Glide.with(CameraActivity2.this).load(Face_path).error(R.drawable.left_img).into(img_server);
        }
        isOpenDoor = true;
        rkGpioControlNative.ControlGpio(1, 0);//开门
         soundPoolUtil.play(4);
        flag_tag.setImageResource(R.drawable.pass);
        uploadFinish();
    }



    private class SerialControl extends SerialHelper{

        public SerialControl(){
        }

        @Override
        protected void onDataReceived(final ComBean ComRecData)
        {
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
                    runOnUiThread(new Runnable() {
                        public void run() {
                            if(!isReading){
                                updateUserActionTime();
                                isReading =true;
                                try {
                                    ticketNum = new String(ComData.bRec).trim();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                type = 2;
                                takePhoto();
                            }
                        }
                    });
                    try {
                        Thread.sleep(500);
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


    /////////////////////////////////////////////////////////屏保部分代码

    /**
     * 计时线程
     */
    private Runnable mTask01 = new Runnable() {

        @Override
        public void run() {
            Date timeNow = new Date(System.currentTimeMillis());
            /* 计算User静止不动作的时间间距 */
            /**当前的系统时间 - 上次触摸屏幕的时间 = 静止不动的时间**/
            timePeriod = (long) timeNow.getTime() - (long) lastUpdateTime.getTime();

            /*将静止时间毫秒换算成秒*/
            float timePeriodSecond = ((float) timePeriod / 1000);

            if(timePeriodSecond > mHoldStillTime){
                if(isRunScreenSaver == false){  //说明没有进入屏保
                    /* 启动线程去显示屏保 */
                    mHandler02.postAtTime(mTask02, intervalScreenSaver);
                    /*显示屏保置为true*/
                    isRunScreenSaver = true;
                }else{
                    /*屏保正在显示中*/
                }
            }else{
                /*说明静止之间没有超过规定时长*/
                isRunScreenSaver = false;
            }
            /*反复调用自己进行检查*/
            mHandler01.postDelayed(mTask01, intervalKeypadeSaver);
        }
    };

    /**
     * 持续屏保显示线程
     */
    private Runnable mTask02 = new Runnable() {

        @Override
        public void run() {
            // TODO Auto-generated method stub
            if (isRunScreenSaver == true) {  //如果屏保正在显示，就计算不断持续显示
//              hideOriginalLayout();
                showScreenSaver();
                mHandler02.postDelayed(mTask02, intervalScreenSaver);
            } else {
                mHandler02.removeCallbacks(mTask02);  //如果屏保没有显示则移除线程
            }
        }
    };

    /**
     * 显示屏保
     */
    private void showScreenSaver(){
        Log.d("danxx", "显示屏保------>");
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);

    }

    /*用户有操作的时候不断重置静止时间和上次操作的时间*/
    public void updateUserActionTime() {
        Date timeNow = new Date(System.currentTimeMillis());
        timePeriod = timeNow.getTime() - lastUpdateTime.getTime();
        lastUpdateTime.setTime(timeNow.getTime());
    }

    //三和一
    private int flag = 3;
    private final int TIME = 1000;
    //UltralightCard读卡
    private UltralightCardModel model;
    private boolean isHaveOne = false;
    //M1
    private M1CardModel model2;

    Runnable task = new Runnable() {
        @Override
        public void run() {
            while (isAuto) {
                lock.lock();
                try {
                    if(flag == 1){//UltralightCard
                        if(uitralight){
                            model.bt_seek_card(ConstUtils.BT_SEEK_CARD);
                            Log.i("sss",">>>>>>>>>>>>>>>>>>>>>>UltralightCard");
                            Thread.sleep(TIME);
                        }else {//M1
                            if (MDSEUtils.isSucceed(BasicOper.dc_card_hex(1))) {
                                final int keyType = 0;// 0 : 4; 密钥套号 0(0套A密钥)  4(0套B密钥)
                                isHaveOne = true;
                                model2.bt_read_card(ConstUtils.BT_READ_CARD,keyType,0);
                            }
                            Log.i("sss",">>>>>>>>>>>>>>>>>>>>>>M1");
                            Thread.sleep(TIME);
                        }
                        if(idcard){
                            flag = 2;
                        }
                    }else if(flag == 2){//身份证
                        if(idcard){
                            Log.i("sss",">>>>>>>>>>>>>>>>>>>>>>身份证");
                            com.decard.entitys.IDCard idCardData;
                                idCardData = BasicOper.dc_get_i_d_raw_info();
                            if(idCardData!= null){
                                updateUserActionTime();
                                BasicOper.dc_beep(5);
                                if(!isReading){
                                    type = 3;
                                    ticketNum = idCardData.getId().trim();
                                    isReading = true;
                                    takePhoto();
                                }
                            }
                            Thread.sleep(1000);
                        }
                        if(isHaveThree){
                            flag = 1;
                        }
                    }else if(flag == 3){
                        if(isHaveThree){
                            flag = 1;
                        }
                        if(idcard){
                            flag = 2;
                        }
                        Thread.sleep(2000);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    lock.unlock();
                }
            }
        }
    };

    @Override
    public void getUltralightCardResult(String cmd, String result) {
        if(!result.equals("1003|无卡或无法寻到卡片")){
            if(!result.equals("0001|操作失败")){
                if(!result.equals("FFFF|操作失败")){
                   onBackMsg(1,result);
                }
            }
        }
    }

    private void onBackMsg(int i, String result) {
        updateUserActionTime();
        BasicOper.dc_beep(5);
        if(!isReading){
            isReading = true;
            type = i;
            if(i == 1){
                ticketNum = result.trim() + "00";
            }else {
                ticketNum = result.trim();
            }
            takePhoto();
        }


    }

    @Override
    public void getM1CardResult(String cmd, List<String> list, String result, String resultCode) {
        if(isHaveOne){
            isHaveOne = false;
            if (list == null){
                if (result.length() > 2){
                    readSectorData(Integer.parseInt(resultCode));
                }else {
                    readSectorData(Integer.parseInt(result));
                }
            }
        }
    }

    private void readSectorData(int currentSectors) {
        boolean b = true;
        int piece = (currentSectors + 1) * 4;
        SectorDataBean sectorDataBean = new SectorDataBean();
        String[] pieceDatas = new String[4];
        for (int i = piece - 4, j = 0; i < piece; i++, j++) {
            String pieceData = MDSEUtils.returnResult(BasicOper.dc_read_hex(i));
            pieceDatas[j] = pieceData;
        }
        sectorDataBean.pieceZero = pieceDatas[0];

        if (b){
            String string = sectorDataBean.pieceZero.substring(0,8);
            onBackMsg(4,string);
            b = false;
        }

    }
}
