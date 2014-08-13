package trackvia.client;

import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;

public interface OverHttpCommand<T> {
    HttpClientContext getContext();
    T execute(CloseableHttpClient httpClient);
}
