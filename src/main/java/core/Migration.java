package core;

import core.internal.NewStorageService;
import core.internal.OldStorageService;
import core.model.MigrationResult;
import core.utils.RequestHelper;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import org.apache.commons.io.FileUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.converter.jaxb.JaxbConverterFactory;

import javax.activation.MimetypesFileTypeMap;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

import static core.utils.FileUtils.writeToFile;

public class Migration {

    private static final int MAX_NUMBER_OF_ATTEMPTS = 30;

    private static final String DIRECTORY = "tmp";

    private OldStorageService serviceOld;

    private NewStorageService serviceNew;

    private ConcurrentLinkedQueue<String> copied = new ConcurrentLinkedQueue<>();

    private ConcurrentLinkedQueue<String> deleted = new ConcurrentLinkedQueue<>();

    private HashMap<String, AtomicLong> numberOfAttempts = new HashMap<>();

    private MigrationResult migrationResult;

    public MigrationResult getMigrationResult() {
        return migrationResult;
    }

    private RequestHelper requestHelper = new RequestHelper();

    public Migration() {
        Retrofit retrofit = new Retrofit.Builder().baseUrl("http://localhost:8080")
                .addConverterFactory(JaxbConverterFactory.create())
                .addConverterFactory(JacksonConverterFactory.create())
                .build();
        serviceOld = retrofit.create(OldStorageService.class);
        serviceNew = retrofit.create(NewStorageService.class);
    }

    public void transferFiles() throws IOException {
        ArrayList<String> files = getFilesFromOldServer();
        File dir = new File(DIRECTORY);
        if (!dir.mkdir()) {
            throw new IOException();
        }
        assert files != null;
        for (String filename : files) {
            numberOfAttempts.put(filename, new AtomicLong(0));
            transferFile(filename);
        }
        try {
            requestHelper.waitCalls();
        } catch (InterruptedException e) {
            System.err.println(e.getMessage());
        }

        ArrayList<String> finalOldServerState = getFilesFromOldServer();
        ArrayList<String> newServerState = getFilesFromNewServer();
        migrationResult = new MigrationResult(files, finalOldServerState, newServerState, new ArrayList<>(copied), new ArrayList<>(deleted));
        FileUtils.deleteDirectory(dir);
    }

    private void transferFile(String filename) {
        Call<ResponseBody> call = serviceOld.getFileContent(filename);

        numberOfAttempts.get(filename).incrementAndGet();
        requestHelper.addCall(call, new Callback<>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    File file = new File(DIRECTORY + File.separator + filename);
                    try {
                        assert response.body() != null;
                        writeToFile(response.body(), file);
                    } catch (IOException e) {
                        System.err.println(e.getMessage());
                        return;
                    }
                    uploadFile(file);
                } else {
                    if (numberOfAttempts.get(filename).get() < MAX_NUMBER_OF_ATTEMPTS) {
                        transferFile(filename);
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                if (numberOfAttempts.get(filename).get() < MAX_NUMBER_OF_ATTEMPTS) {
                    transferFile(filename);
                }
            }
        });
    }

    private void uploadFile(File file) {
        RequestBody requestFile = RequestBody.create(file, MediaType.parse(MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(file)));
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);

        Call<ResponseBody> uploadCall = serviceNew.uploadFile(body);
        numberOfAttempts.get(file.getName()).incrementAndGet();

        requestHelper.addCall(uploadCall, new Callback<>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (!response.isSuccessful()) {
                    if (numberOfAttempts.get(file.getName()).get() < MAX_NUMBER_OF_ATTEMPTS && !response.message().equals("already exists")) {
                        uploadFile(file);
                    }
                } else {
                    copied.add(file.getName());
                    //noinspection ResultOfMethodCallIgnored
                    file.delete();
                    deleteFile(file.getName());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                if (numberOfAttempts.get(file.getName()).get() < MAX_NUMBER_OF_ATTEMPTS) {
                    uploadFile(file);
                }
            }
        });
    }

    private void deleteFile(String filename) {
        Call<ResponseBody> deleteCall = serviceOld.deleteFile(filename);
        numberOfAttempts.get(filename).incrementAndGet();

        requestHelper.addCall(deleteCall, new Callback<>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (!response.isSuccessful()) {
                    if (numberOfAttempts.get(filename).get() < MAX_NUMBER_OF_ATTEMPTS) {
                        deleteFile(filename);
                    }
                } else {
                    deleted.add(filename);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                if (numberOfAttempts.get(filename).get() < MAX_NUMBER_OF_ATTEMPTS) {
                    deleteFile(filename);
                }
            }
        });
    }

    private ArrayList<String> getFilesFromNewServer() throws IOException {
        Response<ArrayList<String>> response = serviceNew.getFiles().execute();
        while (!response.isSuccessful()) {
            response = serviceNew.getFiles().execute();
        }
        return response.body();
    }

    private ArrayList<String> getFilesFromOldServer() throws IOException {
        Response<ArrayList<String>> response = serviceOld.getFiles().execute();
        while (!response.isSuccessful()) {
            response = serviceOld.getFiles().execute();
        }
        return response.body();
    }
}
