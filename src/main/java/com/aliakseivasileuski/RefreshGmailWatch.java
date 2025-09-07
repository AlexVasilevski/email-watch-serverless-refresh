package com.aliakseivasileuski;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.WatchRequest;
import com.google.api.services.gmail.model.WatchResponse;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.UserCredentials;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;


public class RefreshGmailWatch implements HttpFunction {

    private static final Logger logger = Logger.getLogger(RefreshGmailWatch.class.getName());
    private static final Firestore firestore = FirestoreOptions.getDefaultInstance().getService();
    private static final List<String> SCOPES = Collections.singletonList(GmailScopes.GMAIL_READONLY);
    private static final String ME_USER = "me";

    private static final Gmail GMAIL;
    private static final String TOPIC_NAME = "TOPIC_NAME";
    private static final String DESIRED_LABEL_ID = "DESIRED_LABEL_ID";
    private static final String GMAIL_CLIENT_ID = "GMAIL_CLIENT_ID";
    private static final String GMAIL_CLIENT_SECRET = "GMAIL_CLIENT_SECRET";
    private static final String GMAIL_REFRESH_TOKEN = "GMAIL_REFRESH_TOKEN";
    private static final String APPLICATION_NAME = "APPLICATION_NAME";

    private static final String FIRESTORE_COLLECTION_NAME = "FIRESTORE_COLLECTION_NAME";
    private static final String HISTORY_ID_FILED_NAME = "HISTORY_ID_FILED_NAME";

    static {
        try {
            GMAIL = buildGmailServiceOAuth();
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Gmail buildGmailServiceOAuth() throws GeneralSecurityException, IOException {
        String clientId = getEnvRequired(GMAIL_CLIENT_ID);
        String clientSecret = getEnvRequired(GMAIL_CLIENT_SECRET);
        String refreshToken = getEnvRequired(GMAIL_REFRESH_TOKEN);
        String applicationName = getEnvRequired(APPLICATION_NAME);

        GoogleCredentials userCredentials = UserCredentials.newBuilder()
                .setClientId(clientId)
                .setClientSecret(clientSecret)
                .setRefreshToken(refreshToken)
                .build()
                .createScoped(SCOPES);

        return new Gmail.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(userCredentials))
                .setApplicationName(applicationName)
                .build();
    }

    private static String getEnvRequired(String key) {
        String v = System.getenv(key);
        if (v == null || v.isBlank()) {
            throw new IllegalStateException("Missing required env var: " + key);
        }
        return v;
    }

    public WatchResponse sendWatchRequest(String topicName, List<String> desiredLabelIds) throws IOException {
        WatchRequest watchRequest = new WatchRequest()
                .setTopicName(topicName)
                .setLabelIds(desiredLabelIds);

        return GMAIL.users().watch(ME_USER, watchRequest).execute();
    }

    @Override
    public void service(HttpRequest httpRequest, HttpResponse httpResponse) throws Exception {

        String topicName = getEnvRequired(TOPIC_NAME);
        String desiredLabel = getEnvRequired(DESIRED_LABEL_ID);

        WatchResponse watchResponse = sendWatchRequest(topicName, List.of(desiredLabel));
        Long newHistoryId = watchResponse.getHistoryId().longValueExact();
        logger.info("Successfully sent watch request. New historyId: " + newHistoryId + "\n");

        String firestoreCollectionName = getEnvRequired(FIRESTORE_COLLECTION_NAME);
        DocumentReference documentReference = firestore.collection(firestoreCollectionName).document(ME_USER);

        String lastHistoryIdFieldName = getEnvRequired(HISTORY_ID_FILED_NAME);
        Map<String, Object> data = new HashMap<>();
        data.put(lastHistoryIdFieldName, newHistoryId);
        documentReference.set(data);

        logger.info("Document for " + firestoreCollectionName + " was successfully updated.\n");
    }
}