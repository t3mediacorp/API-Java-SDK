package trackvia.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.http.*;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ConnectionRequest;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import trackvia.client.model.*;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static trackvia.client.TestData.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TrackviaClientUnitTest {
    Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX").create();

    TrackviaClient client;
    CloseableHttpClient httpClient;
    HttpClientConnectionManager connectionManager;
    ConnectionRequest cr;
    HttpGet httpGet;
    StatusLine statusLine;
    CloseableHttpResponse response;
    HttpClientConnection clientConnection;
    HttpEntity responseEntity;

    @Before
    public void setUp() throws Exception {

        // Mock the Trackvia Service, using the HTTP Client.  Each individual test sets the
        // expected mocked response's HTTP-status code and if applicable, its response entity (body).

        httpClient = mock(CloseableHttpClient.class);
        connectionManager = mock(HttpClientConnectionManager.class);
        client = TrackviaClient.create(httpClient, connectionManager, "dontcare", "dontcare", "dontcare");
        cr = mock(ConnectionRequest.class);
        httpGet = mock(HttpGet.class);
        statusLine = mock(StatusLine.class);
        response = mock(CloseableHttpResponse.class);
        clientConnection = mock(HttpClientConnection.class);
        responseEntity = mock(HttpEntity.class);

        when(response.getStatusLine()).thenReturn(statusLine);
        when(response.getEntity()).thenReturn(responseEntity);
        when(httpClient.execute(any(HttpGet.class))).thenReturn(response);
        when(connectionManager.requestConnection(any(HttpRoute.class), any(Object.class))).thenReturn(cr);
        when(cr.get(anyInt(), any(TimeUnit.class))).thenReturn(clientConnection);
        when(clientConnection.isOpen()).thenReturn(false);
    }

    @Test
    public void testAuthorize() throws Exception {
        OAuth2Token token = new OAuth2Token();
        token.setValue("123");
        token.setAccess_token("123");
        token.setAccessToken("123");
        token.setExpiration(new Date(System.currentTimeMillis() + 60 * 1000L));
        token.setExpires_in(60L);
        token.setExpiresIn(60L);
        token.setRefresh_token("456");
        token.setRefreshToken(new OAuth2Token.RefreshToken("456", new Date(System.currentTimeMillis() + 60 * 60 * 1000L)));
        token.setScope(new String[]{"read", "write"});
        token.setTokenType(OAuth2Token.Type.bearer);
        ByteArrayInputStream contentInputStream = new ByteArrayInputStream(
                gson.toJson(token).getBytes());

        // form a valid http response with the expected json response body

        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        when(responseEntity.getContent()).thenReturn(contentInputStream);

        try {
            client.authorize("dontcare", "dontcare");

        } catch (TrackviaApiException e) {
            Assert.fail(String.format("authorize() threw an exception: %s", e.getApiError().description()));
        }

    }

    @Test
    public void testUnauthorized() throws Exception {
        ApiErrorResponse errorResponse = new ApiErrorResponse();
        errorResponse.setError(ApiError.InvalidGrant.code());
        errorResponse.setError_description(ApiError.InvalidGrant.description());
        ByteArrayInputStream contentInputStream = new ByteArrayInputStream(
                gson.toJson(errorResponse).getBytes());

        // form a valid http response with the expected json response body

        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_UNAUTHORIZED);
        when(responseEntity.getContent()).thenReturn(contentInputStream);

        try {
            client.authorize("dontcare", "dontcare");
            Assert.fail("authorization shouldn't have succeeded");
        } catch (TrackviaApiException e) {
            Assert.assertEquals(e.getApiError(), errorResponse.getApiError());
        }
    }

    @Test
    public void testGetApps() throws Exception {
        List<App> apps = Arrays.asList(new App[]{
                new App("1", "Contact Management - Construction"),
                new App("2", "Contact Management - IT")
        });
        ByteArrayInputStream contentInputStream = new ByteArrayInputStream(
                gson.toJson(apps).getBytes());

        // form a valid http response with the expected json response body

        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        when(responseEntity.getContent()).thenReturn(contentInputStream);

        List<App> appsResponse = client.getApps();

        Assert.assertNotNull(appsResponse);
        Assert.assertEquals(2, appsResponse.size());
        for (int i = 0; i < apps.size(); i++)
            Assert.assertEquals(apps.get(i), appsResponse.get(i));
    }

    @Test
    public void testGetViews() throws Exception {
        List<View> views = Arrays.asList(new View[]{
                new View("1", "Default Contacts View", "Default Contacts View"),
                new View("1", "Default Activities View", "Default Activities View")

        });
        ByteArrayInputStream contentInputStream = new ByteArrayInputStream(
                gson.toJson(views).getBytes());

        // form a valid http response with the expected json response body
        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        when(responseEntity.getContent()).thenReturn(contentInputStream);

        List<View> viewsResponse = client.getViews();

        Assert.assertNotNull(viewsResponse);
        Assert.assertEquals(2, viewsResponse.size());
        for (int i = 0; i < views.size(); i++)
            Assert.assertEquals(views.get(i), viewsResponse.get(i));
    }

    @Test
    public void testGetViewByName() throws Exception {
        List<View> views = Arrays.asList(new View[]{
                new View("1", "Default Contacts View", "Default Contacts View"),
                new View("2", "Default Activities View", "Default Activities View")
        });
        ByteArrayInputStream contentInputStream = new ByteArrayInputStream(
                gson.toJson(views).getBytes());

        // positive test
        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        when(responseEntity.getContent()).thenReturn(contentInputStream);

        View viewResponse = client.getView("Default Contacts View");

        Assert.assertNotNull(viewResponse);
        Assert.assertEquals(views.get(0), viewResponse);

        // negative test
        when(responseEntity.getContent()).thenReturn(new ByteArrayInputStream("".getBytes()));

        viewResponse = client.getView("does not exist");

        Assert.assertNull(viewResponse);
    }

    @Test
    public void testGetRecordsAsDomainClass() throws Exception {
        RecordSet rs = Unit.getUnitTestRecordSet3();
        ByteArrayInputStream contentInputStream = new ByteArrayInputStream(
                gson.toJson(rs).getBytes());

        // form a valid http response with the expected json response body
        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        when(responseEntity.getContent()).thenReturn(contentInputStream);

        DomainRecordSet<Unit.Contact> responseRs = client.getRecords(Unit.Contact.class, 1);

        Assert.assertNotNull(responseRs);
        Assert.assertEquals(1, responseRs.getTotalCount());
        Assert.assertEquals(1, responseRs.getData().size());
    }

    @Test
    public void testGetRecords() throws Exception {
        // Create mocked data
        RecordSet rs = Unit.getUnitTestRecordSet1();
        ByteArrayInputStream contentInputStream = new ByteArrayInputStream(
                gson.toJson(rs).getBytes());

        // form a valid http response with the expected json response body
        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        when(responseEntity.getContent()).thenReturn(contentInputStream);

        RecordSet rsResponse = client.getRecords(1);

        Assert.assertNotNull(rsResponse);
        Assert.assertEquals(2, rsResponse.getTotalCount());
        Assert.assertEquals(2, rsResponse.getData().size());

        for (int i = 0; i < 2; i++) {
            RecordData rd1 = rsResponse.getData().get(i);
            RecordData rd2 = rsResponse.getData().get(i);

            Assert.assertEquals(rd1, rd2);
        }
    }

    @Test
    public void testGetRecordAsDomainClass() throws Exception {
        Record record = Unit.getUnitTestRecord1();
        ByteArrayInputStream contentInputStream = new ByteArrayInputStream(
                gson.toJson(record).getBytes());

        // Form a valid http response with the expected json response body
        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        when(responseEntity.getContent()).thenReturn(contentInputStream);

        DomainRecord<Unit.Contact> contactRecord = client.getRecord(Unit.Contact.class, 1, 1);

        Assert.assertNotNull(contactRecord);
        Assert.assertNotNull(contactRecord.getData());
    }

    @Test
    public void testGetRecord() throws Exception {
        Record record = Unit.getUnitTestRecord1();
        ByteArrayInputStream contentInputStream = new ByteArrayInputStream(
                gson.toJson(record).getBytes());

        // Form a valid http response with the expected json response body
        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        when(responseEntity.getContent()).thenReturn(contentInputStream);

        Record recordResponse = client.getRecord(1, 1);

        Assert.assertNotNull(recordResponse);
        Assert.assertEquals(record.getRecordId(), recordResponse.getRecordId());
    }

    @Test
    public void testFindRecords() throws Exception {
        RecordSet rs = Unit.getUnitTestRecordSet1();
        ByteArrayInputStream contentInputStream = new ByteArrayInputStream(
                gson.toJson(rs).getBytes());

        // form a valid http response with the expected json response body
        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        when(responseEntity.getContent()).thenReturn(contentInputStream);

        RecordSet rsResponse = client.findRecords(1, "dontcare", 0, 25);

        Assert.assertNotNull(rsResponse);
        Assert.assertEquals(2, rsResponse.getTotalCount());
        Assert.assertEquals(2, rsResponse.getData().size());

        for (int i = 0; i < 2; i++) {
            RecordData rd1 = rs.getData().get(i);
            RecordData rd2 = rs.getData().get(i);

            Assert.assertEquals(rd1, rd2);
        }
    }

    @Test
    public void testFindRecordsAsDomainClass() throws Exception {
        RecordSet rs = Unit.getUnitTestRecordSet3();
        ByteArrayInputStream contentInputStream = new ByteArrayInputStream(
                gson.toJson(rs).getBytes());

        // form a valid http response with the expected json response body
        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        when(responseEntity.getContent()).thenReturn(contentInputStream);

        DomainRecordSet<Unit.Contact> responseRs = client.findRecords(Unit.Contact.class, 1, "1", 0, 25);

        List<Unit.Contact> contacts = responseRs.getData();

        Assert.assertNotNull(contacts);
        Assert.assertEquals(1, contacts.size());
    }

    @Test
    public void testGetUsers() throws Exception {
        UserRecordSet userRecordSet = Unit.getUnitTestUserRecordSet1();
        ByteArrayInputStream contentInputStream = new ByteArrayInputStream(
                gson.toJson(userRecordSet).getBytes());

        // form a valid http response with the expected json response body
        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        when(responseEntity.getContent()).thenReturn(contentInputStream);

        List<User> usersResponse = client.getUsers(0, 25);

        Assert.assertNotNull(usersResponse);
        Assert.assertEquals(1, usersResponse.size());
        Assert.assertEquals(userRecordSet.getData().get(0), usersResponse.get(0));
    }

    @Test
    public void testCreateUsers() throws Exception {
        UserRecord record = Unit.getUnitTestUserRecord1();
        ByteArrayInputStream contentInputStream = new ByteArrayInputStream(
                gson.toJson(record).getBytes());

        // form a valid http response with the expected json response body
        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_CREATED);
        when(responseEntity.getContent()).thenReturn(contentInputStream);

        User userResponse = client.createUser(record.getData().getEmail(), record.getData().getFirstName(),
                record.getData().getFirstName(), TimeZone.getTimeZone(record.getData().getTimezone()));

        Assert.assertNotNull(userResponse);
        Assert.assertEquals(record.getData(), userResponse);
    }

    @Test
    public void testCreateRecordBatchAsDomainClass() throws Exception {
        Record rawRecord = TestData.Unit.getUnitTestRecord1();
        Unit.Contact contact = TestData.Unit.getUnitTestContact1();
        List<Unit.Contact> contacts = Arrays.asList(new Unit.Contact[]{contact});
        DomainRecordSet<Unit.Contact> rs = new DomainRecordSet<Unit.Contact>(
                rawRecord.getStructure(), contacts, 1);
        ByteArrayInputStream contentInputStream = new ByteArrayInputStream(
                gson.toJson(rs).getBytes());

        // form a valid http response with the expected json response body
        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_CREATED);
        when(responseEntity.getContent()).thenReturn(contentInputStream);

        DomainRecordDataBatch<Unit.Contact> batch = new DomainRecordDataBatch<>(contacts);
        DomainRecordSet<Unit.Contact> responseRs = client.createRecords(1, batch);

        Assert.assertNotNull(responseRs);
        Assert.assertEquals(1, responseRs.getTotalCount());
    }

    @Test
    public void testCreateRecordBatch() throws Exception {
        RecordSet rs = Unit.getUnitTestRecordSet1();
        RecordDataBatch batch = new RecordDataBatch(rs.getData());
        ByteArrayInputStream contentInputStream = new ByteArrayInputStream(
                gson.toJson(rs).getBytes());

        // form a valid http response with the expected json response body
        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_CREATED);
        when(responseEntity.getContent()).thenReturn(contentInputStream);

        RecordSet rsResponse = client.createRecords(1, batch);

        Assert.assertNotNull(rsResponse);
        Assert.assertEquals(2, rsResponse.getTotalCount());
        Assert.assertEquals(2, rsResponse.getData().size());

        for (int i = 0; i < 2; i++) {
            RecordData rd1 = rs.getData().get(i);
            RecordData rd2 = rs.getData().get(i);

            Assert.assertEquals(rd1, rd2);
        }
    }

    @Test
    public void testUpdateRecordAsDomainClass() throws Exception {
        RecordSet rs = TestData.Unit.getUnitTestRecordSet3();
        ByteArrayInputStream contentInputStream = new ByteArrayInputStream(
                gson.toJson(rs).getBytes());

        // Form a valid http response with the expected json response body
        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        when(responseEntity.getContent()).thenReturn(contentInputStream);

        Unit.Contact contact = Unit.getUnitTestContact1();

        DomainRecord<Unit.Contact> responseRecord = client.updateRecord(1, 1, contact);

        Assert.assertNotNull(responseRecord);
        Assert.assertEquals(contact.getId(), responseRecord.getData().getId());
    }

    @Test
    public void testUpdateRecord() throws Exception {
        RecordSet rs = Unit.getUnitTestRecordSet2();
        ByteArrayInputStream contentInputStream = new ByteArrayInputStream(
                gson.toJson(rs).getBytes());

        // Form a valid http response with the expected json response body
        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        when(responseEntity.getContent()).thenReturn(contentInputStream);

        Record updateResponse = client.updateRecord(1, 1, rs.getData().get(0));

        Assert.assertNotNull(updateResponse);
        Assert.assertEquals(rs.getData().get(0), updateResponse.getData());
    }

    @Test
    public void testDeleteRecord() throws Exception {
        // Form a valid http response with the expected json response body
        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_NO_CONTENT);

        // no exception indicates success
        client.deleteRecord(1, 1);
    }

    @Test
    public void testAddFileUsingDomainClass() throws Exception {
        Path filePath = null;

        try {
            // create a temporary text file for upload
            filePath = Files.createTempFile("trackvia-client-file", "");
            Files.write(filePath, "This is only a test".getBytes());

            Record record = Unit.getUnitTestRecord1();
            ByteArrayInputStream contentInputStream = new ByteArrayInputStream(
                    gson.toJson(record).getBytes());

            // Form a valid http response with the expected json response body
            when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
            when(responseEntity.getContent()).thenReturn(contentInputStream);

            DomainRecord<Unit.Contact> recordResponse = client.addFile(Unit.Contact.class, 1, 1, "Test File",
                    Paths.get("/path/to/file"));

            Unit.Contact contact = Unit.getUnitTestContact1();

            Assert.assertNotNull(recordResponse);
            Assert.assertEquals(contact.getId(), recordResponse.getData().getId());

        } finally {
            if (filePath != null && Files.exists(filePath)) Files.delete(filePath);
        }
    }

    @Test
    public void testAddFile() throws Exception {
        Path filePath = null;

        try {
            // create a temporary text file for upload
            filePath = Files.createTempFile("trackvia-client-file", "");
            Files.write(filePath, "This is only a test".getBytes());

            Record record = Unit.getUnitTestRecord1();
            ByteArrayInputStream contentInputStream = new ByteArrayInputStream(
                    gson.toJson(record).getBytes());

            // Form a valid http response with the expected json response body
            when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
            when(responseEntity.getContent()).thenReturn(contentInputStream);

            Record recordResponse = client.addFile(1, 1, "Test File", Paths.get("/path/to/file"));

            Assert.assertNotNull(recordResponse);
            Assert.assertEquals(record.getRecordId(), recordResponse.getRecordId());

        } finally {
            if (filePath != null && Files.exists(filePath)) Files.delete(filePath);
        }
    }

    @Test
    public void testGetFile() throws Exception {
        String pathToFile = String.format("./trackvia-client-test-%d", System.currentTimeMillis() / 1000L);
        Path filePath = Paths.get(pathToFile);

        try {
            ByteArrayInputStream contentInputStream = new ByteArrayInputStream(
                    "This is only a test".getBytes());

            // Form a valid http response with the expected json response body
            when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
            when(responseEntity.getContent()).thenReturn(contentInputStream);

            client.getFile(1, 1, "Test File", filePath);

            Assert.assertEquals("This is only a test", new String(Files.readAllBytes(filePath)));

        } finally {
            if (Files.exists(filePath)) Files.delete(filePath);
        }
    }

    @Test
    public void testDeleteFile() throws Exception {

        // Form a valid http response with the expected json response body
        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_NO_CONTENT);

        client.deleteFile(1, 1, "Test File");
    }
    
    @Test
    public void testGetRecordWithPoint() throws Exception {
    	Record record = Unit.getUnitTestRecord1();
        ByteArrayInputStream contentInputStream = new ByteArrayInputStream(
                gson.toJson(record).getBytes());

        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        when(responseEntity.getContent()).thenReturn(contentInputStream);

        Record recordResponse = client.getRecord(1, 1);

        Assert.assertNotNull(recordResponse);
        
        Point testPoint = (Point) record.getData().get("TestPoint");
        Assert.assertEquals(testPoint, recordResponse.getData().get("TestPoint"));
    }
    
    @Test
    public void testCreateRecordWithPoint() throws Exception {
    	RecordSet rs = Unit.getUnitTestRecordSet4();
        RecordDataBatch batch = new RecordDataBatch(rs.getData());
        ByteArrayInputStream contentInputStream = new ByteArrayInputStream(
                gson.toJson(rs).getBytes());

        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_CREATED);
        when(responseEntity.getContent()).thenReturn(contentInputStream);

        RecordSet rsResponse = client.createRecords(1, batch);

        Assert.assertNotNull(rsResponse);
        Assert.assertEquals(1, rsResponse.getTotalCount());
        Assert.assertEquals(1, rsResponse.getData().size());
    }
    
    @Test
    public void testUpdateRecordWithPoint() throws Exception {
        RecordSet rs = Unit.getUnitTestRecordSet4();
        ByteArrayInputStream contentInputStream = new ByteArrayInputStream(
                gson.toJson(rs).getBytes());

        // Form a valid http response with the expected json response body
        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        when(responseEntity.getContent()).thenReturn(contentInputStream);

        Record updateResponse = client.updateRecord(1, 1, rs.getData().get(0));

        Assert.assertNotNull(updateResponse);
        Assert.assertEquals(rs.getData().get(0), updateResponse.getData());
    }
}
