package com.plasmit.diagnostic.quality.common.response;

import java.time.Instant;

public class ApiMeta {

    private String requestId;
    private String timestamp;

    public ApiMeta() {}

    public ApiMeta(String requestId) {
        this.requestId = requestId;
        this.timestamp = Instant.now().toString();
    }

    public String getRequestId() {
        return requestId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}