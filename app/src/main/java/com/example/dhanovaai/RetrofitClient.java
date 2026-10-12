package com.example.dhanovaai;

import android.content.Context;
import android.content.SharedPreferences;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static final String BASE_URL = "http://10.106.236.125:5000/"; // keep this matching your current ipconfig

    private static Retrofit retrofit;

    public static ApiService getApiService(Context context) {
        if (retrofit == null) {
            Context appContext = context.getApplicationContext();

            Interceptor authInterceptor = chain -> {
                Request original = chain.request();
                SharedPreferences prefs = appContext.getSharedPreferences("dhanova_prefs", Context.MODE_PRIVATE);
                String token = prefs.getString("auth_token", null);

                Request.Builder builder = original.newBuilder();
                if (token != null) {
                    builder.header("Authorization", "Bearer " + token);
                }
                return chain.proceed(builder.build());
            };

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(authInterceptor)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(ApiService.class);
    }
}