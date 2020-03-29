package core.utils;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

public class RequestHelper {
    private ConcurrentLinkedQueue<Call> calls = new ConcurrentLinkedQueue<>();

    private AtomicLong finished = new AtomicLong(0);

    public void addCall(Call<ResponseBody> call, Callback<ResponseBody> callback) {
        calls.add(call);
        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                callback.onResponse(call, response);
                finished.incrementAndGet();
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                callback.onFailure(call, t);
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
}
