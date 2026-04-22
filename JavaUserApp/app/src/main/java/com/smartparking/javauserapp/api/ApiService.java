package com.smartparking.javauserapp.api;

import java.util.Map;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiService {
    @POST("api/login")
    Call<Map<String, Object>> login(@Body Map<String, String> body);

    @POST("api/register")
    Call<Map<String, Object>> register(@Body Map<String, String> body);

    @GET("api/user/info")
    Call<Map<String, Object>> getUserInfo(@Query("username") String username);

    @POST("api/user/topup")
    Call<Map<String, Object>> topup(@Body Map<String, Object> body);

    @GET("api/user/history")
    Call<Map<String, Object>> getUserHistory(@Query("username") String username);

    @POST("api/user/link_plate")
    Call<Map<String, Object>> linkPlate(@Body Map<String, String> body);

    @POST("api/user/scan_qr")
    Call<Map<String, Object>> scanQr(@Body Map<String, Object> body);

    @GET("api/user/active_sessions")
    Call<Map<String, Object>> getActiveSessions(@Query("username") String username);

    @GET("api/parking/status")
    Call<Map<String, Object>> getParkingStatus();
}
