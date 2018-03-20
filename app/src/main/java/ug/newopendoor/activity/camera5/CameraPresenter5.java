package ug.newopendoor.activity.camera5;

import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONObject;
import java.io.File;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import ug.newopendoor.activity.base.BasePresenter;
import ug.newopendoor.retrofit.Api;
import ug.newopendoor.retrofit.ConnectUrl;


/**
 * Created by Administrator on 2017/6/3.
 */

public class CameraPresenter5 extends BasePresenter implements CameraContract5.Presenter {
    private CameraContract5.View view;

    public CameraPresenter5(CameraContract5.View view) {
        this.view = view;
        this.view.setPresenter(this);
    }

    @Override
    public void start() {

    }

    @Override
    public void load(String device_id, int type, String ticketNum, File newFile) {
        MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);
        RequestBody requestBody = RequestBody.create(MediaType.parse("multipart/form-data"), newFile);
        builder.addFormDataPart("photoImgFiles", newFile.getName(), requestBody);
        Api.getBaseApiWithOutFormat(ConnectUrl.URL)
                .uploadPhotoBase(device_id, ticketNum, type, builder.build().parts())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<JSONObject>() {
                               @Override
                               public void call(JSONObject jsonObject) {
                                  // Log.i("sss",jsonObject.toString());
                                   jsonObjectResult(jsonObject);
                               }
                           }, new Action1<Throwable>() {
                               @Override
                               public void call(Throwable throwable) {
                                  // Log.i("sss",throwable.toString());
                                   view.doCommonError("网络异常",6);
                               }
                           }
                );
    }

    private void jsonObjectResult(JSONObject jsonObject) {
        if (jsonObject != null) {
            String result = jsonObject.optString("Result");
            if (!TextUtils.isEmpty(result)) {
                if (result.equals("1")) {
                    String imageStr = jsonObject.optString("Face_path");
                    view.doSuccess(imageStr);
                }  else if (result.equals("2")) {//入场次数已用完
                    view.doCommonError("入场次数已用完",7);
                } else if (result.equals("3")) {//入场口错误
                    view.doCommonError("入场口错误",8);
                } else if (result.equals("5")) {
                    view.doCommonError("人脸验证失败",1);
                } else if (result.equals("7")) {//入场时间错误
                    view.doCommonError("入场时间错误",5);
                }else {
                    view.doCommonError("无效票卡请重试",3);
                }
            } else {
                view.doCommonError("无效票卡请重试",3);
            }
        } else {
            view.doCommonError("无效票卡请重试",3);
        }
    }
}
