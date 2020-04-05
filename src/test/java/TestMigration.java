import core.Migration;
import core.MigrationFactory;
import core.internal.NewStorageService;
import core.internal.OldStorageService;
import core.model.MigrationResult;
import core.utils.RequestHelper;
import kotlin.text.Regex;
import okhttp3.*;
import org.junit.Test;

import static java.io.File.separator;
import static org.junit.Assert.*;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedDeque;

// testFileUpload и testMigration требуют, чтобы сервер был запущен
public class TestMigration {
    private final String DIRECTORY = "src/test/resources/".replace("/", separator);

    @Test
    public void testFileUpload() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException, IOException, InterruptedException {
        String filename = "example.txt";
        File example = new File(DIRECTORY + "example.txt");
        byte[] content = new FileInputStream(example).readAllBytes();

        Migration migration = MigrationFactory.create();

        Method uploadFile = Migration.class.getDeclaredMethod("uploadFile", byte[].class, String.class);
        uploadFile.setAccessible(true);
        uploadFile.invoke(migration, content, filename);

        Field requestHelperField = Migration.class.getDeclaredField("requestHelper");
        requestHelperField.setAccessible(true);
        RequestHelper requestHelper = (RequestHelper) requestHelperField.get(migration);

        requestHelper.waitCalls();

        Field serviceNewField = Migration.class.getDeclaredField("serviceNew");
        serviceNewField.setAccessible(true);
        NewStorageService serviceNew = (NewStorageService) serviceNewField.get(migration);


        Call<ResponseBody> call = serviceNew.getFileContent(filename);
        Response<ResponseBody> response = RequestHelper.executeUntilSuccess(call);
        ResponseBody body = response.body();

        assert body != null;
        assertArrayEquals(body.bytes(), content);
    }

    @Test
    public void testWithMock() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, IOException {
        Constructor<Migration> constructor = Migration.class.getDeclaredConstructor(OldStorageService.class, NewStorageService.class);
        constructor.setAccessible(true);
        Migration migration = constructor.newInstance(new OldServerMock(), new NewServiceMock());

        MigrationResult migrationResult = migration.transferFiles();

        assertTrue(migrationResult.isSuccessful());
    }

    @Test
    public void testMigration() throws IOException {
        Migration migration = MigrationFactory.create();

        MigrationResult migrationResult = migration.transferFiles();

        assertTrue(migrationResult.isSuccessful());
    }

    private static class NewServiceMock implements NewStorageService {

        private ConcurrentLinkedDeque<String> files = new ConcurrentLinkedDeque<>();

        @Override
        public Call<ArrayList<String>> getFiles() {
            return new Call<ArrayList<String>>() {

                private boolean executed = false;
                private boolean canceled = false;

                @Override
                public Response<ArrayList<String>> execute() throws IOException {
                    executed = true;
                    return Response.success(new ArrayList<>(files));
                }

                @Override
                public void enqueue(Callback<ArrayList<String>> callback) {
                    if (executed) return;
                    executed = true;
                    callback.onResponse(this, Response.success(new ArrayList<>(files)));
                }

                @Override
                public boolean isExecuted() {
                    return executed;
                }

                @Override
                public void cancel() {
                    canceled = true;
                }

                @Override
                public boolean isCanceled() {
                    return canceled;
                }

                @Override
                public Call<ArrayList<String>> clone() {
                    return null;
                }

                @Override
                public Request request() {
                    return null;
                }
            };
        }

        @Override
        public Call<ResponseBody> getFileContent(String filename) {
            return new Call<ResponseBody>() {

                private boolean executed = false;
                private boolean canceled = false;

                @Override
                public Response<ResponseBody> execute() throws IOException {
                    if (executed) throw new IOException();
                    executed = true;
                    return Response.success(ResponseBody.create(filename.getBytes(), MediaType.parse("application/octet-stream")));
                }

                @Override
                public void enqueue(Callback<ResponseBody> callback) {
                    if (executed) return;
                    executed = true;
                    callback.onResponse(this, Response.success(ResponseBody.create(filename.getBytes(), MediaType.parse("application/octet-stream"))));
                }

                @Override
                public boolean isExecuted() {
                    return executed;
                }

                @Override
                public void cancel() {
                    canceled = true;
                }

                @Override
                public boolean isCanceled() {
                    return canceled;
                }

                @Override
                public Call<ResponseBody> clone() {
                    return null;
                }

                @Override
                public Request request() {
                    return null;
                }
            };
        }

        @Override
        public Call<ResponseBody> deleteFile(String filename) {
            return new Call<ResponseBody>() {

                private boolean executed = false;
                private boolean canceled = false;

                @Override
                public Response<ResponseBody> execute() throws IOException {
                    if (executed) throw new IOException();
                    executed = true;
                    files.remove(filename);
                    return Response.success(ResponseBody.create("", MediaType.parse("application/octet-stream")));
                }

                @Override
                public void enqueue(Callback<ResponseBody> callback) {
                    if (executed) return;
                    executed = true;

                    files.remove(filename);

                    callback.onResponse(this, Response.success(ResponseBody.create("", MediaType.parse("application/octet-stream"))));
                }

                @Override
                public boolean isExecuted() {
                    return executed;
                }

                @Override
                public void cancel() {
                    canceled = true;
                }

                @Override
                public boolean isCanceled() {
                    return canceled;
                }

                @Override
                public Call<ResponseBody> clone() {
                    return null;
                }

                @Override
                public Request request() {
                    return null;
                }
            };
        }

