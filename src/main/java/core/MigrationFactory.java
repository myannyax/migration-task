package core;

import core.internal.NewStorageService;
import core.internal.OldStorageService;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.converter.jaxb.JaxbConverterFactory;

public class MigrationFactory {

    public static Migration create() {
        Retrofit retrofit = new Retrofit.Builder().baseUrl("http://localhost:8080")
                .addConverterFactory(JaxbConverterFactory.create())
                .addConverterFactory(JacksonConverterFactory.create())
                .build();
        OldStorageService serviceOld = retrofit.create(OldStorageService.class);
        NewStorageService serviceNew = retrofit.create(NewStorageService.class);
        return new Migration(serviceOld, serviceNew);
    }
}
