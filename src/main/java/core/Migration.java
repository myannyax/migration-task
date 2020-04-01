package core;

import core.internal.NewStorageService;
import core.internal.OldStorageService;
import core.model.MigrationResult;
import core.utils.RequestHelper;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.converter.jaxb.JaxbConverterFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Migration {

    private OldStorageService serviceOld;

    private NewStorageService serviceNew;

    private ConcurrentLinkedQueue<String> copied = new ConcurrentLinkedQueue<>();

    private ConcurrentLinkedQueue<String> deleted = new ConcurrentLinkedQueue<>();

    private RequestHelper requestHelper = new RequestHelper();

    public Migration() {
        Retrofit retrofit = new Retrofit.Builder().baseUrl("http://localhost:8080")
                .addConverterFactory(JaxbConverterFactory.create())
                .addConverterFactory(JacksonConverterFactory.create())
                .build();
        serviceOld = retrofit.create(OldStorageService.class);
        serviceNew = retrofit.create(NewStorageService.class);
    }

    public MigrationResult transferFiles() throws IOException {
        ArrayList<String> files = getFilesFromOldServer();

        assert files != null;
        for (String filename : files) {
            transferFile(filename);
        }
        try {
            requestHelper.waitCalls();
        } catch (InterruptedException e) {
            System.err.println(e.getMessage());
        }

        ArrayList<String> finalOldServerState = getFilesFromOldServer();
        ArrayList<String> newServerState = getFilesFromNewServer();

        boolean isSuccessful = files.size() == newServerState.size()
                && finalOldServerState.size() == 0
                && copied.size() == deleted.size();

        return new MigrationResult(new ArrayList<>(copied), new ArrayList<>(deleted), finalOldServerState, isSuccessful);
    }

    private void transferFile(String filename) {
        Call<ResponseBody> call = serviceOld.getFileContent(filename);
        requestHelper.enqueue(call, (response) -> {
            try {
                assert response.body() != null;
                uploadFile(response.body().bytes(), filename);
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        });
    }

    private void uploadFile(byte[] content, String filename) {
        RequestBody requestBody = RequestBody.create(content, MediaType.parse("application/octet-stream"));
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", filename, requestBody);

        Call<ResponseBody> uploadCall = serviceNew.uploadFile(body);

        requestHelper.enqueue(uploadCall, (response) -> {
            copied.add(filename);
            deleteFile(filename);
        });
    }

    private void deleteFile(String filename) {
        Call<ResponseBody> deleteCall = serviceOld.deleteFile(filename);

        requestHelper.enqueue(deleteCall, (response) -> deleted.add(filename));
    }

    private ArrayList<String> getFilesFromNewServer() throws IOException {
        Call<ArrayList<String>> call = serviceNew.getFiles();
        return RequestHelper.executeUntilSuccess(call).body();
    }

    private ArrayList<String> getFilesFromOldServer() throws IOException {
        Call<ArrayList<String>> call = serviceOld.getFiles();
        return RequestHelper.executeUntilSuccess(call).body();
    }
}
