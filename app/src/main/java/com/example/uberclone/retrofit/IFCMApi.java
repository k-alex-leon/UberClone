package com.example.uberclone.retrofit;

import com.example.uberclone.models.FCMBody;
import com.example.uberclone.models.FCMResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface IFCMApi {

    @Headers({
            "Content-Type:application/json",
            "Authorization:key=AAAA5eYyskE:APA91bGPT6YlwWwpiGBgqd1nMxt0_DUDQaMnIckf4C4uPvgDGs_VD4dBiFom8qJqC2JcnDRMfxyUtQrGge5Z1GPVIIytahpD6m6A5JyL_kFWtDdxYmy6jPoPOoiu4DOABilDDME3j7T1"
    })
    @POST("fcm/send")
    Call<FCMResponse> send(@Body FCMBody body);
}
