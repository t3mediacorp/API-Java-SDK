package trackvia.client;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class CommandOverHttpGet<T> extends OverHttpCommand <T> {
    private static Logger LOG = LoggerFactory.getLogger(CommandOverHttpGet.class);

    
    

    public CommandOverHttpGet(final HttpClientContext context, TrackviaClient client) {
       super(context, client);
    }

    

    @Override
    public HttpClientContext getContext() {
        return this.context;
    }

    @Override
    public T execute(CloseableHttpClient client) {
        final List<Integer> ValidResponseCodes = Arrays.asList(
                new Integer[]{HttpStatus.SC_OK});
        T result = null;
        CloseableHttpResponse response = null;

        try {
            URI uri = getApiRequestUri();
            HttpGet request = new HttpGet(uri);
            setHeaders(request);
            response = client.execute(request);
            
            result = handleResponse(client, request, ValidResponseCodes, response, uri, LOG);
            
        } catch (URISyntaxException | IOException e) {
            throw new TrackviaClientException(e);
        } finally {
            if (response != null) try { response.close(); } catch (IOException e) {}
        }

        return result;
    }
}
