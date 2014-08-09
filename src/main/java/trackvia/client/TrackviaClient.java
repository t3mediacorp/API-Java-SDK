package trackvia.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

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
 *      TrackviaClient client = TrackviaClient.create("go.api.trackvia.com", "myuser", "mypassword");
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
 * other tables, your application code will handle these "foreign key" references as additional retrieval of the
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

    public static final String DEFAULT_SCHEME = "https";
    public static final int DEFAULT_PORT = 80;

    private CloseableHttpClient httpClient;
    private HttpClientConnectionManager connectionManager;
    private String scheme = DEFAULT_SCHEME;
    private String hostname;
    private int port = DEFAULT_PORT;
    private OAuth2Token lastGoodToken;
    private Gson recordAsMapGson;
    private Map<String, Gson> typeToGsonMap = new HashMap<String, Gson>();

    private TrackviaClient() {}

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

        trackviaClient.recordAsMapGson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX")
                .registerTypeAdapter(RecordData.class, new RecordDataDeserializer())
                .create();

        // Obtain user credentials to use the API.  authorize() throws TrackviaApiException if the
        // authorization process fails for any reason.  Let it propagate.
        OAuth2Token token = trackviaClient.authorize(username, password);

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
                .create();

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

    protected <T> Gson lookupGsonForDomainClass(final Class<T> domainClass, final Class<?> parameterClass) {
        String key = String.format("%s-%s", domainClass.getName(), parameterClass.getName());
        Gson gson = typeToGsonMap.get(key);
        if (gson == null) {
            gson = new GsonBuilder()
                    .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX")
                    .registerTypeAdapter(parameterClass, new DomainRecordSetDeserializer<>(domainClass))
                    .create();
            typeToGsonMap.put(key, gson);
        }
        return gson;
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
        final Gson gson = this.recordAsMapGson;
        final HttpClientContext context = HttpClientContext.create();
        final OAuth2Token token = (OAuth2Token) execute(new CommandOverHttpGet<OAuth2Token>(context) {
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
            public OAuth2Token processResponseEntity(final HttpEntity entity) throws IOException {
                Reader jsonReader = new InputStreamReader(entity.getContent());

                return gson.fromJson(jsonReader, OAuth2Token.class);
            }
        });

        setAuthToken(token);

        return token;
    }

    public OAuth2Token authorize(final String username, final String password) throws TrackviaApiException {
        final Gson gson = this.recordAsMapGson;
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
            public OAuth2Token processResponseEntity(final HttpEntity entity) throws IOException {
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
        final Gson gson = this.recordAsMapGson;
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
        final Gson gson = this.recordAsMapGson;
        final Authorized<UserRecord> action = new Authorized<>(this);
        final UserRecord userRecord = action.execute(new Callable<UserRecord>() {
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
     * Get a user's authorized apps.
     */
    public List<App> getApps() throws TrackviaApiException {
        final Gson gson = this.recordAsMapGson;
        final Authorized<List<App>> action = new Authorized<>(this);

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
     * Gets an authorized view.
     *
     * @param name the case-sensitive view name to get
     * @return the view or null if it's not found
     */
    public View getView(final String name) throws TrackviaApiException {
        List<View> views = getViews(null);
        return (views == null || views.isEmpty()) ? (null) : (views.get(0));
    }

    /**
     * Gets all authorized views.
     *
     * @return a list of all views
     * @see View
     * @see #getView(String)
     */
    public List<View> getViews() throws TrackviaApiException {
        return getViews(null);
    }

    protected List<View> getViews(final String optionalName) throws TrackviaApiException {
        final Gson gson = this.recordAsMapGson;
        final Authorized<List<View>> action = new Authorized<>(this);

        return action.execute(new Callable<List<View>>() {
            @Override
            public List<View> call() throws Exception {
                HttpClientContext context = HttpClientContext.create();

                return (List<View>) execute(new CommandOverHttpGet<List<View>>(context) {
                    @Override
                    public URI getApiRequestUri() throws URISyntaxException {
                        final String path = (optionalName == null || optionalName.isEmpty()) ? ("/openapi/views") :
                                (String.format("/openapi/views?name=%s", optionalName));
                        return new URIBuilder()
                                .setScheme("https")
                                .setHost(TrackviaClient.this.hostname)
                                .setPath(path)
                                .setParameter("access_token", getAccessToken())
                                .build();
                    }

                    @Override
                    public List<View> processResponseEntity(final HttpEntity entity) throws IOException {
                        Reader jsonReader = new InputStreamReader(entity.getContent());
                        Type responseType = new TypeToken<List<View>>() {}.getType();

                        return gson.fromJson(jsonReader, responseType);
                    }
                });
            }
        });
    }

    // Not so pretty but type erasure forces some not-so-"classy" acts.
    public <T> List<T> findRecords(final Class<T> domainClass, final int viewId, final String q,
            final int start, final int max) throws TrackviaApiException {
        final ParameterizedType returnType = new DomainRecordSetType<T>(domainClass);
        final Gson domainGson = lookupGsonForDomainClass(domainClass, DomainRecordSetType.class);
        final Authorized<DomainRecordSet<T>> action = new Authorized<>(this);
        final DomainRecordSet<T> rs = action.execute(new Callable<DomainRecordSet<T>>() {
            @Override
            public DomainRecordSet<T> call() throws Exception {
                HttpClientContext context = HttpClientContext.create();
                return (DomainRecordSet<T>) execute(new CommandOverHttpGet<DomainRecordSet<T>>(context) {
                    @Override
                    public URI getApiRequestUri() throws URISyntaxException {
                        List<NameValuePair> params = pairsFromFindRecordParams(q, start, max);
                        params.add(new NameValuePair() {
                            @Override public String getName() { return "access_token"; }
                            @Override public String getValue() { return getAccessToken(); }
                        });
                        return new URIBuilder()
                                .setScheme("https")
                                .setHost(TrackviaClient.this.hostname)
                                .setPath(String.format("/openapi/views/%d/find", viewId))
                                .setParameters(params)
                                .build();
                    }

                    @Override
                    public DomainRecordSet<T> processResponseEntity(final HttpEntity entity) throws IOException {
                        Reader jsonReader = new InputStreamReader(entity.getContent());

                        return domainGson.fromJson(jsonReader, returnType);
                    }
                });
            }
        });

        return rs.getData();
    }

    /**
     * Find matching records in a given view.
     */
    public RecordSet findRecords(final int viewId, final String q, final int start, final int max) throws TrackviaApiException {
        final Gson gson = this.recordAsMapGson;
        final Authorized<RecordSet> action = new Authorized<>(this);

        return action.execute(new Callable<RecordSet>() {
            @Override
            public RecordSet call() throws Exception {
                HttpClientContext context = HttpClientContext.create();
                return (RecordSet) execute(new CommandOverHttpGet<RecordSet>(context) {
                    @Override
                    public URI getApiRequestUri() throws URISyntaxException {
                        List<NameValuePair> params = pairsFromFindRecordParams(q, start, max);
                        params.add(new NameValuePair() {
                            @Override public String getName() { return "access_token"; }
                            @Override public String getValue() { return getAccessToken(); }
                        });
                        return new URIBuilder()
                                .setScheme("https")
                                .setHost(TrackviaClient.this.hostname)
                                .setPath(String.format("/openapi/views/%d/find", viewId))
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
            @Override public String getValue() { return (max < start) ? ("50") : (max > 100) ? ("100") : (String.valueOf(max)); }
        });

        return pairs;
    }

    public <T> DomainRecordSet<T> getRecords(final Class<T> domainClass, final int viewId) throws TrackviaApiException {
        final ParameterizedType returnType = new DomainRecordSetType<T>(domainClass);
        final Gson domainGson = lookupGsonForDomainClass(domainClass, DomainRecordSetType.class);
        final Authorized<DomainRecordSet<T>> action = new Authorized<>(this);

        return action.execute(new Callable<DomainRecordSet<T>>() {
            @Override
            public DomainRecordSet<T> call() throws Exception {
                HttpClientContext context = HttpClientContext.create();
                return (DomainRecordSet<T>) execute(new CommandOverHttpGet<DomainRecordSet<T>>(context) {
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
                    public DomainRecordSet<T> processResponseEntity(final HttpEntity entity) throws IOException {
                        Reader jsonReader = new InputStreamReader(entity.getContent());

                        return domainGson.fromJson(jsonReader, returnType);
                    }
                });
            }
        });
    }

    /**
     * Gets all records in a given view.
     */
    public RecordSet getRecords(final int viewId) throws TrackviaApiException {
        final Gson gson = this.recordAsMapGson;
        final Authorized<RecordSet> action = new Authorized<>(this);

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
                    public RecordSet processResponseEntity(final HttpEntity entity) throws IOException {
                        Reader jsonReader = new InputStreamReader(entity.getContent());

                        return gson.fromJson(jsonReader, RecordSet.class);
                    }
                });
            }
        });
    }

    public <T> DomainRecord<T> getRecord(final Class<T> domainClass, final long viewId, final long recordId)
            throws TrackviaApiException {
        final ParameterizedType returnType = new DomainRecordType<T>(domainClass);
        final Gson domainGson = lookupGsonForDomainClass(domainClass, DomainRecordType.class);
        final Authorized<DomainRecord<T>> action = new Authorized<>(this);

        return action.execute(new Callable<DomainRecord<T>>() {
            @Override
            public DomainRecord<T> call() throws Exception {
                HttpClientContext context = HttpClientContext.create();
                return (DomainRecord<T>) execute(new CommandOverHttpGet<DomainRecord<T>>(context) {
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
                    public DomainRecord<T> processResponseEntity(final HttpEntity entity) throws IOException {
                        Reader jsonReader = new InputStreamReader(entity.getContent());

                        return domainGson.fromJson(jsonReader, returnType);
                    }
                });
            }
        });
    }

    /**
     * Gets a specific record in a given view.
     */
    public Record getRecord(final long viewId, final long recordId) throws TrackviaApiException {
        final Gson gson = this.recordAsMapGson;
        final Authorized<Record> action = new Authorized<>(this);

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
                    public Record processResponseEntity(final HttpEntity entity) throws IOException {
                        Reader jsonReader = new InputStreamReader(entity.getContent());

                        return gson.fromJson(jsonReader, Record.class);
                    }
                });
            }
        });
    }

    public <T> DomainRecordSet<T> createRecords(final int viewId, final DomainRecordDataBatch<T> batch) {
        // assertions
        if (batch == null || batch.getData() == null || batch.getData().size() == 0) {
            throw new IllegalArgumentException("Batch input is either empty or null");
        }

        final Class<T> domainClass = (Class<T>) batch.getData().get(0).getClass();
        final ParameterizedType returnType = new DomainRecordSetType<T>(domainClass);
        final Gson domainGson = lookupGsonForDomainClass(domainClass, DomainRecordSetType.class);
        final Authorized<DomainRecordSet<T>> action = new Authorized<>(this);

        return action.execute(new Callable<DomainRecordSet<T>>() {
            @Override
            public DomainRecordSet<T> call() throws Exception {
                HttpClientContext context = HttpClientContext.create();
                return (DomainRecordSet<T>) execute(new CommandOverHttpPost<DomainRecordSet<T>>(context) {
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
                    public DomainRecordSet<T> processResponseEntity(final HttpEntity entity) throws IOException {
                        Reader jsonReader = new InputStreamReader(entity.getContent());

                        return domainGson.fromJson(jsonReader, returnType);
                    }

                    @Override
                    public HttpEntity getApiRequestEntity() throws UnsupportedEncodingException {
                        return new StringEntity(domainGson.toJson(batch), ContentType.APPLICATION_JSON);
                    }
                });
            }
        });
    }

    /**
     * Creates records.
     */
    public RecordSet createRecords(final int viewId, final RecordDataBatch batch) {
        final Gson gson = this.recordAsMapGson;
        final Authorized<RecordSet> action = new Authorized<>(this);

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

    public <T> DomainRecord<T> updateRecord(final int viewId, final long recordId, final T data) {
        // assertions
        if (data == null) {
            throw new IllegalArgumentException("Data must be non null");
        }

        final Class<T> domainClass = (Class<T>) data.getClass();
        final ParameterizedType returnType = new DomainRecordSetType<T>(domainClass);
        final Gson domainGson = lookupGsonForDomainClass(domainClass, DomainRecordSetType.class);
        final Authorized<DomainRecordSet<T>> action = new Authorized<>(this);
        final DomainRecordSet<T> rs = action.execute(new Callable<DomainRecordSet<T>>() {
            @Override
            public DomainRecordSet<T> call() throws Exception {
                HttpClientContext context = HttpClientContext.create();
                return (DomainRecordSet<T>) execute(new CommandOverHttpPut<DomainRecordSet<T>>(context) {
                    @Override
                    public URI getApiRequestUri() throws URISyntaxException {
                        return new URIBuilder()
                                .setScheme("https")
                                .setHost(TrackviaClient.this.hostname)
                                .setPath(String.format("/openapi/views/%d/records/%s", viewId, recordId))
                                .setParameter("access_token", getAccessToken())
                                .build();
                    }

                    @Override
                    public DomainRecordSet<T> processResponseEntity(final HttpEntity entity) throws IOException {
                        Reader jsonReader = new InputStreamReader(entity.getContent());

                        return domainGson.fromJson(jsonReader, returnType);
                    }

                    @Override
                    public HttpEntity getApiRequestEntity() throws UnsupportedEncodingException {
                        DomainRecordDataBatch<T> batchOfOne = new DomainRecordDataBatch<T>();
                        List<T> list = new ArrayList<T>();
                        list.add(data);
                        batchOfOne.setData(list);

                        return new StringEntity(domainGson.toJson(batchOfOne), ContentType.APPLICATION_JSON);
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
     * Updates a record.
     */
    public Record updateRecord(final int viewId, final RecordData data) {
        final Gson gson = this.recordAsMapGson;
        final Authorized<RecordSet> action = new Authorized<>(this);
        final RecordSet rs = action.execute(new Callable<RecordSet>() {
            @Override
            public RecordSet call() throws Exception {
                HttpClientContext context = HttpClientContext.create();
                return (RecordSet) execute(new CommandOverHttpPut<RecordSet>(context) {
                    @Override
                    public URI getApiRequestUri() throws URISyntaxException {
                        return new URIBuilder()
                                .setScheme("https")
                                .setHost(TrackviaClient.this.hostname)
                                .setPath(String.format("/openapi/views/%d/records/%s", viewId, data.getRecordId()))
                                .setParameter("access_token", getAccessToken())
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
     * Deletes a record.
     */
    public void deleteRecord(final int viewId, final long recordId) {
        final Gson gson = this.recordAsMapGson;
        final Authorized<Void> action = new Authorized<>(this);
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

    public <T> DomainRecord<T> addFile(final Class<T> domainClass, final int viewId, final long recordId,
            final String columnName, final Path filePath) {
        final ParameterizedType returnType = new DomainRecordType<T>(domainClass);
        final Gson domainGson = lookupGsonForDomainClass(domainClass, DomainRecordType.class);
        final Authorized<DomainRecord<T>> action = new Authorized<>(this);
        return action.execute(new Callable<DomainRecord<T>>() {
            @Override
            public DomainRecord<T> call() throws Exception {
                HttpClientContext context = HttpClientContext.create();
                return (DomainRecord<T>) execute(new CommandOverHttpPost<DomainRecord<T>>(context) {
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
                    public DomainRecord<T> processResponseEntity(final HttpEntity entity) throws IOException {
                        Reader jsonReader = new InputStreamReader(entity.getContent());

                        return domainGson.fromJson(jsonReader, returnType);
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
     * Adds file contents (image or document) to a record, if permissible.
     */
    public Record addFile(final int viewId, final long recordId, final String fileName, final Path filePath) throws TrackviaApiException {
        final Gson gson = this.recordAsMapGson;
        final Authorized<Record> action = new Authorized<>(this);
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
                                .setPath(String.format("/openapi/views/%d/records/%d/files/%s", viewId, recordId, fileName))
                                .setParameter("access_token", getAccessToken())
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
     * Gets file contents from a record, if permissible.
     */
    public void getFile(final int viewId, final long recordId, final String fileName, final Path filePath) {
        final Authorized<Void> action = new Authorized<>(this);

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
                                .setPath(String.format("/openapi/views/%d/records/%d/files/%s", viewId, recordId, fileName))
                                .setParameter("access_token", getAccessToken())
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
     * Deletes a file, if permissible.
     */
    public void deleteFile(final int viewId, final long recordId, final String columnName) {
        final Authorized<Void> action = new Authorized<>(this);

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
