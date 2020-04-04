package core.internal;

import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.ArrayList;

public interface NewStorageService {
    @GET("/newStorage/files")
    Call<ArrayList<String>> getFiles();

    @GET("/newStorage/files/{filename}")
    Call<ResponseBody> getFileContent(@Path("filename") String filename);

    @DELETE("/newStorage/files/{filename}")
    Call<ResponseBody> deleteFile(@Path("filename") String filename);

    @Multipart
    @POST("/newStorage/files")
    Call<ResponseBody> uploadFile(@Part MultipartBody.Part file);
}
