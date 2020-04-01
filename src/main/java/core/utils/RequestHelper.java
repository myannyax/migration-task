package core.utils;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

public class RequestHelper {

    private ConcurrentLinkedQueue<Call> calls = new ConcurrentLinkedQueue<>();

    private AtomicLong finished = new AtomicLong(0);

    private static final int MAX_NUMBER_OF_ATTEMPTS = 30;

    public void enqueue(Call<ResponseBody> call, Callback<ResponseBody> callback) {
        enqueue(call, callback, MAX_NUMBER_OF_ATTEMPTS);
    }

    private void enqueue(Call<ResponseBody> call, Callback<ResponseBody> callback, int attempt) {
        calls.add(call);
        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    callback.onResponse(call, response);
                } else {
                    if (attempt > 1) {
                        enqueue(call.clone(), callback, attempt - 1);
                    }
                }
                finished.incrementAndGet();
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                if (attempt > 1) {
                    enqueue(call.clone(), callback, attempt - 1);
                }
                finished.incrementAndGet();
            }
        });
    }

    public void waitCalls() throws InterruptedException {
        while (finished.get() != calls.size()) {
            if (Thread.interrupted()) {
                for (Call call : calls) {
                    call.cancel();
                }
                throw new InterruptedException();
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException ie) {
                for (Call call : calls) {
                    call.cancel();
                }
                throw ie;
            }
        }

        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
    }

    public static <T> Response<T> executeUntilSuccess(Call<T> call) throws IOException {
        return executeUntilSuccess(call, MAX_NUMBER_OF_ATTEMPTS);
    }

    private static <T> Response<T> executeUntilSuccess(Call<T> call, int bound) throws IOException {
        try {
            Response<T> response = call.execute();
            if (bound == 1 || response.isSuccessful()) {
                return response;
            } else {
                return executeUntilSuccess(call.clone(), bound - 1);
            }
        } catch (IOException e) {
            if (bound > 1) return executeUntilSuccess(call.clone(), bound - 1);
            else throw e;
        }
    }
}
