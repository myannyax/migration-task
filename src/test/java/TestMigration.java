import core.Migration;
import core.internal.NewStorageService;
import core.model.MigrationResult;
import core.utils.RequestHelper;
import okhttp3.ResponseBody;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import static java.io.File.separator;
import static org.junit.Assert.assertTrue;

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
        File example = new File(DIRECTORY + "example.txt");
        File file = new File(DIRECTORY + "copy_" + "example.txt");
        FileUtils.copyFile(example, file);
        Migration migration = new Migration();

        Field numberOfAttemptsField = Migration.class.getDeclaredField("numberOfAttempts");
        numberOfAttemptsField.setAccessible(true);
        HashMap<String, AtomicLong> numberOfAttempts = (HashMap<String, AtomicLong>) numberOfAttemptsField.get(migration);

        numberOfAttempts.put("copy_example.txt", new AtomicLong(0));

        Method uploadFile = Migration.class.getDeclaredMethod("uploadFile", File.class);
        uploadFile.setAccessible(true);
        uploadFile.invoke(migration, file);

        Field requestHelperField = Migration.class.getDeclaredField("requestHelper");
        requestHelperField.setAccessible(true);
        RequestHelper requestHelper = (RequestHelper) requestHelperField.get(migration);

        requestHelper.waitCalls();

        file.delete();

        Field serviceNewField = Migration.class.getDeclaredField("serviceNew");
        serviceNewField.setAccessible(true);
        NewStorageService serviceNew = (NewStorageService) serviceNewField.get(migration);


        Response<ResponseBody> resp = serviceNew.getFileContent(file.getName()).execute();
        while (!resp.isSuccessful()) {
            resp = serviceNew.getFileContent(file.getName()).execute();
        }
        ResponseBody body = resp.body();
        File result = new File(DIRECTORY + "new_" + "example.txt");
        try {
            assert body != null;
            try (InputStream input = body.byteStream(); OutputStream output = new FileOutputStream(result)) {
                input.transferTo(output);
            }
            assertTrue(FileUtils.contentEquals(example, result));
            result.delete();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testMigration() throws IOException {
        Migration migration = new Migration();

        MigrationResult migrationResult = migration.transferFiles();

        assertTrue(migrationResult.isSuccessful());
    }
}
