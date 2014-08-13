package trackvia.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import trackvia.client.model.ApiErrorResponse;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import trackvia.client.model.RecordData;
import trackvia.client.model.RecordDataDeserializer;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

public abstract class CommandOverHttpPost<T> implements OverHttpCommand<T> {
    private static Logger LOG = LoggerFactory.getLogger(CommandOverHttpPost.class);

    private HttpClientContext context;
    private Gson gson;

    public CommandOverHttpPost(final HttpClientContext context) {
        this.context = context;
        this.gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX")
                .registerTypeAdapter(RecordData.class, new RecordDataDeserializer())
                .create();
    }

    public abstract URI getApiRequestUri() throws URISyntaxException;
    public abstract T processResponseEntity(final HttpEntity entity) throws IOException;
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
            request.setEntity(getApiRequestEntity());

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
        } catch (URISyntaxException |IOException e) {
            throw new TrackviaClientException(e);
        } finally {
            if (response != null) try { response.close(); } catch (IOException e) {}
        }

        return result;
    }
}
