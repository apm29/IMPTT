package com.imptt.apm29.api;



import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.FormBody;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.internal.platform.Platform;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitManager {

    private static volatile RetrofitManager instance;

    public static RetrofitManager getInstance() {
        if (instance == null) {
            synchronized (RetrofitManager.class) {
                if (instance == null) {
                    instance = new RetrofitManager();
                }
            }
        }
        return instance;
    }

    private Retrofit retrofit;

    private RetrofitManager() {
        Retrofit.Builder builder = new Retrofit.Builder();
        builder.addCallAdapterFactory(RxJava2CallAdapterFactory.create());
        builder.addConverterFactory(GsonConverterFactory.create());
        builder.baseUrl("http://axj.ciih.net");
        builder.client(
                new OkHttpClient.Builder()
                        .connectTimeout(125, TimeUnit.SECONDS)
                        .readTimeout(125, TimeUnit.SECONDS)
                        .callTimeout(125, TimeUnit.SECONDS)
                        .writeTimeout(125, TimeUnit.SECONDS)
                        .build()
        );
        retrofit = builder
                .build();
    }

    public Retrofit getRetrofit() {
        return retrofit;
    }
}
