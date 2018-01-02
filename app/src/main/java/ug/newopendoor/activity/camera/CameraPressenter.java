package ug.newopendoor.activity.camera;

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
import ug.newopendoor.bean.WhiteList;
import ug.newopendoor.retrofit.Api;
import ug.newopendoor.retrofit.ConnectUrl;
import ug.newopendoor.util.GetDataUtil;


/**
 * Created by Administrator on 2017/6/3.
 */

public class CameraPressenter extends BasePresenter implements CameraContract.Presenter{
    private CameraContract.View view;

    public CameraPressenter(CameraContract.View view) {
        this.view = view;
        this.view.setPresenter(this);
    }

    @Override
    public void start() {

    }

    @Override
    public void load(boolean isNetAble, String device_id, int type, String ticketNum, File newFile) {
        if (isNetAble) {
                MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);
                RequestBody requestBody = RequestBody.create(MediaType.parse("multipart/form-data"), newFile);
                builder.addFormDataPart("photoImgFiles", newFile.getName(), requestBody);
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
                                   Log.i("sss",throwable.toString());
                                   view.doError();
                               }
                           }
                        );
        } else {//不联网
            String sStr = ticketNum.toUpperCase().trim();
            WhiteList whiteList =  GetDataUtil.getDataBooean(sStr);
            if(whiteList != null){
                view.doSuccess("");
            }else {
                view.doError();
            }
        }

    }

    private void jsonObjectResult(JSONObject jsonObject){
       // Log.i("xxxx",jsonObject.toString());
        if(jsonObject !=null){
            String result = jsonObject.optString("Result");
            if(!TextUtils.isEmpty(result)){
                if(result.equals("1")){
                       String imageStr = jsonObject.optString("Face_path");
                    view.doSuccess(imageStr);
                }else{
                    view.doError();
                }
            }else {
                view.doError();
            }
        }else {
            view.doError();
        }
    }
}
