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

    //上传照片
    @POST("face.php")
    @Multipart
    Observable<JSONObject> uploadPhoto(
            @Part List<MultipartBody.Part> file
    );

    @POST("Api.php")
    @Multipart
    Observable<JSONObject> uploadPhotoBase(
            @Query("deviceid") String deviceid,
            @Query("chipId") String ticketid,//芯片
            @Query("qrCodeId") String xinCode,//二维码
            @Query("type") int type,
            @Part List<MultipartBody.Part> file
    );

    @POST("Api.php")
    @Multipart
    Observable<JSONObject> uploadPhotoBase(
            @Query("deviceid") String deviceid,
            @Query("ticketid") String ticketid,
            @Query("type") int type,
            @Part List<MultipartBody.Part> file
    );


}

