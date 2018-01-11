package ug.newopendoor.activity.camera3;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.cmm.rkgpiocontrol.rkGpioControlNative;
import com.decard.NDKMethod.BasicOper;
import org.json.JSONObject;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import butterknife.BindView;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import ug.newopendoor.R;
import ug.newopendoor.activity.base.BaseFragment;
import ug.newopendoor.retrofit.Api;
import ug.newopendoor.retrofit.ConnectUrl;
import ug.newopendoor.rx.RxBus;
import ug.newopendoor.util.FileUtil;
import ug.newopendoor.util.MyUtil;
import ug.newopendoor.util.RoundImageView;
import ug.newopendoor.util.SoundPoolUtil;
import ug.newopendoor.util.Ticket;


/**
 * Created by dhht on 16/9/29.
 */

public class CameraFragment extends BaseFragment implements SurfaceHolder.Callback {
    @BindView(R.id.camera_sf)
    SurfaceView camera_sf;
    @BindView(R.id.text_card)
    TextView text_card;
    @BindView(R.id.img1)
    RoundImageView img1;
    @BindView(R.id.img_server)
    RoundImageView img_server;

    @BindView(R.id.flag_tag)
    ImageView flag_tag;
    private Camera camera;
    private String filePath;
    private SurfaceHolder holder;
    private boolean isFrontCamera = true;
    private int width = 640;
    private int height = 480;
    private String device_id;
    private boolean isReading = false;
    private String ticketNum;
    private int type;
    private boolean isOpenDoor = false;
    private Handler handler = new Handler();

    @Override
    protected int getLayoutId() {
        return R.layout.activity_camera2;
    }

    @Override
    protected void init() {
        holder = camera_sf.getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        device_id = MyUtil.getDeviceID(getActivity());//获取设备号
        RxBus.getDefault().toObserverable(Ticket.class).subscribe(myMessage -> {
            Log.i("sss","isReading>>>>  " +isReading );
            if(!isReading){
                type = myMessage.getType();
                if(type != 2 ){
                    BasicOper.dc_beep(5);
                }
                if(type==1){
                    ticketNum = myMessage.getNum().trim() + "00";
                }else {
                    ticketNum = myMessage.getNum().trim();
                }
                isReading = true;
                Log.i("sss","type >>" + type +"" +" ticketNum>>" + ticketNum);
                FragmentActivity activity = (FragmentActivity ) getActivity();
                activity.updateUserActionTime();
                takePhoto();
            }
        });
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
                RequestOptions options = new RequestOptions()
                        .error(R.drawable.left_img);
                Glide.with(getActivity()).load(filePath).apply(options).into(img1);
                upload();
            }
        }
    };

    /**
     * 上传信息
     */
    private void  upload(){
        File  file = new File(filePath);
        if(!file.exists() ){
            uploadFinish();
            return;
        }
        boolean isNetAble = MyUtil.isNetworkAvailable(getActivity());
        if(!isNetAble){
            Toast.makeText(getActivity(),"网路无法连接！",Toast.LENGTH_LONG).show();
            uploadFinish();
            return;
        }
        MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);
        RequestBody requestBody = RequestBody.create(MediaType.parse("multipart/form-data"), file);
        builder.addFormDataPart("photoImgFiles", file.getName(), requestBody);
        Api.getBaseApiWithOutFormat(ConnectUrl.URL)
                .uploadPhotoBase(device_id,ticketNum,type,builder.build().parts())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<JSONObject>() {
                               @Override
                               public void call(JSONObject jsonObject) {
                                   jsonObjectResult(jsonObject);
                               }
                           }, new Action1<Throwable>() {
                               @Override
                               public void call(Throwable throwable) {

                               }
                           }
                );
    }
    private void jsonObjectResult(JSONObject jsonObject){
        if(jsonObject !=null){
            String result = jsonObject.optString("Result");
            if(!TextUtils.isEmpty(result)){
                if(result.equals("1")){
                    String imageStr = jsonObject.optString("Face_path");
                    doSuccess(imageStr);
                }else if(result.equals("5")){
                    doFaceError();
                }else{
                    doError();
                }
            }else {
                doError();
            }
        }else {
            doError();
        }
    }

    public void doFaceError() {
        flag_tag.setImageResource(R.drawable.face_error);
        SoundPoolUtil.play(1);
        uploadFinish();
    }

    public void doError() {
        flag_tag.setImageResource(R.drawable.not_pass);
        SoundPoolUtil.play(3);
        uploadFinish();
    }
    public void doSuccess(String Face_path) {
        if(!TextUtils.isEmpty(Face_path)){
            RequestOptions options = new RequestOptions()
                    .error(R.drawable.left_img);
            if(!TextUtils.isEmpty(Face_path)){
                Glide.with(getActivity()).load(Face_path).apply(options).into(img_server);
            }
        }
        isOpenDoor = true;
        rkGpioControlNative.ControlGpio(1, 0);//开门
        rkGpioControlNative.ControlGpio(20, 0);//亮灯
        SoundPoolUtil.play(4);
        flag_tag.setImageResource(R.drawable.pass);
        uploadFinish();
    }

    /**
     * 0.5秒关门
     */
    private void uploadFinish() {
        isReading =false;
        ticketNum = "";

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
                File file = new File(filePath);
                if(file.exists()){
                    file.delete();
                }
            }
        },1000);
    }

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            rkGpioControlNative.ControlGpio(1, 1);//关门
            rkGpioControlNative.ControlGpio(20, 1);//变灯
        }
    };


    @Override
    public void onResume() {
        super.onResume();
        camera = openCamera();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        closeCamera();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) {
            isReading = true;
        } else {
            isReading = false;
        }
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
        int rotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
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

    public static BitmapFactory.Options setOptions(BitmapFactory.Options opts) {
        opts.inJustDecodeBounds = false;
        opts.inPurgeable = true;
        opts.inInputShareable = true;
        opts.inPreferredConfig = Bitmap.Config.RGB_565;
        opts.inSampleSize = 1;
        return opts;
    }



}
