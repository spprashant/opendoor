package com.home.pr.opendoor;

/**
 * Created by Pr on 28/10/2016.
 */
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;

/**
 * Created by Pr on 26/10/2016.
 */

// helper class to create adafruit REST api calls

public class AdafruitApiCallModel {
    public interface UpdateDoorStatus {
        @Headers({"Content-Type:application/json","x-aio-key:b6f77f8edcab47bc8aee969d3afcab33"})
        @POST("api/feeds/617396/data")
        Call<ResponseClass> getResponse(@Body DoorValue value);
    }
    public interface GetDoorStatus {
        @Headers({"Content-Type:application/json","x-aio-key:b6f77f8edcab47bc8aee969d3afcab33"})
        @GET("api/feeds/617396")
        Call<StatusClass> getResponse();
    }
}
