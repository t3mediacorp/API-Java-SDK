package trackvia.client;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import trackvia.client.TestData.Integration;
import trackvia.client.model.ApiError;
import trackvia.client.model.App;
import trackvia.client.model.Record;
import trackvia.client.model.RecordData;
import trackvia.client.model.RecordDataBatch;
import trackvia.client.model.RecordSet;
import trackvia.client.model.User;
import trackvia.client.model.View;

@RunWith(JUnit4.class)
public class TrackviaClientIntegrationTest {
    static final String TEST_SCHEME = "http";
    static final String TEST_HOST = "localhost";
    static final int TEST_PORT = 8080;
    static final String TEST_BASE_URI = "/xvia";
    static final String TEST_USER = "bpmsols@gmail.com";
    static final String TEST_PASSWORD = "password";
    static final String TEST_API_USER_KEY = "12345";
    static final int TEST_VIEW_ID = 5;

    TrackviaClient getClient() throws Exception {
        final boolean localEnv = true;

        if (localEnv) {
            return TrackviaClient.create(TEST_BASE_URI, TEST_SCHEME, TEST_HOST, TEST_PORT, TEST_USER, TEST_PASSWORD,
                    TEST_API_USER_KEY);
        } else {
            throw new UnsupportedOperationException("Can't create an integration test for a non-local environment");
        }
    }

    @Ignore
    @Test
    public void testAuthorized() throws Exception {
        // throws TrackviaApiException if access is not granted
        TrackviaClient client = getClient();
    }

    @Ignore
    @Test
    public void testUnauthorized() throws Exception {
        try {
            TrackviaClient client = TrackviaClient.create(TEST_BASE_URI, TEST_SCHEME, TEST_HOST, TEST_PORT,
                    "fake_user", "fake_password", "fake api user key");
        } catch (TrackviaApiException e) {
            Assert.assertEquals(e.getApiError(), ApiError.InvalidGrant);
        }
    }

    @Ignore
    @Test
    public void testGetApps() throws Exception {
        TrackviaClient client = getClient();
        List<App> apps = client.getApps();

        Assert.assertNotNull(apps);
        Assert.assertFalse(apps.isEmpty());
    }

    @Ignore
    @Test
    public void testGetViews() throws Exception {
        TrackviaClient client = getClient();
        List<View> views = client.getViews();

        Assert.assertNotNull(views);
        Assert.assertFalse(views.isEmpty());
    }

    @Ignore
    @Test
    public void testGetRecords() throws Exception {
        TrackviaClient client = getClient();
        RecordSet rs = client.getRecords(TEST_VIEW_ID);

        Assert.assertNotNull(rs);
        Assert.assertNotNull(rs.getData());
        Assert.assertTrue(rs.getData().size() > 0);
    }

    @Ignore
    @Test
    public void testGetRecord() throws Exception {
        TrackviaClient client = getClient();
        RecordData createdRecord = createOneTestRecord();
        Record record = client.getRecord(TEST_VIEW_ID, createdRecord.getId());

        Assert.assertNotNull(record);
    }

    @Ignore
    @Test
    public void testFindRecords() throws Exception {
        TrackviaClient client = getClient();
        RecordSet rs = client.findRecords(TEST_VIEW_ID, "1", 0, 25);

        Assert.assertNotNull(rs);
        Assert.assertNotNull(rs.getData());
        Assert.assertTrue(rs.getData().size() > 0);
    }

    @Ignore
    @Test
    public void testGetUsers() throws Exception {
        TrackviaClient client = getClient();
        List<User> users = client.getUsers(0, 25);

        Assert.assertNotNull(users);
        Assert.assertTrue(users.size() > 0);
    }

    @Ignore
    @Test
    public void testCreateUser() throws Exception {
        final String email = String.format("tester-%d@we-love-testing.org", System.currentTimeMillis());
        final String first = "UnitTest";
        final String last = "User";

        TrackviaClient client = getClient();
        User user = client.createUser(email, first, last, TimeZone.getDefault());

        Assert.assertNotNull(user);
    }

    
    private RecordData createOneTestRecord() throws Exception {
        RecordDataBatch batch = new RecordDataBatch();
        List<RecordData> records = new ArrayList<RecordData>();

        records.add(Integration.getIntegrationTestRecord1().getData());

        batch.setData(records);

        TrackviaClient client = getClient();
        RecordSet rs = client.createRecords(TEST_VIEW_ID, batch);

        Assert.assertNotNull(rs);
        Assert.assertNotNull(rs.getData());
        Assert.assertTrue(rs.getData().size() == 1);

        return rs.getData().get(0);
    }

