package com.avasileuski.helper;

import com.google.cloud.secretmanager.v1.AccessSecretVersionRequest;
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import com.google.cloud.secretmanager.v1.SecretVersionName;

public class SecretHelper {
    private static final String PROJECT_ID = "ivory-studio-471117-u6";

    public static String getSecret(String secretId) throws Exception {
        try (SecretManagerServiceClient client = SecretManagerServiceClient.create()) {
            SecretVersionName secretVersionName = SecretVersionName.of(PROJECT_ID, secretId, "latest");
            AccessSecretVersionRequest request = AccessSecretVersionRequest.newBuilder()
                    .setName(secretVersionName.toString())
                    .build();
            return client.accessSecretVersion(request).getPayload().getData().toStringUtf8();
        }
    }
}
