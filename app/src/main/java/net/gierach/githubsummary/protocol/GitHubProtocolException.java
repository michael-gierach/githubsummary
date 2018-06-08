package net.gierach.githubsummary.protocol;

public class GitHubProtocolException extends Exception {
    public final int httpStatusCode;
    public final String httpStatusMessage;

    public GitHubProtocolException(int httpStatusCode, String httpStatusMessage) {
        this.httpStatusCode = httpStatusCode;
        this.httpStatusMessage = httpStatusMessage;
    }

    @Override
    public String getMessage() {
        return (Integer.toString(this.httpStatusCode) + " " + httpStatusMessage);
    }
}
