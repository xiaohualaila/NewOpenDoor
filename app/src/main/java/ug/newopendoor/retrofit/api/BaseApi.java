package ug.newopendoor.retrofit.api;

import org.json.JSONObject;

import java.util.List;

import okhttp3.MultipartBody;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Query;
import rx.Observable;

/**
 * Created by xyuxiao on 2016/9/23.
 */
public interface BaseApi {

    @POST("Api.php")
    Observable<JSONObject> uploadPhotoBase(
            @Query("projectId") int projectId,//项目ID
            @Query("chipId") String ticketid,//芯片号码
            @Query("qrCodeId") String qrCodeId,// 二维码号
            @Query("doorType") String doorType
    );


}

