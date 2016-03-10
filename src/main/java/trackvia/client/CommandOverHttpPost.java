package trackvia.client;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class CommandOverHttpPost<T> extends OverHttpCommand<T> {
    private static Logger LOG = LoggerFactory.getLogger(CommandOverHttpPost.class);

   

    public CommandOverHttpPost(final HttpClientContext context, TrackviaClient client) {
        super(context, client);
     }

    public abstract HttpEntity getApiRequestEntity() throws UnsupportedEncodingException;

    @Override
    public HttpClientContext getContext() {
        return this.context;
    }

    @Override
    public T execute(CloseableHttpClient client) {
        final List<Integer> ValidResponseCodes = Arrays.asList(
                new Integer[] { HttpStatus.SC_OK, HttpStatus.SC_CREATED });
        T result = null;
        CloseableHttpResponse response = null;

        try {
            URI uri = getApiRequestUri();
            HttpPost request = new HttpPost(uri);
            setHeaders(request);
            request.setEntity(getApiRequestEntity());

            response = client.execute(request);
        	
            result = handleResponse(client, request, ValidResponseCodes, response, uri, LOG);
        } catch (URISyntaxException |IOException e) {
            throw new TrackviaClientException(e);
        } finally {
            if (response != null) try { response.close(); } catch (IOException e) {}
        }

        return result;
    }
}
