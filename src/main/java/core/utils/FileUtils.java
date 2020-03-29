package core.utils;

import okhttp3.ResponseBody;

import java.io.*;

public class FileUtils {

    public static void writeToFile(ResponseBody body, File file) throws IOException {
        try (InputStream input = body.byteStream(); OutputStream output = new FileOutputStream(file)) {
            input.transferTo(output);
        }
    }
}
