package trackvia.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import trackvia.client.model.ApiError;
import trackvia.client.model.ApiErrorResponse;
import trackvia.client.model.RecordData;
import trackvia.client.model.RecordDataDeserializer;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

public abstract class CommandOverHttpDelete<T> implements OverHttpCommand <T> {
    private static Logger LOG = LoggerFactory.getLogger(CommandOverHttpDelete.class);

    private HttpClientContext context;
    private Gson gson;

    public CommandOverHttpDelete(final HttpClientContext context) {
        this.context = context;
        this.gson = new GsonBuilder()
                .setDateFormat(getDateFormat())
                .registerTypeAdapter(RecordData.class, new RecordDataDeserializer())
                .create();
    }

    public abstract URI getApiRequestUri() throws URISyntaxException;

    public String getDateFormat() { return "yyyy-MM-dd'T'HH:mm:ss.SSSX"; }

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
            response = client.execute(request);

            int sc = response.getStatusLine().getStatusCode();
            if (!ValidResponseCodes.contains(sc)) {
                Reader jsonReader = new InputStreamReader(response.getEntity().getContent());
                ApiErrorResponse apiError = gson.fromJson(jsonReader, ApiErrorResponse.class);

                LOG.debug("{} api error: {}", uri.getPath(), apiError.toString());

                throw new TrackviaApiException(apiError);
            }
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
