package trackvia.client;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class CommandOverHttpDelete<T> extends OverHttpCommand <T> {
    private static Logger LOG = LoggerFactory.getLogger(CommandOverHttpDelete.class);


    public CommandOverHttpDelete(final HttpClientContext context, TrackviaClient client) {
        super(context, client);
     }

    @Override
    public HttpClientContext getContext() {
        return this.context;
    }

    @Override
    public T execute(CloseableHttpClient client) {
        final List<Integer> ValidResponseCodes = Arrays.asList(
                new Integer[]{HttpStatus.SC_OK, HttpStatus.SC_ACCEPTED, HttpStatus.SC_NO_CONTENT});
        T result = null;
        CloseableHttpResponse response = null;

        try {
            URI uri = getApiRequestUri();
            HttpDelete request = new HttpDelete(uri);
            setHeaders(request);
            response = client.execute(request);

            result = handleResponse(client, request, ValidResponseCodes, response, uri, LOG);
        } catch (URISyntaxException |IOException e) {
            throw new TrackviaClientException(e);
        } finally {
            if (response != null) try { response.close(); } catch (IOException e) {}
        }

        return result;
    }

    String getMessage(int statusCode) {
        switch (statusCode) {
            case HttpStatus.SC_UNAUTHORIZED:
                return "Deletion of this resource is unauthorized.";
        }

        return "unknown";
    }
}
