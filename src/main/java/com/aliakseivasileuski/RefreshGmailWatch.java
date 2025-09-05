package com.aliakseivasileuski;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.WatchRequest;
import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.logging.impl.Log4JLogger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import java.util.List;


public class RefreshGmailWatch implements HttpFunction {

    Logger logger = LogManager.getLogger(getClass());
    private static final String TOPIC_NAME = "projects/My First Project/topics/gmail-notifications";
    private static final List<String> DESIRED_LABELS_IDS = List.of("Label_1511191873356474886");

    private static final GmailClient gmailClient = new GmailClient();

    @Override
    public void service(HttpRequest httpRequest, HttpResponse httpResponse) throws Exception {
        logger.log(Level.INFO, "Starting scheduled gmail watch refresh...");
        gmailClient.sendWatchRequest(TOPIC_NAME, DESIRED_LABELS_IDS);
        logger.log(Level.INFO, "Scheduled gmail watch refresh is finished successfully.");
    }
}