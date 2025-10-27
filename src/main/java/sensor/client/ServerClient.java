package sensor.client;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.*;
import sensor.Sensor;
import sensor.dto.ReadingDto;

import java.util.Map;

public interface ServerClient {

    @POST("/api/sensors/register")
    Call<Map<String, Object>> registerSensor(@Body Map<String, Object> sensorData);

    @GET("/api/sensors/{id}/nearest")
    Call<Sensor> getNearest(@Path("id") long id);

    @POST("/api/sensors/{id}/readings")
    Call<Void> sendReading(@Path("id") long id, @Body ReadingDto reading);

    static ServerClient create(String baseUrl) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        return retrofit.create(ServerClient.class);
    }
}