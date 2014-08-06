package trackvia.client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import trackvia.client.model.*;
import org.apache.http.HttpClientConnection;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectionRequest;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class TrackviaClient {
    private static Logger LOG = LoggerFactory.getLogger(TrackviaClient.class);

    public static final String DEFAULT_SCHEME = "https";
    public static final int DEFAULT_PORT = 80;

    private CloseableHttpClient httpClient;
    private HttpClientConnectionManager connectionManager;
    private String scheme = DEFAULT_SCHEME;
    private String hostname;
    private int port = DEFAULT_PORT;
    private OAuth2Token lastGoodToken;

    private TrackviaClient() {
    }

    public static TrackviaClient create(final String hostname, final String username, final String password)
            throws TrackviaApiException {
        return create(DEFAULT_SCHEME, hostname, DEFAULT_PORT, username, password);
    }

    public static TrackviaClient create(final String scheme, final String hostname, final int port,
                                        final String username, final String password)
            throws TrackviaApiException {
        TrackviaClient trackviaClient = new TrackviaClient();
        trackviaClient.initializeHttpClient();
        trackviaClient.scheme = scheme;
        trackviaClient.hostname = hostname;
        trackviaClient.port = port;

        // Obtain user credentials to use the API.  authorize() throws TrackviaApiException if the
        // authorization process fails for any reason.  Let it propagate.
        OAuth2Token token = trackviaClient.authorize(username, password);

        return trackviaClient;
    }

    /**
     * Facilitates use of mocking frameworks for testing.
     */
    static TrackviaClient create(final CloseableHttpClient mockHttpClient,
                                 final HttpClientConnectionManager mockConnectionManager,
                                 final String hostname, final String username, final String password) {
        TrackviaClient trackviaClient = new TrackviaClient();
        trackviaClient.httpClient = mockHttpClient;
        trackviaClient.hostname = hostname;
        trackviaClient.connectionManager = mockConnectionManager;

        return trackviaClient;
    }

    public void shutdown() {
        this.connectionManager.shutdown();
    }

    protected void initializeHttpClient() {
        PlainConnectionSocketFactory plainsf = PlainConnectionSocketFactory.getSocketFactory();
        SSLConnectionSocketFactory sslsf = SSLConnectionSocketFactory.getSocketFactory();
        Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", plainsf)
                .register("https", sslsf)
                .build();
        this.connectionManager = new PoolingHttpClientConnectionManager(registry);
        this.httpClient = HttpClients.custom()
                .setConnectionManager(this.connectionManager)
                .build();
    }

    protected Object execute(OverHttpCommand command) {
        HttpRoute route = new HttpRoute(new HttpHost(TrackviaClient.this.hostname, 80));
        ConnectionRequest cr = this.connectionManager.requestConnection(route, null);
        HttpClientConnection connection = null;
        Object apiResponse = null;

        try {
            connection = cr.get(5, TimeUnit.SECONDS);

            if (!connection.isOpen()) {
                this.connectionManager.connect(connection, route, 1000, command.getContext());
                this.connectionManager.routeComplete(connection, route, command.getContext());
            }

            apiResponse = command.execute(this.httpClient);

        } catch (TrackviaApiException e) {
            throw e;
        } catch (Exception e) {
            throw new TrackviaClientException(e);
        } finally {
            if (connection != null) this.connectionManager.releaseConnection(connection, null, 1, TimeUnit.SECONDS);
        }

        return apiResponse;
    }

    protected synchronized void setAuthToken(OAuth2Token token) {
        this.lastGoodToken = token;
    }

    protected synchronized String getAccessToken() {
        return (this.lastGoodToken != null) ? (this.lastGoodToken.getValue()) : (null);
    }

    protected synchronized String getRefreshToken() {
        return (this.lastGoodToken != null) ? (this.lastGoodToken.getRefreshToken().getValue()) : (null);
    }

    public OAuth2Token refreshAccessToken() throws TrackviaApiException {
        HttpClientContext context = HttpClientContext.create();
        OAuth2Token token = (OAuth2Token) execute(new CommandOverHttpGet<OAuth2Token>(context) {
            @Override
            public URI getApiRequestUri() throws URISyntaxException {
                return new URIBuilder()
                        .setScheme("https")
                        .setHost(TrackviaClient.this.hostname)
                        .setPath("/oauth/token")
                        .setParameter("refresh_token", getRefreshToken())
                        .setParameter("client_id", "xvia-webapp")
                        .setParameter("grant_type", "refresh_token")
                        .setParameter("redirect_uri", "")
                        .build();
            }

            @Override
            public OAuth2Token processResponseEntity(final HttpEntity entity, final Gson gson) throws IOException {
                Reader jsonReader = new InputStreamReader(entity.getContent());

                return gson.fromJson(jsonReader, OAuth2Token.class);
            }
        });

        setAuthToken(token);

        return token;
    }

    public OAuth2Token authorize(final String username, final String password) throws TrackviaApiException {
        HttpClientContext context = HttpClientContext.create();
        OAuth2Token token = (OAuth2Token) execute(new CommandOverHttpGet<OAuth2Token>(context) {
            @Override
            public URI getApiRequestUri() throws URISyntaxException {
                return new URIBuilder()
                        .setScheme("https")
                        .setHost(TrackviaClient.this.hostname)
                        .setPath("/oauth/token")
                        .setParameter("username", username)
                        .setParameter("password", password)
                        .setParameter("client_id", "xvia-webapp")
                        .setParameter("grant_type", "password")
                        .build();
            }

            @Override
            public OAuth2Token processResponseEntity(final HttpEntity entity, final Gson gson) throws IOException {
                Reader jsonReader = new InputStreamReader(entity.getContent());

                return gson.fromJson(jsonReader, OAuth2Token.class);
            }
        });

        setAuthToken(token);

        return token;
    }

    /**
     * Get all users.
     */
    public List<User> getUsers(final int start, final int max) throws TrackviaApiException {
        Authorized<UserRecordSet> action = new Authorized<>(this);
        UserRecordSet rs = action.execute(new Callable<UserRecordSet>() {
            @Override
            public UserRecordSet call() throws Exception {
                HttpClientContext context = HttpClientContext.create();
                return (UserRecordSet) execute(new CommandOverHttpGet<UserRecordSet>(context) {
                    @Override
                    public URI getApiRequestUri() throws URISyntaxException {
                        List<NameValuePair> params = pairsFromGetUsersParams(start, max);
                        params.add(new NameValuePair() {
                            @Override
                            public String getName() {
                                return "access_token";
                            }

                            @Override
                            public String getValue() {
                                return getAccessToken();
                            }
                        });
                        return new URIBuilder()
                                .setScheme("https")
                                .setHost(TrackviaClient.this.hostname)
                                .setPath("/openapi/users")
                                .setParameters(params)
                                .build();
                    }

                    @Override
                    public UserRecordSet processResponseEntity(final HttpEntity entity, final Gson gson) throws IOException {
                        Reader jsonReader = new InputStreamReader(entity.getContent());

                        return gson.fromJson(jsonReader, UserRecordSet.class);
                    }
                });
            }
        });
        return rs.getData();
    }

    private List<NameValuePair> pairsFromGetUsersParams(final int start, final int max) {
        List<NameValuePair> pairs = new ArrayList<NameValuePair>(3);

        pairs.add(new NameValuePair() {
            @Override
            public String getName() {
                return "start";
            }

            @Override
            public String getValue() {
                return (start < 0) ? ("0") : (String.valueOf(start));
            }
        });

        pairs.add(new NameValuePair() {
            @Override
            public String getName() {
                return "max";
            }

            @Override
            public String getValue() {
                return (max < start) ? ("50") : (max > 100) ? ("100") : (String.valueOf(max));
            }
        });

        return pairs;
    }

    /**
     * Create a user.
     */
    public User createUser(final String email, final String firstName, final String lastName, final TimeZone timeZone) {
        Authorized<UserRecord> action = new Authorized<>(this);
        UserRecord userRecord = action.execute(new Callable<UserRecord>() {
            @Override
            public UserRecord call() throws Exception {
                HttpClientContext context = HttpClientContext.create();

                return (UserRecord) execute(new CommandOverHttpPost<UserRecord>(context) {
                    @Override
                    public URI getApiRequestUri() throws URISyntaxException {
                        return new URIBuilder()
                                .setScheme("https")
                                .setHost(TrackviaClient.this.hostname)
                                .setPath("/openapi/users")
                                .setParameter("access_token", getAccessToken())
                                .build();
                    }

                    @Override
                    public UserRecord processResponseEntity(final HttpEntity entity, final Gson gson) throws IOException {
                        Reader jsonReader = new InputStreamReader(entity.getContent());

                        return gson.fromJson(jsonReader, UserRecord.class);
                    }

                    @Override
                    public HttpEntity getApiRequestEntity(final Gson gson) throws UnsupportedEncodingException {
                        List<NameValuePair> params = new ArrayList<NameValuePair>();
                        params.add(new BasicNameValuePair("email", email));
                        params.add(new BasicNameValuePair("firstName", firstName));
                        params.add(new BasicNameValuePair("lastName", lastName));
                        params.add(new BasicNameValuePair("timeZone", timeZone.getDisplayName(false, TimeZone.SHORT)));
                        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params);
                        return entity;
                    }
                });
            }
        });

        return (userRecord != null) ? (userRecord.getData()) : (null);
    }

    /**
     * Get a user's authorized apps.
     */
    public List<App> getApps() throws TrackviaApiException {
        Authorized<List<App>> action = new Authorized<>(this);

        return action.execute(new Callable<List<App>>() {
            @Override
            public List<App> call() throws Exception {
                HttpClientContext context = HttpClientContext.create();

                return (List<App>) execute(new CommandOverHttpGet<List<App>>(context) {
                    @Override
                    public URI getApiRequestUri() throws URISyntaxException {
                        return new URIBuilder()
                                .setScheme("https")
                                .setHost(TrackviaClient.this.hostname)
                                .setPath("/openapi/apps")
                                .setParameter("access_token", getAccessToken())
                                .build();
                    }

                    @Override
                    public List<App> processResponseEntity(final HttpEntity entity, final Gson gson) throws IOException {
                        Reader jsonReader = new InputStreamReader(entity.getContent());
                        Type responseType = new TypeToken<List<App>>() {
                        }.getType();

                        return gson.fromJson(jsonReader, responseType);
                    }
                });
            }
        });
    }

    /**
     * Gets a user's authorized views.
     */
    public List<View> getViews() throws TrackviaApiException {
        Authorized<List<View>> action = new Authorized<>(this);

        return action.execute(new Callable<List<View>>() {
            @Override
            public List<View> call() throws Exception {
                HttpClientContext context = HttpClientContext.create();

                return (List<View>) execute(new CommandOverHttpGet<List<View>>(context) {
                    @Override
                    public URI getApiRequestUri() throws URISyntaxException {
                        return new URIBuilder()
                                .setScheme("https")
                                .setHost(TrackviaClient.this.hostname)
                                .setPath("/openapi/views")
                                .setParameter("access_token", getAccessToken())
                                .build();
                    }

                    @Override
                    public List<View> processResponseEntity(final HttpEntity entity, final Gson gson) throws IOException {
                        Reader jsonReader = new InputStreamReader(entity.getContent());
                        Type responseType = new TypeToken<List<View>>() {
                        }.getType();

                        return gson.fromJson(jsonReader, responseType);
                    }
                });
            }
        });
    }

    /**
     * Find matching records in a given view.
     */
    public RecordSet findRecords(final int viewId, final String q, final int start, final int max) throws TrackviaApiException {
        Authorized<RecordSet> action = new Authorized<>(this);

        return action.execute(new Callable<RecordSet>() {
            @Override
            public RecordSet call() throws Exception {
                HttpClientContext context = HttpClientContext.create();
                return (RecordSet) execute(new CommandOverHttpGet<RecordSet>(context) {
                    @Override
                    public URI getApiRequestUri() throws URISyntaxException {
                        List<NameValuePair> params = pairsFromFindRecordParams(q, start, max);
                        params.add(new NameValuePair() {
                            @Override
                            public String getName() {
                                return "access_token";
                            }

                            @Override
                            public String getValue() {
                                return getAccessToken();
                            }
                        });
                        return new URIBuilder()
                                .setScheme("https")
                                .setHost(TrackviaClient.this.hostname)
                                .setPath(String.format("/openapi/views/%d/find", viewId))
                                .setParameters(params)
                                .build();
                    }

                    @Override
                    public RecordSet processResponseEntity(final HttpEntity entity, final Gson gson) throws IOException {
                        Reader jsonReader = new InputStreamReader(entity.getContent());

                        return gson.fromJson(jsonReader, RecordSet.class);
                    }
                });
            }
        });
    }

    private List<NameValuePair> pairsFromFindRecordParams(final String q, final int start, final int max) {
        List<NameValuePair> pairs = new ArrayList<NameValuePair>(3);

        pairs.add(new NameValuePair() {
            @Override
            public String getName() {
                return "q";
            }

            @Override
            public String getValue() {
                return q;
            }
        });

        pairs.add(new NameValuePair() {
            @Override
            public String getName() {
                return "start";
            }

            @Override
            public String getValue() {
                return (start < 0) ? ("0") : (String.valueOf(start));
            }
        });

        pairs.add(new NameValuePair() {
            @Override
            public String getName() {
                return "max";
            }

            @Override
            public String getValue() {
                return (max < start) ? ("50") : (max > 100) ? ("100") : (String.valueOf(max));
            }
        });

        return pairs;
    }

    /**
     * Gets all records in a given view.
     */
    public RecordSet getRecords(final int viewId) throws TrackviaApiException {
        Authorized<RecordSet> action = new Authorized<>(this);

        return action.execute(new Callable<RecordSet>() {
            @Override
            public RecordSet call() throws Exception {
                HttpClientContext context = HttpClientContext.create();
                return (RecordSet) execute(new CommandOverHttpGet<RecordSet>(context) {
                    @Override
                    public URI getApiRequestUri() throws URISyntaxException {
                        return new URIBuilder()
                                .setScheme("https")
                                .setHost(TrackviaClient.this.hostname)
                                .setPath(String.format("/openapi/views/%d", viewId))
                                .setParameter("access_token", getAccessToken())
                                .build();
                    }

                    @Override
                    public RecordSet processResponseEntity(final HttpEntity entity, final Gson gson) throws IOException {
                        Reader jsonReader = new InputStreamReader(entity.getContent());

                        return gson.fromJson(jsonReader, RecordSet.class);
                    }
                });
            }
        });
    }

    /**
     * Gets a specific record in a given view.
     */
    public Record getRecord(final long viewId, final long recordId) throws TrackviaApiException {
        Authorized<Record> action = new Authorized<>(this);

        return action.execute(new Callable<Record>() {
            @Override
            public Record call() throws Exception {
                HttpClientContext context = HttpClientContext.create();
                return (Record) execute(new CommandOverHttpGet<Record>(context) {
                    @Override
                    public URI getApiRequestUri() throws URISyntaxException {
                        return new URIBuilder()
                                .setScheme("https")
                                .setHost(TrackviaClient.this.hostname)
                                .setPath(String.format("/openapi/views/%d/records/%d", viewId, recordId))
                                .setParameter("access_token", getAccessToken())
                                .build();
                    }

                    @Override
                    public Record processResponseEntity(final HttpEntity entity, final Gson gson) throws IOException {
                        Reader jsonReader = new InputStreamReader(entity.getContent());

                        return gson.fromJson(jsonReader, Record.class);
                    }
                });
            }
        });
    }

    /**
     * Creates records.
     */
    public RecordSet createRecords(final int viewId, final RecordDataBatch batch) {
        Authorized<RecordSet> action = new Authorized<>(this);

        return action.execute(new Callable<RecordSet>() {
            @Override
            public RecordSet call() throws Exception {
                HttpClientContext context = HttpClientContext.create();
                return (RecordSet) execute(new CommandOverHttpPost<RecordSet>(context) {
                    @Override
                    public URI getApiRequestUri() throws URISyntaxException {
                        return new URIBuilder()
                                .setScheme("https")
                                .setHost(TrackviaClient.this.hostname)
                                .setPath(String.format("/openapi/views/%d/records", viewId))
                                .setParameter("access_token", getAccessToken())
                                .build();
                    }

                    @Override
                    public RecordSet processResponseEntity(final HttpEntity entity, final Gson gson) throws IOException {
                        Reader jsonReader = new InputStreamReader(entity.getContent());

                        return gson.fromJson(jsonReader, RecordSet.class);
                    }

                    @Override
                    public HttpEntity getApiRequestEntity(final Gson gson) throws UnsupportedEncodingException {
                        return new StringEntity(gson.toJson(batch), ContentType.APPLICATION_JSON);
                    }
                });
            }
        });
    }

    /**
     * Updates a record.
     */
    public Record updateRecord(final int viewId, final RecordData data) {
        Authorized<RecordSet> action = new Authorized<>(this);
        RecordSet rs = action.execute(new Callable<RecordSet>() {
            @Override
            public RecordSet call() throws Exception {
                HttpClientContext context = HttpClientContext.create();
                return (RecordSet) execute(new CommandOverHttpPut<RecordSet>(context) {
                    @Override
                    public URI getApiRequestUri() throws URISyntaxException {
                        final Object id = data.getRecordId();

                        return new URIBuilder()
                                .setScheme("https")
                                .setHost(TrackviaClient.this.hostname)
                                .setPath(String.format("/openapi/views/%d/records/%s", viewId, id))
                                .setParameter("access_token", getAccessToken())
                                .build();
                    }

                    @Override
                    public RecordSet processResponseEntity(final HttpEntity entity, final Gson gson) throws IOException {
                        Reader jsonReader = new InputStreamReader(entity.getContent());

                        return gson.fromJson(jsonReader, RecordSet.class);
                    }

                    @Override
                    public HttpEntity getApiRequestEntity(final Gson gson) throws UnsupportedEncodingException {
                        RecordDataBatch batchOfOne = new RecordDataBatch();
                        batchOfOne.setData(Arrays.asList(new RecordData[]{filterImpossibles(data)}));

                        return new StringEntity(gson.toJson(batchOfOne), ContentType.APPLICATION_JSON);
                    }

                    // TODO: the service throws exception when this column is present.  It has special significance
                    // and must be handled individually in the service itself.
                    RecordData filterImpossibles(RecordData data) {
                        RecordData filtered = new RecordData();
                        for (String key : data.keySet()) {
                            if (!"Record ID".equalsIgnoreCase(key)) {
                                filtered.put(key, data.get(key));
                            }
                        }
                        return filtered;
                    }
                });
            }
        });

        // For updates, the structure between the Service APIs (batch) and client APIs (single) is somewhat different.
        // Exposing 'batch' in the Client API would be misleading since the Service API doesn't support batch
        // updates (yet).
        Record updatedRecord = null;
        if (rs.getData() != null) {
            updatedRecord = new Record(rs.getStructure(), rs.getData().get(0));
        }

        return updatedRecord;
    }

    /**
     * Deletes a record.
     */
    public void deleteRecord(final int viewId, final long recordId) {
        Authorized<Void> action = new Authorized<>(this);
        action.execute(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                HttpClientContext context = HttpClientContext.create();
                execute(new CommandOverHttpDelete<Void>(context) {
                    @Override
                    public URI getApiRequestUri() throws URISyntaxException {
                        return new URIBuilder()
                                .setScheme("https")
                                .setHost(TrackviaClient.this.hostname)
                                .setPath(String.format("/openapi/views/%d/records/%d", viewId, recordId))
                                .setParameter("access_token", getAccessToken())
                                .build();
                    }
                });
                return null;
            }
        });
    }

    /**
     * Adds file contents (image or document) to a record, if permissible.
     */
    public Record addFile(final int viewId, final long recordId, final String columnName, final Path filePath) {
        Authorized<Record> action = new Authorized<>(this);
        return action.execute(new Callable<Record>() {
            @Override
            public Record call() throws Exception {
                HttpClientContext context = HttpClientContext.create();
                return (Record) execute(new CommandOverHttpPost<Record>(context) {
                    @Override
                    public URI getApiRequestUri() throws URISyntaxException {
                        return new URIBuilder()
                                .setScheme("https")
                                .setHost(TrackviaClient.this.hostname)
                                .setPath(String.format("/openapi/views/%d/records/%d/files/%s", viewId, recordId, columnName))
                                .setParameter("access_token", getAccessToken())
                                .build();
                    }

                    @Override
                    public Record processResponseEntity(final HttpEntity entity, final Gson gson) throws IOException {
                        Reader jsonReader = new InputStreamReader(entity.getContent());

                        return gson.fromJson(jsonReader, Record.class);
                    }

                    @Override
                    public HttpEntity getApiRequestEntity(final Gson gson) throws UnsupportedEncodingException {
                        return MultipartEntityBuilder.create()
                                .addPart("file", new FileBody(filePath.toFile()))
                                .build();
                    }
                });
            }
        });
    }

    /**
     * Gets file contents from a record, if permissible.
     */
    public void getFile(final int viewId, final long recordId, final String columnName, final Path filePath) {
        Authorized<Void> action = new Authorized<>(this);
        action.execute(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                HttpClientContext context = HttpClientContext.create();

                // fail fast if the target 'filePath' already exists; no overwrites allowed.
                if (Files.exists(filePath)) {
                    throw new TrackviaClientException(String.format("Will not overwrite the file %s; aborting", filePath.toString()));
                }

                execute(new CommandOverHttpGet<Void>(context) {
                    @Override
                    public URI getApiRequestUri() throws URISyntaxException {
                        return new URIBuilder()
                                .setScheme("https")
                                .setHost(TrackviaClient.this.hostname)
                                .setPath(String.format("/openapi/views/%d/records/%d/files/%s", viewId, recordId, columnName))
                                .setParameter("access_token", getAccessToken())
                                .build();
                    }

                    @Override
                    public Void processResponseEntity(final HttpEntity entity, final Gson gson) throws IOException {
                        Files.copy(entity.getContent(), filePath);

                        return null;
                    }
                });

                return null;
            }
        });
    }

    /**
     * Deletes a file, if permissible.
     */
    public void deleteFile(final int viewId, final long recordId, final String columnName) {
        Authorized<Void> action = new Authorized<>(this);
        action.execute(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                HttpClientContext context = HttpClientContext.create();
                execute(new CommandOverHttpDelete<Void>(context) {
                    @Override
                    public URI getApiRequestUri() throws URISyntaxException {
                        return new URIBuilder()
                                .setScheme("https")
                                .setHost(TrackviaClient.this.hostname)
                                .setPath(String.format("/openapi/views/%d/records/%d/files/%s", viewId, recordId, columnName))
                                .setParameter("access_token", getAccessToken())
                                .build();
                    }
                });

                return null;
            }
        });
    }
}
