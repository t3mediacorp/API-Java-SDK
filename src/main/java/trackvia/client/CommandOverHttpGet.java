package trackvia.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.http.HttpEntity;
import trackvia.client.model.*;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

public abstract class CommandOverHttpGet<T> implements OverHttpCommand <T> {
    private static Logger LOG = LoggerFactory.getLogger(CommandOverHttpGet.class);

    private HttpClientContext context;
    private Gson gson;

    public CommandOverHttpGet(final HttpClientContext context) {
        this.context = context;
        this.gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX")
                .registerTypeAdapter(RecordData.class, new RecordDataDeserializer())
                .create();
    }

    public abstract URI getApiRequestUri() throws URISyntaxException;
    public abstract T processResponseEntity(final HttpEntity entity) throws IOException;

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
            response = client.execute(request);
            if (ValidResponseCodes.contains(response.getStatusLine().getStatusCode())) {
                result = processResponseEntity(response.getEntity());

                LOG.debug("{} api response: {}", uri.getPath(), (result == null) ? ("none") : (result.toString()));
            } else {
                Reader jsonReader = new InputStreamReader(response.getEntity().getContent());
                ApiErrorResponse apiError = gson.fromJson(jsonReader, ApiErrorResponse.class);

                LOG.debug("{} api error: {}", uri.getPath(), apiError.toString());

                throw new TrackviaApiException(apiError);
            }
        } catch (URISyntaxException | IOException e) {
            throw new TrackviaClientException(e);
        } finally {
            if (response != null) try { response.close(); } catch (IOException e) {}
        }

        return result;
    }
}
