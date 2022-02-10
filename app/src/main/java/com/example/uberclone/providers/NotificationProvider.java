package com.example.uberclone.providers;

import com.example.uberclone.models.FCMBody;
import com.example.uberclone.models.FCMResponse;
import com.example.uberclone.retrofit.IFCMApi;
import com.example.uberclone.retrofit.RetrofitClient;

import retrofit2.Call;

public class NotificationProvider {

    private String url = "https://fcm.googleapis.com";

    public NotificationProvider(){}

    public Call<FCMResponse> sendNotification(FCMBody body){
        return RetrofitClient.getClientObject(url).create(IFCMApi.class).send(body);
    }
}
