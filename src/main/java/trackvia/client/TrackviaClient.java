package trackvia.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

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
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import trackvia.client.model.App;
import trackvia.client.model.DomainRecord;
import trackvia.client.model.DomainRecordDataBatch;
import trackvia.client.model.DomainRecordDataBatchSerializer;
import trackvia.client.model.DomainRecordDataBatchType;
import trackvia.client.model.DomainRecordDeserializer;
import trackvia.client.model.DomainRecordSet;
import trackvia.client.model.DomainRecordSetDeserializer;
import trackvia.client.model.DomainRecordSetType;
import trackvia.client.model.DomainRecordType;
import trackvia.client.model.Identifiable;
import trackvia.client.model.OAuth2Token;
import trackvia.client.model.Record;
import trackvia.client.model.RecordData;
import trackvia.client.model.RecordDataBatch;
import trackvia.client.model.RecordDataDeserializer;
import trackvia.client.model.RecordDataSerializer;
import trackvia.client.model.RecordSet;
import trackvia.client.model.TrackviaSerializationExclusionStrategy;
import trackvia.client.model.User;
import trackvia.client.model.UserRecord;
import trackvia.client.model.UserRecordSet;
import trackvia.client.model.View;

/**
 * Trackvia Open API Java client
 *
 * This client provides you a way to integrate with your Trackvia application.
 *
 * Getting Started
 * ===============
 *
 * Authentication
 * ==============
 *
 * You would authenticate as a user of your application, who has been given permission to access the
 * application's table data via forms/roles.
 *
 * <pre>
 * {@code
 *      TrackviaClient client = TrackviaClient.create("go.api.trackvia.com", "myuser", "mypassword", "userkey");
 * }
 * </pre>
 *
 * create() will throw a TrackviaApiException if the credentials are invalid or TrackviaClientException if
 * something else goes wrong.
 *
 * Once authenticated, the client will hold an OAuth2 access token and a refresh token, used to obtain a new
 * access token when the current one expires.
 *
 * Views
 * =====
 *
 * Once authenticated, use views to read or write data.  You will need the view's identifier, which can
 * be obtained either by looking it up by the view's name or by gleaning it from the Trackvia UI.
 *
 * <pre>
 * {@code
 *      // Either
 *      List<View> views = client.getViews();
 *
 *      // Or
 *      View view = client.getView("Default Contacts View")
 * }
 * </pre>
 *
 * Records
 * =======
 *
 * The programming model provides creation/retrieval/update/deletion (CRUD) access to table data in two ways:
 * <ul>
 *     <li>Weakly-typed Map<String, Object> interface</li>
 *     <li>Strongly-typed application-class interface</li>
 * </ul>
 *
 * Weakly-typed (raw) records can be read as follows:
 * <pre>
 *     {@code
 *     int viewId = 1;
 *     int q = "HIGH PRIORITY";
 *     int startIndex = 0;
 *     int maxResults = 25;
 *
 *     // Find records by substring match:
 *     RecordSet rs = client.findRecords(viewId, q, startIndex, maxResults);
 *
 *     // Get all records available in a given view.  Use this only for small tables.  The
 *     // service will impose a maximum record-set size, to keep requests reasonable.
 *     RecordSet rs = client.getRecords(viewId);
 *
 *     RecordSet contains a 'data' field, storing record data, and a 'structure' field storing
 *     field metadata, describing column names, its Trackvia data type and other information about the column.
 *     }
 * </pre>
 *
 * Strong-typing requires class definitions of application data-types, implemented as a simple "value object"
 * class, observing the Java Bean convention.  The mapping strategy requires the class' field names match (case-
 * insensitive) the names of Trackvia columns for the table.  For example:
 * <pre>
 *     {@code
 *     public class Contact {
 *         private String firstName;
 *         private String lastName;
 *         private String company;
 *         private String email;
 *
 *         public Contact() {}
 *
 *         public String getFirstName() { return this.firstName; }
 *         public void setFirstName(String firstName) { this.firstName = firstName; }
 *
 *         public String getLastName() { return this.lastName; }
 *         public void setLastName(String lastName) { this.lastName = lastName; }
 *
 *         public String getCompany() { return this.company; }
 *         public void setCompany(String company) { this.company = company; }
 *
 *         public String getEmail() { return this.email; }
 *         public void setEmail(String email) { this.email = email; }
 *     }
 *     }
 *</pre>
 *
 *     Given this domain class, your integration code can find Contact records:
 *
 * <pre>
 *     {@code
 *     int viewId = 1;
 *     int q = "HIGH PRIORITY";
 *     int startIndex = 0;
 *     int maxResults = 25;
 *     TrackviaClient client = ...;
 *     Contact contact = new Contact();
 *
 *     List<Contact> contacts = client.findRecords(Contact.class, viewId, q, startIndex, maxResults);
 *     }
 * </pre>
 *
 * <i>Important: the client will not deserialize or serialize nested objects.  For Trackvia tables that reference
 * other tables, your application code will handle these "foreign key" references as an additional retrieval of the
 * referenced table data.</i>
 *
 * When it comes to serialization, consider these Java/Trackvia type mappings:
 *
 *      Java type                            Supporting Trackvia type
 *      --------------------------------------------------------------------
 *      Short                                Number
 *      Integer                              Number
 *      Long                                 Number, Identifier, AutoIncrement, Document, Image
 *      Float                                Number, Currency, Percentage
 *      Double                               Number, Currency, Percentage
 *      BigDecimal                           Number, Currency, Percentage
 *      String                               ShortAnswer, Paragraph
 *      java.util.Date                       DateTime, Date
 *      String                               User, Email, UserStatus, TimeZone, URL
 *      java.text.TimeZone                   TimeZone
 *      List<String>                         DropDown, CheckBox
 *
 * Similarly deserialization observes these mappings:
 *
 *      Trackvia type                           Expected Java type on the domain class
 *      -------------------------------------------------------------------------------
 *      DateTime("datetime")                    java.util.Date
 *      User("user")                            String
 *      Identifier("identifier")                Long
 *      ShortAnswer("shortAnswer")              String
 *      Email("email")                          String
 *      UserStatus("userStatus")                String
 *      TimeZone("timeZone")                    String
 *      Paragraph("paragraph")                  String
 *      Number("number")                        Double
 *      Percentage("percentage")                Double
 *      Currency("currency")                    Double
 *      AutoIncrement("autoIncrement")          Long
 *      DropDown("dropDown")                    List<String>
 *      CheckBox("checkbox")                    List<String>
 *      Date("date")                            java.util.Date
 *      Document("document")                    Long
 *      Image("image")                          Long
 *      URL("url")                              String
 *
 * Files
 * =====
 *
 * Tables can have "file" fields, allowing file content to be uploaded/downloaded.
 *
 * <pre>
 *     {@code
 *     TrackviaClient client = ...;
 *     int viewId = 1;
 *     long recordId = 1;
 *     String fileName = "contractDoc";
 *     Path filePath = java.nio.file.Paths.get("/path/to/file");
 *
 *     // Adding a file
 *
 *     Record rawRecord = client.addFile(viewId, recordId, fileName, filePath);
 *       or
 *     DomainRecord<Contact> domainRecord = client.addFile(Contact.class, viewId, recordId, fileName, filePath);
 *
 *     // Getting a file
 *
 *     client.getFile(viewId, recordId, fileName, filePath);
 *
 *     // Deleting a file
 *
 *     client.deleteFile(viewId, recordId, fileName);
 *     }
 * </pre>
 *
 * Apps
 * ====
 *
 * Get all available applications.
 *
 * <pre>
 *     {@code
 *     TrackviaClient client = ...;
 *     List<App> apps = client.getApp();
 *     }
 * </pre>
 *
 * Users
 * =====
 *
 * You can administratively lookup and create users.
 *
 * <pre>
 *     {@code
 *     TrackviaClient client = ...;
 *     int startIndex = 0;
 *     int maxResults = 25;
 *     List<User> users = client.getUsers(startIndex, maxResults);
 *     }
 * </pre>
 */
public class TrackviaClient {
    private static Logger LOG = LoggerFactory.getLogger(TrackviaClient.class);

    public static final String DEFAULT_BASE_URI_PATH = "/";
    public static final String DEFAULT_SCHEME = "https";
    public static final int DEFAULT_PORT = 443;

