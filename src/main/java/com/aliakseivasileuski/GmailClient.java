package com.aliakseivasileuski;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.WatchRequest;
import com.google.api.services.gmail.model.WatchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class GmailClient {

    private static final String APPLICATION_NAME = "email-notification-listener";
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final List<String> SCOPES = Collections.singletonList(GmailScopes.MAIL_GOOGLE_COM);
    private static final String TIMELINE_FILTER = "after:%d before:%d";
    private static final String PKO_LABEL_ID = "Label_1511191873356474886";
    private static final String ME_USER = "me";

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final NetHttpTransport HTTP_TRANSPORT;

    static {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException(e);
        }
    }
    private static final Credential CREDENTIAL;

    static {
        try {
            CREDENTIAL = getCredential();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static final Gmail GMAIL = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, CREDENTIAL)
            .setApplicationName(APPLICATION_NAME)
            .build();

    private static Credential getCredential() throws IOException {
        try(InputStream in = GmailClient.class.getResourceAsStream("src/main/resources/credential.json")){
            if (in == null) {
                throw new RuntimeException();
            }
            GoogleClientSecrets clientSecrets =
                    GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

            // Build flow and trigger user authorization request.
            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    HTTP_TRANSPORT,
                    JSON_FACTORY,
                    clientSecrets,
                    SCOPES
            )
                    .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                    .setAccessType("offline")
                    .build();

            LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
            return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
        }
    }

    public WatchResponse sendWatchRequest(String topicName, List<String> desiredLabelIds) throws IOException {
        WatchRequest watchRequest = new WatchRequest()
                .setTopicName(topicName)
                .setLabelIds(desiredLabelIds);
        return GMAIL.users().watch(ME_USER, watchRequest).execute();
    }
}