        @Override
        public Call<ResponseBody> uploadFile(MultipartBody.Part file) {
            return new Call<ResponseBody>() {

                private boolean executed = false;
                private boolean canceled = false;

                @Override
                public Response<ResponseBody> execute() throws IOException {
                    if (executed) throw new IOException();
                    executed = true;
                    String filename = file.headers().get("filename");
                    files.add(filename);

                    return Response.success(ResponseBody.create(filename.getBytes(), MediaType.parse("application/octet-stream")));
                }

                @Override
                public void enqueue(Callback<ResponseBody> callback) {
                    if (executed) return;
                    executed = true;
                    Regex regexp = new Regex(".*filename=\"(.*)\"");
                    String headers = file.headers().get("Content-Disposition");
                    String filename = regexp.matchEntire(headers).getDestructured().toList().get(0);
                    files.add(filename);

                    callback.onResponse(this, Response.success(ResponseBody.create(filename.getBytes(), MediaType.parse("application/octet-stream"))));
                }

                @Override
                public boolean isExecuted() {
                    return executed;
                }

                @Override
                public void cancel() {
                    canceled = true;
                }

                @Override
                public boolean isCanceled() {
                    return canceled;
                }

                @Override
                public Call<ResponseBody> clone() {
                    return null;
                }

                @Override
                public Request request() {
                    return null;
                }
            };
        }
    }

    private static class OldServerMock implements OldStorageService {

        private ConcurrentLinkedDeque<String> files = new ConcurrentLinkedDeque<>();

        OldServerMock() {
            files.add("1.txt");
            files.add("2.txt");
            files.add("3.txt");
        }

        @Override
        public Call<ArrayList<String>> getFiles() {
            return new Call<ArrayList<String>>() {

                private boolean executed = false;
                private boolean canceled = false;

                @Override
                public Response<ArrayList<String>> execute() throws IOException {
                    executed = true;
                    return Response.success(new ArrayList<>(files));
                }

                @Override
                public void enqueue(Callback<ArrayList<String>> callback) {
                    if (executed) return;
                    executed = true;
                    callback.onResponse(this, Response.success(new ArrayList<>(files)));
                }

                @Override
                public boolean isExecuted() {
                    return executed;
                }

                @Override
                public void cancel() {
                    canceled = true;
                }

                @Override
                public boolean isCanceled() {
                    return canceled;
                }

                @Override
                public Call<ArrayList<String>> clone() {
                    return null;
                }

                @Override
                public Request request() {
                    return null;
                }
            };
        }

        @Override
        public Call<ResponseBody> getFileContent(String filename) {
            return new Call<ResponseBody>() {

                private boolean executed = false;
                private boolean canceled = false;

                @Override
                public Response<ResponseBody> execute() throws IOException {
                    if (executed) throw new IOException();
                    executed = true;
                    return Response.success(ResponseBody.create(filename.getBytes(), MediaType.parse("application/octet-stream")));
                }

                @Override
                public void enqueue(Callback<ResponseBody> callback) {
                    if (executed) return;
                    executed = true;
                    callback.onResponse(this, Response.success(ResponseBody.create(filename.getBytes(), MediaType.parse("application/octet-stream"))));
                }

                @Override
                public boolean isExecuted() {
                    return executed;
                }

                @Override
                public void cancel() {
                    canceled = true;
                }

                @Override
                public boolean isCanceled() {
                    return canceled;
                }

                @Override
                public Call<ResponseBody> clone() {
                    return null;
                }

                @Override
                public Request request() {
                    return null;
                }
            };
        }

        @Override
        public Call<ResponseBody> deleteFile(String filename) {
            return new Call<ResponseBody>() {

                private boolean executed = false;
                private boolean canceled = false;

                @Override
                public Response<ResponseBody> execute() throws IOException {
                    if (executed) throw new IOException();
                    executed = true;
                    files.remove(filename);
                    return Response.success(ResponseBody.create("", MediaType.parse("application/octet-stream")));
                }

                @Override
                public void enqueue(Callback<ResponseBody> callback) {
                    if (executed) return;
                    executed = true;

                    files.remove(filename);

                    callback.onResponse(this, Response.success(ResponseBody.create("", MediaType.parse("application/octet-stream"))));
                }

                @Override
                public boolean isExecuted() {
                    return executed;
                }

                @Override
                public void cancel() {
                    canceled = true;
                }

                @Override
                public boolean isCanceled() {
                    return canceled;
                }

                @Override
                public Call<ResponseBody> clone() {
                    return null;
                }

                @Override
                public Request request() {
                    return null;
                }
            };
        }
    }
}