    protected static final String ACCESS_TOKEN_QUERY_PARAM = "access_token";
    protected static final String USER_KEY_QUERY_PARAM = "user_key";
    protected static final String API_VERSION_HEADER = "api-version";
    
    protected CloseableHttpClient httpClient;
    protected HttpClientConnectionManager connectionManager;
    protected String baseUriPath;
    protected String scheme = DEFAULT_SCHEME;
    protected String hostname;
    protected String apiUserKey;
    protected int port = DEFAULT_PORT;
    private OAuth2Token lastGoodToken;
    protected Gson recordAsMapGson;
    private Map<String, Gson> typeToGsonCache = new HashMap<String, Gson>();

    protected TrackviaClient() {}

    /**
     * Creates a client, with which to access the Trackvia API.
     *
     * Defaults to the HTTPS protocol scheme and port 80.  These can be
     * overridden using a different constructor.
     *
     * @see #create(String, String, String, int, String, String, String) to override scheme and port
     *
     * @param hostname host of the service api endpoint
     * @param username name of an account user with access to targeted views and forms
     * @param password password of the account user
     * @param apiUserKey 3Scale user key, granted when registering using the Trackvia Developer Portal
     * @return a client acting on behalf of an authenticated user
     * @throws TrackviaApiException if authentication fails for whatever reason
     */
    public static TrackviaClient create(final String hostname, final String username, final String password,
            final String apiUserKey) throws TrackviaApiException {
        return create(DEFAULT_BASE_URI_PATH, DEFAULT_SCHEME, hostname, DEFAULT_PORT, username, password, apiUserKey);
    }
    
    /**
     * Creates a client, with which to access the Trackvia API.
     *
     * Defaults to the HTTPS protocol scheme and port 443.  These can be
     * overridden using a different constructor.
     *      
     * @see #create(String, String, int, String) to override scheme and port
     *
     * @param hostname host of the service api endpoint
     * @param accessToken Trackvia oauth access token
     * @param apiUserKey 3Scale user key, granted when registering using the Trackvia Developer Portal
     * @return a client acting on behalf of already authenticated access token
     * @throws TrackviaApiException if authentication fails for whatever reason
     */
    public static TrackviaClient create(final String hostname, final String accessToken, String apiUserKey) {
    	return create(hostname, accessToken, DEFAULT_BASE_URI_PATH, DEFAULT_SCHEME, DEFAULT_PORT, apiUserKey);
    }
    
    public static TrackviaClient create(final String hostname, final String accessToken, String basePath, String scheme, Integer port, String apiUserKey) {
        TrackviaClient trackviaClient = new TrackviaClient();
        trackviaClient.initializeHttpClient();
        trackviaClient.baseUriPath = basePath;
        trackviaClient.scheme = scheme;
        trackviaClient.hostname = hostname;
        trackviaClient.port = port;
        trackviaClient.apiUserKey = apiUserKey;

        trackviaClient.recordAsMapGson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX")
                .registerTypeAdapter(RecordData.class, new RecordDataDeserializer())
                .registerTypeAdapter(RecordData.class, new RecordDataSerializer())
                .addSerializationExclusionStrategy(new TrackviaSerializationExclusionStrategy())
		.serializeNulls()
                .create();

        OAuth2Token token = new OAuth2Token();
        token.setAccessToken(accessToken);
        token.setAccess_token(accessToken);
        token.setValue(accessToken);
        trackviaClient.setAuthToken(token);
        
        return trackviaClient;
    }


    /**
     * Creates a client, with which to access the Trackvia API.
     *
     * @param baseUriPath prefixed to every HTTP request, before API-specific path segments (e.g., /openapi)
     * @param scheme one of the supported protocol schemes (http or https)
     * @param hostname host of the service api endpoint
     * @param port port of the service endpoint (default: 80)
     * @param username name of an account user with access to targeted views and forms
     * @param password password of the account user
     * @param apiUserKey 3Scale user key, granted when registering using the Trackvia Developer Portal
     * @return a client acting on behalf of an authenticated user
     * @throws TrackviaApiException if authentication fails for whatever reason
     */
    public static TrackviaClient create(final String baseUriPath, final String scheme, final String hostname, final int port,
                                        final String username, final String password, final String apiUserKey)
            throws TrackviaApiException {
        TrackviaClient trackviaClient = new TrackviaClient();
        trackviaClient.initializeHttpClient();
        trackviaClient.baseUriPath = baseUriPath;
        trackviaClient.scheme = scheme;
        trackviaClient.hostname = hostname;
        trackviaClient.port = port;
        trackviaClient.apiUserKey = apiUserKey;

        trackviaClient.recordAsMapGson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX")
                .registerTypeAdapter(RecordData.class, new RecordDataDeserializer())
                .registerTypeAdapter(RecordData.class, new RecordDataSerializer())
                .addSerializationExclusionStrategy(new TrackviaSerializationExclusionStrategy())
                .create();

        // Obtain user credentials to use the API.  authorize() throws TrackviaApiException if the
        // authorization process fails for any reason.  Let it propagate.
        trackviaClient.authorize(username, password);

        return trackviaClient;
    }

    /**
     * Facilitates use of mocking frameworks for testing.
     *
     * Kept at package-level visibility, assuming the test classes live in the same package: trackvia.client
     */
    static TrackviaClient create(final CloseableHttpClient mockHttpClient,
                                 final HttpClientConnectionManager mockConnectionManager,
                                 final String hostname, final String username, final String password) {
        TrackviaClient trackviaClient = new TrackviaClient();
        trackviaClient.httpClient = mockHttpClient;
        trackviaClient.hostname = hostname;
        trackviaClient.connectionManager = mockConnectionManager;

        trackviaClient.recordAsMapGson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX")
                .registerTypeAdapter(RecordData.class, new RecordDataDeserializer())
                .registerTypeAdapter(RecordData.class, new RecordDataSerializer())
                .addSerializationExclusionStrategy(new TrackviaSerializationExclusionStrategy())
                .create();

        return trackviaClient;
    }

    /**
     * Gracefully shuts down connection management, allowing work on open connections
     * to finish and disallowing new connections.
     */
    public void shutdown() {
        this.connectionManager.shutdown();
    }

    protected void initializeHttpClient() {
        PlainConnectionSocketFactory plainsf = PlainConnectionSocketFactory.getSocketFactory();
        SSLConnectionSocketFactory sslsf = SSLConnectionSocketFactory.getSocketFactory();
        Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", plainsf)
                .register(this.scheme, sslsf)
                .build();
        this.connectionManager = new PoolingHttpClientConnectionManager(registry);
        this.httpClient = HttpClients.custom()
                .setConnectionManager(this.connectionManager)
                .build();
    }

    protected <T> Gson lookupSerializer(final Class<T> domainClass, final ParameterizedType parameterClass) {
        String key = String.format("%s-%s", domainClass.getName(), parameterClass.getRawType().toString());
        Gson gson = typeToGsonCache.get(key);

        if (gson == null) {
            Object serializer = null;
            if (parameterClass.getRawType() == DomainRecordDataBatch.class) {
                serializer = new DomainRecordDataBatchSerializer<>(domainClass);
            } else {
                throw new IllegalArgumentException(String.format(
                        "No serializer available for type %s<%s>", parameterClass.getRawType().toString(),
                        domainClass.getName()));
            }

            gson = new GsonBuilder()
                    .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX")
                    .registerTypeAdapter(parameterClass.getRawType(), serializer)
                    .addSerializationExclusionStrategy(new TrackviaSerializationExclusionStrategy())
                    .create();

            typeToGsonCache.put(key, gson);
        }

        return gson;
    }

    protected <T> Gson lookupDeserializer(final Class<T> domainClass, final ParameterizedType parameterClass) {
        String key = String.format("%s-%s", domainClass.getName(), parameterClass.getRawType().toString());
        Gson gson = typeToGsonCache.get(key);

        if (gson == null) {
            Object deserializer = null;
            if (parameterClass.getRawType() == DomainRecordSet.class) {
                deserializer = new DomainRecordSetDeserializer<>(domainClass);
            } else if (parameterClass.getRawType() == DomainRecord.class) {
                deserializer = new DomainRecordDeserializer<>(domainClass);
            } else {
                throw new IllegalArgumentException(String.format(
                        "No deserializer available for type %s<%s>", parameterClass.getRawType().toString(),
                        domainClass.getName()));
            }

            gson = new GsonBuilder()
                    .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX")
                    .registerTypeAdapter(parameterClass, deserializer)
                    .addSerializationExclusionStrategy(new TrackviaSerializationExclusionStrategy())
                    .create();

            typeToGsonCache.put(key, gson);
        }

        return gson;
    }

