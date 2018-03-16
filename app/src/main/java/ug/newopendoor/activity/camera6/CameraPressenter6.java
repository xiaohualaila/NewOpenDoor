package ug.newopendoor.activity.camera6;

import android.text.TextUtils;
import android.util.Log;

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

public class CameraPressenter6 extends BasePresenter implements CameraContract6.Presenter {
    private CameraContract6.View view;

    public CameraPressenter6(CameraContract6.View view) {
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
                                   view.requestFail();
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
                    view.doNumError();
                } else if (result.equals("3")) {//入场口错误
                    view.doDoorError();
                } else if (result.equals("5")) {
                    view.doFaceError();
                } else if (result.equals("7")) {//入场时间错误
                    view.doTimeError();
                }else {
                    view.doError();
                }
            } else {
                view.doError();
            }
        } else {
            view.doError();
        }
    }
}
