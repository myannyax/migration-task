package core.internal;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Path;

import java.util.ArrayList;

public interface OldStorageService {
    @GET("/oldStorage/files")
    Call<ArrayList<String>> getFiles();

    @GET("/oldStorage/files/{filename}")
    Call<ResponseBody> getFileContent(@Path("filename") String filename);

    @DELETE("/oldStorage/files/{filename}")
    Call<ResponseBody> deleteFile(@Path("filename") String filename);
}
