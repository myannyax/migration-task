import core.Migration;
import core.internal.NewStorageService;
import core.model.MigrationResult;
import core.utils.RequestHelper;
import okhttp3.ResponseBody;
import org.junit.Test;

import static java.io.File.separator;
import static org.junit.Assert.*;

import retrofit2.Response;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;

@SuppressWarnings("unchecked")
public class TestMigration {
    private final String DIRECTORY = "src/test/resources/".replace("/", separator);

    @Test
    public void testFileUpload() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException, IOException, InterruptedException {
        String filename = "example.txt";
        File example = new File(DIRECTORY + "example.txt");
        byte[] content = new FileInputStream(example).readAllBytes();

        Migration migration = new Migration();

        Field numberOfAttemptsField = Migration.class.getDeclaredField("numberOfAttempts");
        numberOfAttemptsField.setAccessible(true);
        HashMap<String, AtomicLong> numberOfAttempts = (HashMap<String, AtomicLong>) numberOfAttemptsField.get(migration);

        numberOfAttempts.put(filename, new AtomicLong(0));

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


        Response<ResponseBody> resp = serviceNew.getFileContent(filename).execute();
        while (!resp.isSuccessful()) {
            resp = serviceNew.getFileContent(filename).execute();
        }
        ResponseBody body = resp.body();

        assert body != null;
        assertArrayEquals(body.bytes(), content);
    }

    @Test
    public void testMigration() throws IOException {
        Migration migration = new Migration();

        MigrationResult migrationResult = migration.transferFiles();

        assertTrue(migrationResult.isSuccessful());
    }
}
