package ug.newopendoor.activity.camera8;

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

public class CameraPresenter8 extends BasePresenter implements CameraContract8.Presenter {
    private CameraContract8.View view;

    public CameraPresenter8(CameraContract8.View view) {
        this.view = view;
        this.view.setPresenter(this);
    }

    @Override
    public void start() {

    }

    @Override
    public void load(int projectId,String ticketType,String ticketId, File newFile) {
        MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);
        RequestBody requestBody = RequestBody.create(MediaType.parse("multipart/form-data"), newFile);
        builder.addFormDataPart("photoImgFiles", newFile.getName(), requestBody);
        Api.getBaseApiWithOutFormat(ConnectUrl.URL)
                .uploadPhotoBase(projectId,ticketType,ticketId, builder.build().parts())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<JSONObject>() {
                               @Override
                               public void call(JSONObject jsonObject) {
                                Log.i("sss",jsonObject.toString());
                                   jsonObjectResult(jsonObject);
                               }
                           }, new Action1<Throwable>() {
                               @Override
                               public void call(Throwable throwable) {
                                   Log.i("sss",throwable.toString());
                                   view.doCommonError("网络异常",6,"");
                               }
                           }
                );
    }

    private void jsonObjectResult(JSONObject jsonObject) {
        if (jsonObject != null) {
            String result = jsonObject.optString("Result");
            String imageStr = jsonObject.optString("Face_path");

            if (!TextUtils.isEmpty(result)) {
                if (result.equals("1")) {
                    view.doSuccess(imageStr);
                } else if (result.equals("5")) {
                    view.doCommonError("人脸验证失败",1,imageStr);
                }  else if (result.equals("6")) {
                    view.doCommonError("人脸检测失败",1,imageStr);
                } else if(result.equals("7")){
                    view.doCommonError("入场时间错误",5,imageStr);
                }  else if(result.equals("2")){
                    view.doCommonError("不可重复入场",2,imageStr);
                } else if(result.equals("3")){
                    view.doCommonError("入场口错误",8,imageStr);
                }else if(result.equals("4")){
                    view.doCommonError("入场次数频繁",7,imageStr);
                }else {
                    view.doCommonError("无效票卡请重试",3,imageStr);
                }
            } else {
                view.doCommonError("无效票卡请重试",3,imageStr);
            }
        } else {
            view.doCommonError("无效票卡请重试",3,"");
        }
    }
}
