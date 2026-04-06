// Author: Yunqi Dong, Andrew ID: yunqid
package ds.web;

/**
 * Result of calling the Arbeitnow Job Board API (HTTP status, timing, raw JSON body).
 */
public final class ArbeitnowFetchResult {

    private final int httpStatus;
    private final long latencyMs;
    private final String responseBody;
    private final String errorMessage;

    public ArbeitnowFetchResult(int httpStatus, long latencyMs, String responseBody, String errorMessage) {
        this.httpStatus = httpStatus;
        this.latencyMs = latencyMs;
        this.responseBody = responseBody;
        this.errorMessage = errorMessage;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public long getLatencyMs() {
        return latencyMs;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public boolean isSuccess() {
        return httpStatus == 200 && responseBody != null;
    }
}