    @Ignore
    @Test
    public void testCreateRecordBatch() throws Exception {
        RecordDataBatch batch = new RecordDataBatch();
        List<RecordData> records = new ArrayList<RecordData>();

        records.add(Integration.getIntegrationTestRecord1().getData());
        records.add(Integration.getIntegrationTestRecord2().getData());

        batch.setData(records);

        TrackviaClient client = getClient();
        RecordSet rs = client.createRecords(TEST_VIEW_ID, batch);

        Assert.assertNotNull(rs);
        Assert.assertNotNull(rs.getData());
        Assert.assertTrue(rs.getData().size() > 0);
    }

    @Ignore
    @Test
    public void testUpdateRecord() throws Exception {
        TrackviaClient client = getClient();

        RecordSet rs = client.createRecords(TEST_VIEW_ID, batchOfOne());

        Assert.assertNotNull(rs);
        Assert.assertEquals(1, rs.getTotalCount());
        Assert.assertEquals(1, rs.getData().size());

        RecordData recordData = rs.getData().get(0);

        // change a field and update
        long recordId = recordData.getId();
        RecordData updatedData = new RecordData(recordData);
        updatedData.put(Integration.COLUMN_FIRST_NAME, UUID.randomUUID().toString());
        Record updatedRecord = client.updateRecord(TEST_VIEW_ID, recordId, updatedData);

        Assert.assertNotNull(updatedRecord);
        Assert.assertEquals(updatedRecord.getData().get(Integration.COLUMN_FIRST_NAME),
                updatedData.get(Integration.COLUMN_FIRST_NAME));
    }


    private RecordDataBatch batchOfOne() {
        RecordDataBatch batch = new RecordDataBatch();
        List<RecordData> records = new ArrayList<RecordData>();

        records.add(Integration.getIntegrationTestRecord1().getData());

        batch.setData(records);

        return batch;
    }
    
    @Ignore
    @Test
    public void testDeleteRecord() throws Exception {
        TrackviaClient client = getClient();
        RecordSet rs = client.createRecords(TEST_VIEW_ID, batchOfOne());

        Assert.assertNotNull(rs);
        Assert.assertEquals(1, rs.getTotalCount());
        Assert.assertEquals(1, rs.getData().size());

        RecordData recordData = rs.getData().get(0);
        client.deleteRecord(TEST_VIEW_ID, recordData.getId());

        // verify
        Record record = client.getRecord(TEST_VIEW_ID, recordData.getId());
        Assert.assertNull(record);
    }

    @Ignore
    @Test
    public void testAddFile() throws Exception {
        TrackviaClient client = getClient();
        Path filePath = null;

        try {
            RecordData testRecord = createOneTestRecord();

            // create a temporary text file for upload
            filePath = Files.createTempFile("trackvia-client-file", "txt");
            Files.write(filePath, "This is only a test".getBytes());

            Record record = client.addFile(TEST_VIEW_ID, testRecord.getId(), Integration.COLUMN_FILE1, filePath);

            Assert.assertNotNull(record);
            Assert.assertNotNull(record.getData().get(Integration.COLUMN_FILE1));
        } finally {
            if (filePath != null && Files.exists(filePath)) Files.delete(filePath);
            if (client != null) client.shutdown();
        }
    }

    @Ignore
    @Test
    public void testGetFile() throws Exception {
        TrackviaClient client = getClient();
        String pathToFile = String.format("./trackvia-client-test-%d", System.currentTimeMillis() / 1000L);
        Path filePath = Paths.get(pathToFile);
        RecordData testRecord = createOneTestRecord();

        client.getFile(TEST_VIEW_ID, testRecord.getId(), Integration.COLUMN_FILE1, filePath);
    }

    @Ignore
    @Test
    public void testDeleteFile() throws Exception {
        TrackviaClient client = getClient();
        Path filePath = null;

        try {
            // create a temporary text file for upload
            filePath = Files.createTempFile("trackvia-client-file", "txt");
            Files.write(filePath, "This is only a test".getBytes());

            RecordData originalRecord = createOneTestRecord();

            Record updatedRecord = client.addFile(TEST_VIEW_ID, originalRecord.getId(), Integration.COLUMN_FILE1, filePath);

            Assert.assertNotNull(updatedRecord);
            Assert.assertNotNull(updatedRecord.getData().get(Integration.COLUMN_FILE1));

            client.deleteFile(TEST_VIEW_ID, updatedRecord.getRecordId(), Integration.COLUMN_FILE1);

        } finally {
            if (filePath != null && Files.exists(filePath)) Files.delete(filePath);
            if (client != null) client.shutdown();
        }
    }
}