    protected Object execute(OverHttpCommand command) {
        HttpRoute route = new HttpRoute(new HttpHost(TrackviaClient.this.hostname, this.port));
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

    protected String getApiUserKey() { return this.apiUserKey; }

    protected synchronized void setAuthToken(OAuth2Token token) {
        this.lastGoodToken = token;
    }
    
    public synchronized void updateApiVersion(String apiVersion) {
        if(lastGoodToken == null){
        	return;
        }
        lastGoodToken.setApiVersion(apiVersion);
        
    }

    protected synchronized String getAccessToken() {
        return (this.lastGoodToken != null) ? (this.lastGoodToken.getValue()) : (null);
    }

    protected synchronized String getRefreshToken() {
        return (this.lastGoodToken != null && this.lastGoodToken.getRefreshToken() != null) ?
                (this.lastGoodToken.getRefreshToken().getValue()) : (null);
    }
    
    /**
     * Grab the api version as a string, else gets negative
     * @return
     */
    protected synchronized String getApiVersion(){
    	if(this.lastGoodToken != null && this.lastGoodToken.getApiVersion() != null){
    		return lastGoodToken.getApiVersion();
    	} else {
    		return null;
    	}
    }

    /**
     * Force refresh of the last known good token, using the refresh token provided
     * by the service for that token.
     *
     * The client will automatically try to refresh the access token if it encounters
     * an ApiError.InvalidToken error on any service call.  Should this fail, that error
     * will be rethrown.  Catching it will provide an empty to handle authentication outside
     * of the client.  The advantage of refreshAccessToken is it is faster than calling
     * authorize().
     *
     * @see #authorize(String, String) to obtain another access and refresh token
     *
     * @throws TrackviaApiException if token refresh fails
     * @throws TrackviaClientException if an error occurs outside the service, failing the request
     */
    public void refreshAccessToken() throws TrackviaApiException, TrackviaClientException {
        final Gson gson = this.recordAsMapGson;
        final HttpClientContext context = HttpClientContext.create();
        final OAuth2Token token = (OAuth2Token) execute(new CommandOverHttpGet<OAuth2Token>(context, TrackviaClient.this) {
            @Override
            public URI getApiRequestUri() throws URISyntaxException {
                final String path = String.format("%s/oauth/token", TrackviaClient.this.baseUriPath);
                return new URIBuilder()
                        .setScheme(TrackviaClient.this.scheme)
                        .setHost(TrackviaClient.this.hostname)
                        .setPort(TrackviaClient.this.port)
                        .setPath(path)
                        .setParameter("refresh_token", getRefreshToken())
                        .setParameter("client_id", "TrackViaAPI")
                        .setParameter("grant_type", "refresh_token")
                        .setParameter("redirect_uri", "")
                        .build();
            }

            @Override
            public OAuth2Token processResponseEntity(final HttpEntity entity) throws IOException {
                Reader jsonReader = new InputStreamReader(entity.getContent());

                return gson.fromJson(jsonReader, OAuth2Token.class);
            }
        });

        setAuthToken(token);
    }

    /**
     * Authorizes the client for access to views and forms of a given account user.
     *
     * A side effect of a successful authentication try is the client saves the resulting
     * access and refresh token, caching it for future client calls.
     *
     * @param username name of the account user
     * @param password password of the account user
     * @throws TrackviaApiException if the authentication try fails for whatever reason
     * @throws TrackviaClientException if an error occurs outside the service, failing the request
     */
    public void authorize(final String username, final String password) throws TrackviaApiException, TrackviaClientException {
        final Gson gson = this.recordAsMapGson;
        HttpClientContext context = HttpClientContext.create();
        OAuth2Token token = (OAuth2Token) execute(new CommandOverHttpGet<OAuth2Token>(context, TrackviaClient.this) {
            @Override
            public URI getApiRequestUri() throws URISyntaxException {
                final String path = String.format("%s/oauth/token", TrackviaClient.this.baseUriPath);
                return new URIBuilder()
                        .setScheme(TrackviaClient.this.scheme)
                        .setHost(TrackviaClient.this.hostname)
                        .setPort(TrackviaClient.this.port)
                        .setPath(path)
                        .setParameter("username", username)
                        .setParameter("password", password)
                        .setParameter("client_id", "TrackViaAPI")
                        .setParameter("grant_type", "password")
                        .build();
            }

            @Override
            public OAuth2Token processResponseEntity(final HttpEntity entity) throws IOException {
                Reader jsonReader = new InputStreamReader(entity.getContent());

                return gson.fromJson(jsonReader, OAuth2Token.class);
            }
        });

        setAuthToken(token);
    }

    /**
     * Gets account users available to the authenticated user.
     *
     * @param start the index (0 based) of the first user record, useful for paging
     * @param max retrieve no more than this many user records
     * @return a list of available users, observing the start and max constraints
     * @throws TrackviaApiException if the service fails to process this request
     * @throws TrackviaClientException if an error occurs outside the service, failing the request
     */
    public List<User> getUsers(final int start, final int max) throws TrackviaApiException, TrackviaClientException {
        final Gson gson = this.recordAsMapGson;
        Authorized<UserRecordSet> action = new Authorized<>(this);
        UserRecordSet rs = action.execute(new Callable<UserRecordSet>() {
            @Override
            public UserRecordSet call() throws Exception {
                HttpClientContext context = HttpClientContext.create();
                return (UserRecordSet) execute(new CommandOverHttpGet<UserRecordSet>(context, TrackviaClient.this) {
                    @Override
                    public URI getApiRequestUri() throws URISyntaxException {
                        final List<NameValuePair> params = pairsFromGetUsersParams(start, max);
                        params.add(new NameValuePair() {
                            @Override public String getName() {
                                return ACCESS_TOKEN_QUERY_PARAM;
                            }
                            @Override public String getValue() {
                                return TrackviaClient.this.getAccessToken();
                            }
                        });
                        params.add(new NameValuePair() {
                            @Override public String getName() {
                                return USER_KEY_QUERY_PARAM;
                            }
                            @Override public String getValue() {
                                return TrackviaClient.this.getApiUserKey();
                            }
                        });
                        final String path = String.format("%s/openapi/users", TrackviaClient.this.baseUriPath);
                        return new URIBuilder()
                                .setScheme(TrackviaClient.this.scheme)
                                .setHost(TrackviaClient.this.hostname)
                                .setPort(TrackviaClient.this.port)
                                .setPath(path)
                                .setParameters(params)
                                .build();
                    }

                    @Override
                    public UserRecordSet processResponseEntity(final HttpEntity entity) throws IOException {
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
            @Override public String getName() {
                return "start";
            }
            @Override public String getValue() {
                return (start < 0) ? ("0") : (String.valueOf(start));
            }
        });

        pairs.add(new NameValuePair() {
            @Override public String getName() {
                return "max";
            }
            @Override public String getValue() {
                return (max < 0) ? ("0") : (String.valueOf(max));
            }
        });

        return pairs;
    }

    /**
     * Creates a new account user.  The user's initial state starts with email confirmation.
     *
     * @param email email address of the user
     * @param firstName first name of the user
     * @param lastName last name of the user
     * @param timeZone abbreviated time zone where the user observes time
     * @return the new user
     * @throws TrackviaApiException if the service fails to process the request
     * @throws TrackviaClientException if an error occurs outside the service, failing the request
     */
    public User createUser(final String email, final String firstName, final String lastName, final TimeZone timeZone)
            throws TrackviaApiException, TrackviaClientException {
        final Gson gson = this.recordAsMapGson;
        final Authorized<UserRecord> action = new Authorized<>(this);
        final UserRecord userRecord = action.execute(new Callable<UserRecord>() {
            @Override
            public UserRecord call() throws Exception {
                HttpClientContext context = HttpClientContext.create();

                return (UserRecord) execute(new CommandOverHttpPost<UserRecord>(context, TrackviaClient.this) {
                    @Override
                    public URI getApiRequestUri() throws URISyntaxException {
                        final String path = String.format("%s/openapi/users", TrackviaClient.this.baseUriPath);
                        return new URIBuilder()
                                .setScheme(TrackviaClient.this.scheme)
                                .setHost(TrackviaClient.this.hostname)
                                .setPort(TrackviaClient.this.port)
                                .setPath(path)
                                .setParameter(ACCESS_TOKEN_QUERY_PARAM, getAccessToken())
                                .setParameter(USER_KEY_QUERY_PARAM, getApiUserKey())
                                .build();
                    }

                    @Override
                    public UserRecord processResponseEntity(final HttpEntity entity) throws IOException {
                        Reader jsonReader = new InputStreamReader(entity.getContent());

                        return gson.fromJson(jsonReader, UserRecord.class);
                    }

                    @Override
                    public HttpEntity getApiRequestEntity() throws UnsupportedEncodingException {
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
     * Gets the applications available to the authenticated user.
     *
     * @return list of applications, which may be empty if none are available
     * @throws TrackviaApiException if the service fails to process this request
     * @throws TrackviaClientException if an error occurs outside the service, failing the request
     */
    public List<App> getApps() throws TrackviaApiException, TrackviaClientException {
        final Gson gson = this.recordAsMapGson;
        final Authorized<List<App>> action = new Authorized<>(this);

        return action.execute(new Callable<List<App>>() {
            @Override
            public List<App> call() throws Exception {
                HttpClientContext context = HttpClientContext.create();

                return (List<App>) execute(new CommandOverHttpGet<List<App>>(context, TrackviaClient.this) {
                    @Override
                    public URI getApiRequestUri() throws URISyntaxException {
                        final String path = String.format("%s/openapi/apps", TrackviaClient.this.baseUriPath);
                        return new URIBuilder()
                                .setScheme(TrackviaClient.this.scheme)
                                .setHost(TrackviaClient.this.hostname)
                                .setPort(TrackviaClient.this.port)
                                .setPath(path)
                                .setParameter(ACCESS_TOKEN_QUERY_PARAM, TrackviaClient.this.getAccessToken())
                                .setParameter(USER_KEY_QUERY_PARAM, TrackviaClient.this.getApiUserKey())
                                .build();
                    }

                    @Override
                    public List<App> processResponseEntity(final HttpEntity entity) throws IOException {
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
     * Gets a view by its name, if available to the authenticated user.
     *
     * @param name the case-sensitive view name to get
     * @return the view or null if not found
     * @throws TrackviaApiException if the service fails to process this request
     * @throws TrackviaClientException if an error occurs outside the service, failing the request
     *
     * @see #getViews() to get all available views
     */
    public View getView(final String name) throws TrackviaApiException, TrackviaClientException {
        List<View> views = getViews(name);

        return (views == null || views.isEmpty()) ? (null) : (views.get(0));
    }

    /**
     * Gets views available to the authenticated user.
     *
     * @return a list of views, which may be empty if none are available
     * @throws TrackviaApiException if the service fails to process this request
     * @throws TrackviaClientException if an error occurs outside the service, failing the request
     *
     * @see #getView(String) to get a view by its name
     */
    public List<View> getViews() throws TrackviaApiException {
        return getViews(null);
    }

    protected List<View> getViews(final String optionalName) throws TrackviaApiException, TrackviaClientException {
        final Gson gson = this.recordAsMapGson;
        final Authorized<List<View>> action = new Authorized<>(this);

        return action.execute(new Callable<List<View>>() {
            @SuppressWarnings("unchecked")
			@Override
            public List<View> call() throws Exception {
                HttpClientContext context = HttpClientContext.create();

                return (List<View>) execute(new CommandOverHttpGet<List<View>>(context, TrackviaClient.this) {
                    @Override
                    public URI getApiRequestUri() throws URISyntaxException {
                        final String basePath = String.format("%s/openapi/views", TrackviaClient.this.baseUriPath);
                        URIBuilder builder = new URIBuilder();
                        
                        builder.setScheme(TrackviaClient.this.scheme)
                                .setHost(TrackviaClient.this.hostname)
                                .setPort(TrackviaClient.this.port)
                                .setPath(basePath)
                                .setParameter(ACCESS_TOKEN_QUERY_PARAM, TrackviaClient.this.getAccessToken())
                                .setParameter(USER_KEY_QUERY_PARAM, TrackviaClient.this.getApiUserKey());
              
                        if (optionalName != null && !optionalName.isEmpty()) {
                        	builder.setParameter("name", optionalName);
                        }
                        
                        return builder.build();
                    }

                    @Override
                    public List<View> processResponseEntity(final HttpEntity entity) throws IOException {
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
     * Finds record matching given search criteria, returning application objects.
     *
     * A fairly simple name-matching strategy is used, to map the record's field values to
     * the given application's domain class.  The domain class must observe the Java Bean
     * setter/getter naming standard.  The matcher maps a Trackvia field (column) name to
     * the setXXX (XXX part) of the setter name, ignoring case.
     *
     * The mapping strategy is not configurable.
     *
     * For example:
     *
     * Trackvia column name             Domain class getter name
     * ----------------------------------------------------------
     * FIRSTNAME                        getFirstName
     *
     * The string "FirstNme" is extracted from the getFirstName method  name, and
     * equals the "FIRSTNAME" Trackvia column name.  Because the comparison is case
     * insensitive, the matcher considers these two as a successful match.
     *
     * @param domainClass return instances of this type (instead of as raw {@link RecordData}
     * @param viewId view identifier in which to search for records
     * @param q query substring used for a substring match against all of the user-defined fields
     * @param start the index (0 based) of the first user record, useful for paging
     * @param max retrieve no more than this many user records
     * @param <T> parameterized type matching the domainClass
     * @return a list of application objects matching the search criteria, which may be empty
     * @throws TrackviaApiException if the service fails to process this request
     * @throws TrackviaClientException if an error occurs outside the service, failing the request
     *
     * @see #findRecords(int, String, int, int) for Map<String, Object> records
     * @see trackvia.client.model.RecordData
     */
    public <T> DomainRecordSet<T> findRecords(final Class<T> domainClass, final int viewId, final String q,
            final int start, final int max) throws TrackviaApiException, TrackviaClientException {
        final ParameterizedType returnType = new DomainRecordSetType<T>(domainClass);
        final Gson deserializer = lookupDeserializer(domainClass, returnType);
        final Authorized<DomainRecordSet<T>> action = new Authorized<>(this);

        return action.execute(new Callable<DomainRecordSet<T>>() {
            @Override
            public DomainRecordSet<T> call() throws Exception {
                HttpClientContext context = HttpClientContext.create();
                return (DomainRecordSet<T>) execute(new CommandOverHttpGet<DomainRecordSet<T>>(context, TrackviaClient.this) {
                    @Override
                    public URI getApiRequestUri() throws URISyntaxException {
                        final List<NameValuePair> params = pairsFromFindRecordParams(q, start, max);
                        params.add(new NameValuePair() {
                            @Override public String getName() {
                                return ACCESS_TOKEN_QUERY_PARAM;
                            }
                            @Override public String getValue() {
                                return TrackviaClient.this.getAccessToken();
                            }
                        });
                        params.add(new NameValuePair() {
                            @Override public String getName() {
                                return USER_KEY_QUERY_PARAM;
                            }
                            @Override public String getValue() {
                                return TrackviaClient.this.getApiUserKey();
                            }
                        });
                        final String path = String.format("%s/openapi/views/%d/find", TrackviaClient.this.baseUriPath, viewId);
                        return new URIBuilder()
                                .setScheme(TrackviaClient.this.scheme)
                                .setHost(TrackviaClient.this.hostname)
                                .setPort(TrackviaClient.this.port)
                                .setPath(path)
                                .setParameters(params)
                                .build();
                    }

                    @Override
                    public DomainRecordSet<T> processResponseEntity(final HttpEntity entity) throws IOException {
                        Reader jsonReader = new InputStreamReader(entity.getContent());

                        return deserializer.fromJson(jsonReader, returnType);
                    }
                });
            }
        });
    }

    /**
     *
     * Finds record matching given search criteria, returning native records.
     *
     * @see trackvia.client.model.RecordDataDeserializer
     *
     * @param viewId view identifier in which to search for records
     * @param q query substring used for a substring match against all of the user-defined fields
     * @param start the index (0 based) of the first user record, useful for paging
     * @param max retrieve no more than this many user records
     * @return a list of application objects matching the search criteria, which may be empty
     * @throws TrackviaApiException if the service fails to process this request
     * @throws TrackviaClientException if an error occurs outside the service, failing the request
     *
     * @see #findRecords(Class, int, String, int, int) to specify an application class as the return type
     */
    public RecordSet findRecords(final int viewId, final String q, final int start, final int max)
            throws TrackviaApiException, TrackviaClientException {
        final Gson gson = this.recordAsMapGson;
        final Authorized<RecordSet> action = new Authorized<>(this);

        return action.execute(new Callable<RecordSet>() {
            @Override
            public RecordSet call() throws Exception {
                HttpClientContext context = HttpClientContext.create();
                return (RecordSet) execute(new CommandOverHttpGet<RecordSet>(context, TrackviaClient.this) {
                    @Override
                    public URI getApiRequestUri() throws URISyntaxException {
                        final List<NameValuePair> params = pairsFromFindRecordParams(q, start, max);
                        params.add(new NameValuePair() {
                            @Override public String getName() {
                                return ACCESS_TOKEN_QUERY_PARAM;
                            }
                            @Override public String getValue() {
                                return getAccessToken();
                            }
                        });
                        params.add(new NameValuePair() {
                            @Override public String getName() {
                                return USER_KEY_QUERY_PARAM;
                            }
                            @Override public String getValue() {
                                return TrackviaClient.this.getApiUserKey();
                            }
                        });
                        final String path = String.format("%s/openapi/views/%d/find", TrackviaClient.this.baseUriPath, viewId);
                        return new URIBuilder()
                                .setScheme(TrackviaClient.this.scheme)
                                .setHost(TrackviaClient.this.hostname)
                                .setPort(TrackviaClient.this.port)
                                .setPath(path)
                                .setParameters(params)
                                .build();
                    }

                    @Override
                    public RecordSet processResponseEntity(final HttpEntity entity) throws IOException {
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
            @Override public String getName() { return "q"; }
            @Override public String getValue() { return q; }
        });

        pairs.add(new NameValuePair() {
            @Override public String getName() { return "start"; }
            @Override public String getValue() { return (start < 0) ? ("0") : (String.valueOf(start)); }
        });

        pairs.add(new NameValuePair() {
            @Override public String getName() { return "max"; }
            @Override public String getValue() { return (max < 0) ? ("0") : (String.valueOf(max)); }
        });

        return pairs;
    }

    /**
     * Gets records available to the authenticated user in the given view.
     *
     * Use with small tables, when all records can be reasonably transferred in a single call.
     *
     * @param domainClass return instances of this type (instead of a raw record Map<String, Object>)
     * @param viewId view identifier in which to get records
     * @param <T> parameterized type matching the domainClass parameter
     * @return both field metadata and record data, as a record set
     * @throws TrackviaApiException if the service fails to process this request
     * @throws TrackviaClientException if an error occurs outside the service, failing the request
     *
     * @see #getRecords(int) for Map<String, Object> records (@see trackvia.client.model.RecordData)
     *
     */
    public <T> DomainRecordSet<T> getRecords(final Class<T> domainClass, final int viewId)
            throws TrackviaApiException, TrackviaClientException {
        final ParameterizedType returnType = new DomainRecordSetType<T>(domainClass);
        final Gson deserializer = lookupDeserializer(domainClass, returnType);
        final Authorized<DomainRecordSet<T>> action = new Authorized<>(this);

        return action.execute(new Callable<DomainRecordSet<T>>() {
            @Override
            public DomainRecordSet<T> call() throws Exception {
                HttpClientContext context = HttpClientContext.create();
                return (DomainRecordSet<T>) execute(new CommandOverHttpGet<DomainRecordSet<T>>(context, TrackviaClient.this) {
                    @Override
                    public URI getApiRequestUri() throws URISyntaxException {
                        final String path = String.format("%s/openapi/views/%d", TrackviaClient.this.baseUriPath, viewId);
                        return new URIBuilder()
                                .setScheme(TrackviaClient.this.scheme)
                                .setHost(TrackviaClient.this.hostname)
                                .setPort(TrackviaClient.this.port)
                                .setPath(path)
                                .setParameter(ACCESS_TOKEN_QUERY_PARAM, TrackviaClient.this.getAccessToken())
                                .setParameter(USER_KEY_QUERY_PARAM, TrackviaClient.this.getApiUserKey())
                                .build();
                    }

                    @Override
                    public DomainRecordSet<T> processResponseEntity(final HttpEntity entity) throws IOException {
                        Reader jsonReader = new InputStreamReader(entity.getContent());

                        return deserializer.fromJson(jsonReader, returnType);
                    }
                });
            }
        });
    }

    /**
     * Gets records available to the authenticated user in the given view.
     *
     * Use with small tables, when all records can be reasonably transferred in a single call.
     *
     * @param viewId view identifier in which to get records
     * @return both field metadata and record data, as a record set
     * @throws TrackviaApiException if the service fails to process this request
     * @throws TrackviaClientException if an error occurs outside the service, failing the request
     *
     * @see #getRecords(Class, int) for records as an application-defined class
     */
    public RecordSet getRecords(final int viewId) throws TrackviaApiException, TrackviaClientException {
        final Gson gson = this.recordAsMapGson;
        final Authorized<RecordSet> action = new Authorized<>(this);

        return action.execute(new Callable<RecordSet>() {
            @Override
            public RecordSet call() throws Exception {
                HttpClientContext context = HttpClientContext.create();
                return (RecordSet) execute(new CommandOverHttpGet<RecordSet>(context, TrackviaClient.this) {
                    @Override
                    public URI getApiRequestUri() throws URISyntaxException {
                        final String path = String.format("%s/openapi/views/%d", TrackviaClient.this.baseUriPath, viewId);
                        return new URIBuilder()
                                .setScheme(TrackviaClient.this.scheme)
                                .setHost(TrackviaClient.this.hostname)
                                .setPort(TrackviaClient.this.port)
                                .setPath(path)
                                .setParameter(ACCESS_TOKEN_QUERY_PARAM, TrackviaClient.this.getAccessToken())
                                .setParameter(USER_KEY_QUERY_PARAM, TrackviaClient.this.getApiUserKey())
                                .build();
                    }

                    @Override
                    public RecordSet processResponseEntity(final HttpEntity entity) throws IOException {
                        Reader jsonReader = new InputStreamReader(entity.getContent());

                        return gson.fromJson(jsonReader, RecordSet.class);
                    }
                });
            }
        });
    }

    /**
     * Gets a record.  The record must be available to the authenticated user in the given view.
     *
     * @param domainClass return instances of this type (instead of a raw record Map<String, Object>)
     * @param viewId view identifier in which to get records
     * @param recordId unique record identifier
     * @param <T> parameterized type matching the domainClass parameter
     * @return both field metadata and record data
     * @throws TrackviaApiException if the service fails to process this request
     * @throws TrackviaClientException if an error occurs outside the service, failing the request
     *
     * @see #getRecord(long, long) for a Map<String, Object> record data-type
     */
    public <T> DomainRecord<T> getRecord(final Class<T> domainClass, final long viewId, final long recordId)
            throws TrackviaApiException, TrackviaClientException {
        final ParameterizedType returnType = new DomainRecordType<T>(domainClass);
        final Gson deserializer = lookupDeserializer(domainClass, returnType);
        final Authorized<DomainRecord<T>> action = new Authorized<>(this);

        return action.execute(new Callable<DomainRecord<T>>() {
            @Override
            public DomainRecord<T> call() throws Exception {
                HttpClientContext context = HttpClientContext.create();
                return (DomainRecord<T>) execute(new CommandOverHttpGet<DomainRecord<T>>(context, TrackviaClient.this) {
                    @Override
                    public URI getApiRequestUri() throws URISyntaxException {
                        final String path = String.format("%s/openapi/views/%d/records/%d", TrackviaClient.this.baseUriPath,
                                viewId, recordId);
                        return new URIBuilder()
                                .setScheme(TrackviaClient.this.scheme)
                                .setHost(TrackviaClient.this.hostname)
                                .setPort(TrackviaClient.this.port)
                                .setPath(path)
                                .setParameter(ACCESS_TOKEN_QUERY_PARAM, TrackviaClient.this.getAccessToken())
                                .setParameter(USER_KEY_QUERY_PARAM, TrackviaClient.this.getApiUserKey())
                                .build();
                    }

                    @Override
                    public DomainRecord<T> processResponseEntity(final HttpEntity entity) throws IOException {
                        Reader jsonReader = new InputStreamReader(entity.getContent());

                        return deserializer.fromJson(jsonReader, returnType);
                    }
                });
            }
        });
    }

    /**
     * Gets a record.  The record must be available to the authenticated user in the given view.
     *
     * @param viewId view identifier in which to get records
     * @param recordId unique record identifier
     * @return both field metadata and record data, as RecordData (Map<String, Object>)
     * @throws TrackviaApiException if the service fails to process this request
     * @throws TrackviaClientException if an error occurs outside the service, failing the request
     *
     * @see #getRecord(Class, long, long) for a record as an application-defined class
     */
    public Record getRecord(final long viewId, final long recordId)
            throws TrackviaApiException, TrackviaClientException {
        final Gson gson = this.recordAsMapGson;
        final Authorized<Record> action = new Authorized<>(this);

        return action.execute(new Callable<Record>() {
            @Override
            public Record call() throws Exception {
                HttpClientContext context = HttpClientContext.create();
                return (Record) execute(new CommandOverHttpGet<Record>(context, TrackviaClient.this) {
                    @Override
                    public URI getApiRequestUri() throws URISyntaxException {
                        final String path = String.format("%s/openapi/views/%d/records/%d", TrackviaClient.this.baseUriPath,
                                viewId, recordId);
                        return new URIBuilder()
                                .setScheme(TrackviaClient.this.scheme)
                                .setHost(TrackviaClient.this.hostname)
                                .setPort(TrackviaClient.this.port)
                                .setPath(path)
                                .setParameter(ACCESS_TOKEN_QUERY_PARAM, TrackviaClient.this.getAccessToken())
                                .setParameter(USER_KEY_QUERY_PARAM, TrackviaClient.this.getApiUserKey())
                                .build();
                    }

                    @Override
                    public Record processResponseEntity(final HttpEntity entity) throws IOException {
                        Reader jsonReader = new InputStreamReader(entity.getContent());

                        return gson.fromJson(jsonReader, Record.class);
                    }
                });
            }
        });
    }

    /**
     * Creates a batch of records in a view accessible to the authenticated user.
     *
     * Record id field will be set to a newly assigned value.
     *
     * @param viewId view identifier in which to create the record batch
     * @param batch one or more records for creation
     * @param <T> user-provided parameterized type of the records in the batch
     * @return both field metadata and record data, as a record set of <T> objects
     * @throws TrackviaApiException if the service fails to process this request
     * @throws TrackviaClientException if an error occurs outside the service, failing the request
     *
     * @see #createRecords(int, trackvia.client.model.RecordDataBatch) for managing data as a {@link RecordData} map
     */
    public <T> DomainRecordSet<T> createRecords(final int viewId, final DomainRecordDataBatch<T> batch)
            throws TrackviaApiException, TrackviaClientException {
        // assertions
        if (batch == null || batch.getData() == null || batch.getData().size() == 0) {
            throw new IllegalArgumentException("Batch input is either empty or null");
        }

        final Class<T> domainClass = (Class<T>) batch.getData().get(0).getClass();
        final ParameterizedType returnType = new DomainRecordSetType<T>(domainClass);
        final ParameterizedType requestType = new DomainRecordDataBatchType<T>(domainClass);
        final Gson deserializer = lookupDeserializer(domainClass, returnType);
        final Gson serializer = lookupSerializer(domainClass, requestType);
        final Authorized<DomainRecordSet<T>> action = new Authorized<>(this);

        return action.execute(new Callable<DomainRecordSet<T>>() {
            @Override
            public DomainRecordSet<T> call() throws Exception {
                HttpClientContext context = HttpClientContext.create();
                return (DomainRecordSet<T>) execute(new CommandOverHttpPost<DomainRecordSet<T>>(context, TrackviaClient.this) {
                    @Override
                    public URI getApiRequestUri() throws URISyntaxException {
                        final String path = String.format("%s/openapi/views/%d/records", TrackviaClient.this.baseUriPath, viewId);
                        return new URIBuilder()
                                .setScheme(TrackviaClient.this.scheme)
                                .setHost(TrackviaClient.this.hostname)
                                .setPort(TrackviaClient.this.port)
                                .setPath(path)
                                .setParameter(ACCESS_TOKEN_QUERY_PARAM, TrackviaClient.this.getAccessToken())
                                .setParameter(USER_KEY_QUERY_PARAM, TrackviaClient.this.getApiUserKey())
                                .build();
                    }

                    @Override
                    public DomainRecordSet<T> processResponseEntity(final HttpEntity entity) throws IOException {
                        Reader jsonReader = new InputStreamReader(entity.getContent());

                        return deserializer.fromJson(jsonReader, returnType);
                    }

                    @Override
                    public HttpEntity getApiRequestEntity() throws UnsupportedEncodingException {
                        return new StringEntity(serializer.toJson(batch, requestType), ContentType.APPLICATION_JSON);
                    }
                });
            }
        });
    }
    
    /**
     * Helper method for when you want to create just one record
     * @param viewId
     * @param data
     * @return
     */
    public RecordData createRecord(long viewId, RecordData data){
    	RecordDataBatch batch = new RecordDataBatch();
    	LinkedList<RecordData> list = new LinkedList<RecordData>();
    	list.add(data);
    	batch.setData(list);
    	return createRecords(viewId, batch).getData().get(0);
    }

    /**
     * Creates a batch of records in a view accessible to the authenticated user.
     *
     * Record id field will be set to a newly assigned value.
     *
     * @param viewId view identifier in which to create the record batch
     * @param batch one or more records for creation
     * @return both field metadata and record data, as a raw Map<String, Object>
     * @throws TrackviaApiException if the service fails to process this request
     * @throws TrackviaClientException if an error occurs outside the service, failing the request
     *
     * @see #createRecords(int, trackvia.client.model.DomainRecordDataBatch) for managing
     * application-defined value objects
     * @see trackvia.client.model.RecordData for the Map<String, Object> return type
     */
    public RecordSet createRecords(final long viewId, final RecordDataBatch batch)
            throws TrackviaApiException, TrackviaClientException {
        final Gson gson = this.recordAsMapGson;
        final Authorized<RecordSet> action = new Authorized<>(this);

        return action.execute(new Callable<RecordSet>() {
            @Override
            public RecordSet call() throws Exception {
                HttpClientContext context = HttpClientContext.create();
                return (RecordSet) execute(new CommandOverHttpPost<RecordSet>(context, TrackviaClient.this) {
                    @Override
                    public URI getApiRequestUri() throws URISyntaxException {
                        final String path = String.format("%s/openapi/views/%d/records", TrackviaClient.this.baseUriPath, viewId);
                        return new URIBuilder()
                                .setScheme(TrackviaClient.this.scheme)
                                .setHost(TrackviaClient.this.hostname)
                                .setPort(TrackviaClient.this.port)
                                .setPath(path)
                                .setParameter(ACCESS_TOKEN_QUERY_PARAM, TrackviaClient.this.getAccessToken())
                                .setParameter(USER_KEY_QUERY_PARAM, TrackviaClient.this.getApiUserKey())
                                .build();
                    }

                    @Override
                    public RecordSet processResponseEntity(final HttpEntity entity) throws IOException {
                        Reader jsonReader = new InputStreamReader(entity.getContent());

                        return gson.fromJson(jsonReader, RecordSet.class);
                    }

                    @Override
                    public HttpEntity getApiRequestEntity() throws UnsupportedEncodingException {
                        return new StringEntity(gson.toJson(batch), ContentType.APPLICATION_JSON);
                    }
                });
            }
        });
    }

    /**
     * Updates a record in a view accessible to the authenticated user.
     *
     * Since the given record is an instance as an application-defined class, it will be mapped
     * to the specified {@link View} according to the serialization policy defined by
     * {@link trackvia.client.model.DomainRecordDataBatchSerializer}..
     *
     * @param viewId view identifier in which to update the record
     * @param recordId unique record identifier
     * @param data instance of an application-defined class, representing the record data
     * @param <T> parameterized type of the record data
     * @return both field metadata and record data, as a single record containing <T> data
     * @throws TrackviaApiException if the service fails to process this request
     * @throws TrackviaClientException if an error occurs outside the service, failing the request
     *
     * @see #updateRecord(int, long, trackvia.client.model.RecordData) to manage raw {@link RecordData} data
     */
    public <T> DomainRecord<T> updateRecord(final int viewId, final long recordId, final T data)
            throws TrackviaApiException, TrackviaClientException {
        // assertions
        if (data == null) {
            throw new IllegalArgumentException("Data must be non null");
        }

        final Class<T> domainClass = (Class<T>) data.getClass();
        final ParameterizedType returnType = new DomainRecordSetType<T>(domainClass);
        final ParameterizedType requestType = new DomainRecordDataBatchType<T>(domainClass);
        final Gson deserializer = lookupDeserializer(domainClass, returnType);
        final Gson serializer = lookupSerializer(domainClass, requestType);
        final Authorized<DomainRecordSet<T>> action = new Authorized<>(this);
        final DomainRecordSet<T> rs = action.execute(new Callable<DomainRecordSet<T>>() {
            @Override
            public DomainRecordSet<T> call() throws Exception {
                HttpClientContext context = HttpClientContext.create();
                return (DomainRecordSet<T>) execute(new CommandOverHttpPut<DomainRecordSet<T>>(context, TrackviaClient.this) {
                    @Override
                    public URI getApiRequestUri() throws URISyntaxException {
                        final String path = String.format("%s/openapi/views/%d/records/%s", TrackviaClient.this.baseUriPath,
                                viewId, recordId);
                        return new URIBuilder()
                                .setScheme(TrackviaClient.this.scheme)
                                .setHost(TrackviaClient.this.hostname)
                                .setPort(TrackviaClient.this.port)
                                .setPath(path)
                                .setParameter(ACCESS_TOKEN_QUERY_PARAM, TrackviaClient.this.getAccessToken())
                                .setParameter(USER_KEY_QUERY_PARAM, TrackviaClient.this.getApiUserKey())
                                .build();
                    }

                    @Override
                    public DomainRecordSet<T> processResponseEntity(final HttpEntity entity) throws IOException {
                        Reader jsonReader = new InputStreamReader(entity.getContent());

                        return deserializer.fromJson(jsonReader, returnType);
                    }

                    @Override
                    public HttpEntity getApiRequestEntity() throws UnsupportedEncodingException {
                        DomainRecordDataBatch<T> batchOfOne = new DomainRecordDataBatch<T>();
                        List<T> list = new ArrayList<T>();
                        list.add(data);
                        batchOfOne.setData(list);

                        return new StringEntity(serializer.toJson(batchOfOne), ContentType.APPLICATION_JSON);
                    }
                });
            }
        });

        // For updates, the structure between the Service APIs (batch) and client APIs (single) is somewhat different.
        // Exposing 'batch' in the Client API would be misleading since the Service API doesn't support batch
        // updates (yet).
        DomainRecord<T> updatedRecord = null;
        if (rs.getData() != null) {
            updatedRecord = new DomainRecord<T>(rs.getStructure(), rs.getData().get(0));
        }

        return updatedRecord;
    }

    /**
     * Updates a record in a view accessible to the authenticated user.
     *
     * The record's raw Map<String, Object> entries will map directly, one-for-one
     * with a {@link View}.  Entries that don't exist in the view are ignored.
     *
     * @param viewId view identifier in which to update the record
     * @param recordId unique record identifier
     * @param data instance of {@link RecordData}
     * @return the updated {@link Record}
     * @throws TrackviaApiException if the service fails to process this request
     * @throws TrackviaClientException if an error occurs outside the service, failing the request
     *
     */
    public Record updateRecord(final int viewId, final long recordId, final RecordData data)
            throws TrackviaApiException, TrackviaClientException {
    	data.put(Identifiable.INTERNAL_ID_FIELD_NAME, recordId);
        final Gson gson = this.recordAsMapGson;
        final Authorized<RecordSet> action = new Authorized<>(this);
        final RecordSet rs = action.execute(new Callable<RecordSet>() {
            @Override
            public RecordSet call() throws Exception {
                HttpClientContext context = HttpClientContext.create();
                return (RecordSet) execute(new CommandOverHttpPut<RecordSet>(context, TrackviaClient.this) {
                    @Override
                    public URI getApiRequestUri() throws URISyntaxException {
                        final String path = String.format("%s/openapi/views/%d/records/%s", TrackviaClient.this.baseUriPath,
                                viewId, data.getId());
                        return new URIBuilder()
                                .setScheme(TrackviaClient.this.scheme)
                                .setHost(TrackviaClient.this.hostname)
                                .setPort(TrackviaClient.this.port)
                                .setPath(path)
                                .setParameter(ACCESS_TOKEN_QUERY_PARAM, TrackviaClient.this.getAccessToken())
                                .setParameter(USER_KEY_QUERY_PARAM, TrackviaClient.this.getApiUserKey())
                                .build();
                    }

                    @Override
                    public RecordSet processResponseEntity(final HttpEntity entity) throws IOException {
                        Reader jsonReader = new InputStreamReader(entity.getContent());

                        return gson.fromJson(jsonReader, RecordSet.class);
                    }

                    @Override
                    public HttpEntity getApiRequestEntity() throws UnsupportedEncodingException {
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
     *
     * Deletes a record in the view of the authenticated user.
     *
     * @param viewId view identifier in which to delete the record
     * @param recordId unique record identifier
     * @throws TrackviaApiException if the service fails to process this request
     * @throws TrackviaClientException if an error occurs outside the service, failing the request
     */
    public void deleteRecord(final int viewId, final long recordId) throws TrackviaApiException, TrackviaClientException {
        final Gson gson = this.recordAsMapGson;
        final Authorized<Void> action = new Authorized<>(this);
        action.execute(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                HttpClientContext context = HttpClientContext.create();
                execute(new CommandOverHttpDelete<Void>(context, TrackviaClient.this) {
                    @Override
                    public URI getApiRequestUri() throws URISyntaxException {
                        final String path = String.format("%s/openapi/views/%d/records/%d", TrackviaClient.this.baseUriPath,
                                viewId, recordId);
                        return new URIBuilder()
                                .setScheme(TrackviaClient.this.scheme)
                                .setHost(TrackviaClient.this.hostname)
                                .setPort(TrackviaClient.this.port)
                                .setPath(path)
                                .setParameter(ACCESS_TOKEN_QUERY_PARAM, TrackviaClient.this.getAccessToken())
                                .setParameter(USER_KEY_QUERY_PARAM, TrackviaClient.this.getApiUserKey())
                                .build();
                    }

					@Override
					public Void processResponseEntity(HttpEntity entity) throws IOException {
						// no-op
						return null;
					}
                });
                return null;
            }
        });
    }

    /**
     * Adds a file to a record in the view of the authenticated user.
     *
     * Since this method returns the record as an application-defined type, that type must be
     * given as a parameter.  See 'domainClass' below.
     *
     * @param domainClass return instances of this type (instead of as raw {@link RecordData}
     * @param viewId view identifier in which to modify the record
     * @param recordId unique record identifier
     * @param fileName name of the file (named like the corresponding Trackvia "column")
     * @param filePath locally accessible path to the {@link java.nio.file.Path}
     * @return the updated {@link trackvia.client.model.DomainRecord}, including the file's identifier
     * @throws TrackviaApiException if the service fails to process this request
     * @throws TrackviaClientException if an error occurs outside the service, failing the request
     *
     */
    public <T> DomainRecord<T> addFile(final Class<T> domainClass, final int viewId, final long recordId,
            final String fileName, final Path filePath) throws TrackviaApiException, TrackviaClientException {
        final ParameterizedType returnType = new DomainRecordType<T>(domainClass);
        final Gson deserializer = lookupDeserializer(domainClass, returnType);
        final Authorized<DomainRecord<T>> action = new Authorized<>(this);
        return action.execute(new Callable<DomainRecord<T>>() {
            @Override
            public DomainRecord<T> call() throws Exception {
                HttpClientContext context = HttpClientContext.create();
                return (DomainRecord<T>) execute(new CommandOverHttpPost<DomainRecord<T>>(context, TrackviaClient.this) {
                    @Override
                    public URI getApiRequestUri() throws URISyntaxException {
                        final String path = String.format("%s/openapi/views/%d/records/%d/files/%s",
                                TrackviaClient.this.baseUriPath, viewId, recordId, fileName);
                        return new URIBuilder()
                                .setScheme(TrackviaClient.this.scheme)
                                .setHost(TrackviaClient.this.hostname)
                                .setPort(TrackviaClient.this.port)
                                .setPath(path)
                                .setParameter(ACCESS_TOKEN_QUERY_PARAM, TrackviaClient.this.getAccessToken())
                                .setParameter(USER_KEY_QUERY_PARAM, TrackviaClient.this.getApiUserKey())
                                .build();
                    }

                    @Override
                    public DomainRecord<T> processResponseEntity(final HttpEntity entity) throws IOException {
                        Reader jsonReader = new InputStreamReader(entity.getContent());

                        return deserializer.fromJson(jsonReader, returnType);
                    }

                    @Override
                    public HttpEntity getApiRequestEntity() throws UnsupportedEncodingException {
                        return MultipartEntityBuilder.create()
                                .addPart("file", new FileBody(filePath.toFile()))
                                .build();
                    }
                });
            }
        });
    }

    /**
     * Adds a file to a record in the view of the authenticated user.
     *
     * @param viewId view identifier in which to modify the record
     * @param recordId unique record identifier
     * @param fileName name of the file (named like the corresponding Trackvia "column")
     * @param filePath locally accessible path to the {@link java.nio.file.Path}
     * @return updated {@link trackvia.client.model.Record}, including the file's identifier
     * @throws TrackviaApiException if the service fails to process this request
     * @throws TrackviaClientException if an error occurs outside the service, failing the request
     *
     */
    public Record addFile(final int viewId, final long recordId, final String fileName, final Path filePath)
            throws TrackviaApiException, TrackviaClientException {
        final Gson gson = this.recordAsMapGson;
        final Authorized<Record> action = new Authorized<>(this);
        return action.execute(new Callable<Record>() {
            @Override
            public Record call() throws Exception {
                HttpClientContext context = HttpClientContext.create();
                return (Record) execute(new CommandOverHttpPost<Record>(context, TrackviaClient.this) {
                    @Override
                    public URI getApiRequestUri() throws URISyntaxException {
                        final String path = String.format("%s/openapi/views/%d/records/%d/files/%s",
                                TrackviaClient.this.baseUriPath, viewId, recordId, fileName);
                        return new URIBuilder()
                                .setScheme(TrackviaClient.this.scheme)
                                .setHost(TrackviaClient.this.hostname)
                                .setPort(TrackviaClient.this.port)
                                .setPath(path)
                                .setParameter(ACCESS_TOKEN_QUERY_PARAM, TrackviaClient.this.getAccessToken())
                                .setParameter(USER_KEY_QUERY_PARAM, TrackviaClient.this.getApiUserKey())
                                .build();
                    }

                    @Override
                    public Record processResponseEntity(final HttpEntity entity) throws IOException {
                        Reader jsonReader = new InputStreamReader(entity.getContent());

                        return gson.fromJson(jsonReader, Record.class);
                    }

                    @Override
                    public HttpEntity getApiRequestEntity() throws UnsupportedEncodingException {
                        return MultipartEntityBuilder.create()
                                .addPart("file", new FileBody(filePath.toFile()))
                                .build();
                    }
                });
            }
        });
    }
    
    
    
    /**
     * Adds a file to a record in the view of the authenticated user.
     * Uses an input stream as the file source
     *
     * @param viewId view identifier in which to modify the record
     * @param recordId unique record identifier
     * @param fileName name of the file (named like the corresponding Trackvia "column")
     * @param inputStream The input stream where the file data is located
     * @return updated {@link trackvia.client.model.Record}, including the file's identifier
     * @throws TrackviaApiException if the service fails to process this request
     * @throws TrackviaClientException if an error occurs outside the service, failing the request
     *
     */
    public Record addFile(final int viewId, final long recordId, final String fileName, final String inputFileName, final InputStream inputStream)
            throws TrackviaApiException, TrackviaClientException {
        final Gson gson = this.recordAsMapGson;
        final Authorized<Record> action = new Authorized<>(this);
        return action.execute(new Callable<Record>() {
            @Override
            public Record call() throws Exception {
                HttpClientContext context = HttpClientContext.create();
                return (Record) execute(new CommandOverHttpPost<Record>(context, TrackviaClient.this) {
                    @Override
                    public URI getApiRequestUri() throws URISyntaxException {
                        final String path = String.format("%s/openapi/views/%d/records/%d/files/%s",
                                TrackviaClient.this.baseUriPath, viewId, recordId, fileName);
                        return new URIBuilder()
                                .setScheme(TrackviaClient.this.scheme)
                                .setHost(TrackviaClient.this.hostname)
                                .setPort(TrackviaClient.this.port)
                                .setPath(path)
                                .setParameter(ACCESS_TOKEN_QUERY_PARAM, TrackviaClient.this.getAccessToken())
                                .setParameter(USER_KEY_QUERY_PARAM, TrackviaClient.this.getApiUserKey())
                                .build();
                    }

                    @Override
                    public Record processResponseEntity(final HttpEntity entity) throws IOException {
                        Reader jsonReader = new InputStreamReader(entity.getContent());

                        return gson.fromJson(jsonReader, Record.class);
                    }

                    @Override
                    public HttpEntity getApiRequestEntity() throws UnsupportedEncodingException {
                    	return MultipartEntityBuilder.create().addBinaryBody("file", inputStream, ContentType.DEFAULT_BINARY, inputFileName).build();
                    }
                });
            }
        });
    }

    /**
     * Gets file contents from a record in a view of the authenticated user.
     *
     * @param viewId view identifier in which to modify the record
     * @param recordId unique record identifier
     * @param fileName name of the file (named like the corresponding Trackvia "column")
     * @param filePath locally accessible path to the {@link java.nio.file.Path}
     * @throws TrackviaApiException if the service fails to process this request
     * @throws TrackviaClientException if an error occurs outside the service, failing the request
     *
     */
    public void getFile(final int viewId, final long recordId, final String fileName, final Path filePath)
            throws TrackviaApiException, TrackviaClientException {
        final Authorized<Void> action = new Authorized<>(this);

        action.execute(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                HttpClientContext context = HttpClientContext.create();

                // fail fast if the target 'filePath' already exists; no overwrites allowed.
                if (Files.exists(filePath)) {
                    throw new TrackviaClientException(String.format("Will not overwrite the file %s; aborting", filePath.toString()));
                }

                execute(new CommandOverHttpGet<Void>(context, TrackviaClient.this) {
                    @Override
                    public URI getApiRequestUri() throws URISyntaxException {
                        final String path = String.format("%s/openapi/views/%d/records/%d/files/%s",
                                TrackviaClient.this.baseUriPath, viewId, recordId, fileName);
                        return new URIBuilder()
                                .setScheme(TrackviaClient.this.scheme)
                                .setHost(TrackviaClient.this.hostname)
                                .setPort(TrackviaClient.this.port)
                                .setPath(path)
                                .setParameter(ACCESS_TOKEN_QUERY_PARAM, TrackviaClient.this.getAccessToken())
                                .setParameter(USER_KEY_QUERY_PARAM, TrackviaClient.this.getApiUserKey())
                                .build();
                    }

                    @Override
                    public Void processResponseEntity(final HttpEntity entity) throws IOException {
                        Files.copy(entity.getContent(), filePath);

                        return null;
                    }
                });

                return null;
            }
        });
    }

    /**
     * Deletes a file in a view of the authenticated user, if permissible.
     *
     * @param viewId view identifier in which to modify the record
     * @param recordId unique record identifier
     * @param fileName name of the file (named like the corresponding Trackvia "column")
     * @throws TrackviaApiException if the service fails to process this request
     * @throws TrackviaClientException if an error occurs outside the service, failing the request
     */
    public void deleteFile(final int viewId, final long recordId, final String fileName)
            throws TrackviaApiException, TrackviaClientException {
        final Authorized<Void> action = new Authorized<>(this);

        action.execute(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                HttpClientContext context = HttpClientContext.create();
                execute(new CommandOverHttpDelete<Void>(context, TrackviaClient.this) {
                    @Override
                    public URI getApiRequestUri() throws URISyntaxException {
                        final String path = String.format("%s/openapi/views/%d/records/%d/files/%s",
                                TrackviaClient.this.baseUriPath, viewId, recordId, fileName);
                        return new URIBuilder()
                                .setScheme(TrackviaClient.this.scheme)
                                .setHost(TrackviaClient.this.hostname)
                                .setPort(TrackviaClient.this.port)
                                .setPath(path)
                                .setParameter(ACCESS_TOKEN_QUERY_PARAM, TrackviaClient.this.getAccessToken())
                                .setParameter(USER_KEY_QUERY_PARAM, TrackviaClient.this.getApiUserKey())
                                .build();
                    }

					@Override
					public Void processResponseEntity(HttpEntity entity) throws IOException {
						//no-op
						return null;
					}
                });

                return null;
            }
        });
    }
}
