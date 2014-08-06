package trackvia.client;

import trackvia.client.model.*;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static trackvia.client.TestData.*;

@RunWith(JUnit4.class)
public class TrackviaClientIntegrationTest {
    static final String TEST_URI = "go.api.trackvia.com";
    static final String TEST_USER = "tom.michaud@gmail.com";
    static final String TEST_PASSWORD = "password";
    static final int TEST_VIEW_ID = 13;

    @Test
    public void testAuthorized() throws Exception {
        TrackviaClient client = TrackviaClient.create(TEST_URI, TEST_USER, TEST_PASSWORD);

        Assert.assertNotNull(client);
    }

    @Test
    public void testUnauthorized() throws Exception {
        try {
            TrackviaClient client = TrackviaClient.create(TEST_URI, "fake_user", "fake_password");
        } catch (TrackviaApiException e) {
            Assert.assertEquals(e.getApiError(), ApiError.InvalidGrant);
        }
    }

    @Test
    public void testGetApps() throws Exception {
        TrackviaClient client = TrackviaClient.create(TEST_URI, TEST_USER, TEST_PASSWORD);
        List<App> apps = client.getApps();

        Assert.assertNotNull(apps);
        Assert.assertFalse(apps.isEmpty());
    }

    @Test
    public void testGetViews() throws Exception {
        TrackviaClient client = TrackviaClient.create(TEST_URI, TEST_USER, TEST_PASSWORD);
        List<View> views = client.getViews();

        Assert.assertNotNull(views);
        Assert.assertFalse(views.isEmpty());
    }

    @Test
    public void testGetRecords() throws Exception {
        TrackviaClient client = TrackviaClient.create(TEST_URI, TEST_USER, TEST_PASSWORD);
        RecordSet rs = client.getRecords(TEST_VIEW_ID);

        Assert.assertNotNull(rs);
        Assert.assertNotNull(rs.getData());
        Assert.assertTrue(rs.getData().size() > 0);
    }

    @Test
    public void testGetRecord() throws Exception {
        TrackviaClient client = TrackviaClient.create(TEST_URI, TEST_USER, TEST_PASSWORD);
        RecordData createdRecord = createOneTestRecord();
        Record record = client.getRecord(TEST_VIEW_ID, createdRecord.getRecordId());

        Assert.assertNotNull(record);
    }

    @Test
    public void testFindRecords() throws Exception {
        TrackviaClient client = TrackviaClient.create(TEST_URI, TEST_USER, TEST_PASSWORD);
        RecordSet rs = client.findRecords(TEST_VIEW_ID, "1", 0, 25);

        Assert.assertNotNull(rs);
        Assert.assertNotNull(rs.getData());
        Assert.assertTrue(rs.getData().size() > 0);
    }

    @Test
    public void testGetUsers() throws Exception {
        TrackviaClient client = TrackviaClient.create(TEST_URI, TEST_USER, TEST_PASSWORD);
        List<User> users = client.getUsers(0, 25);

        Assert.assertNotNull(users);
        Assert.assertTrue(users.size() > 0);
    }

    @Test
    public void testCreateUser() throws Exception {
        final String email = String.format("tester-%d@we-love-testing.org", System.currentTimeMillis());
        final String first = "UnitTest";
        final String last = "User";

        TrackviaClient client = TrackviaClient.create(TEST_URI, TEST_USER, TEST_PASSWORD);
        User user = client.createUser(email, first, last, TimeZone.getDefault());

        Assert.assertNotNull(user);
    }

    RecordData createOneTestRecord() throws Exception {
        RecordDataBatch batch = new RecordDataBatch();
        List<RecordData> records = new ArrayList<RecordData>();

        records.add(Integration.getIntegrationTestRecord1().getData());

        long id = System.currentTimeMillis();
        records.get(0).put(Integration.COLUMN_ID, id);

        batch.setData(records);

        TrackviaClient client = TrackviaClient.create(TEST_URI, TEST_USER, TEST_PASSWORD);
        RecordSet rs = client.createRecords(TEST_VIEW_ID, batch);

        Assert.assertNotNull(rs);
        Assert.assertNotNull(rs.getData());
        Assert.assertTrue(rs.getData().size() == 1);

        return rs.getData().get(0);
    }

    @Test
    public void testCreateRecordBatch() throws Exception {
        RecordDataBatch batch = new RecordDataBatch();
        List<RecordData> records = new ArrayList<RecordData>();

        records.add(Integration.getIntegrationTestRecord1().getData());
        records.add(Integration.getIntegrationTestRecord2().getData());

        long id = System.currentTimeMillis();
        records.get(0).put(Integration.COLUMN_ID, id);
        records.get(1).put(Integration.COLUMN_ID, id + 1);

        batch.setData(records);

        TrackviaClient client = TrackviaClient.create(TEST_URI, TEST_USER, TEST_PASSWORD);
        RecordSet rs = client.createRecords(TEST_VIEW_ID, batch);

        Assert.assertNotNull(rs);
        Assert.assertNotNull(rs.getData());
        Assert.assertTrue(rs.getData().size() > 0);
    }

    @Test
    public void testUpdateRecord() throws Exception {
        TrackviaClient client = TrackviaClient.create(TEST_URI, TEST_USER, TEST_PASSWORD);

        RecordSet rs = client.createRecords(TEST_VIEW_ID, batchOfOne());

        Assert.assertNotNull(rs);
        Assert.assertEquals(1, rs.getTotalCount());
        Assert.assertEquals(1, rs.getData().size());

        RecordData recordData = rs.getData().get(0);

        // change a field and update
        RecordData updatedData = new RecordData(recordData);
        updatedData.put(Integration.COLUMN_FIRST_NAME, UUID.randomUUID().toString());
        Record updatedRecord = client.updateRecord(TEST_VIEW_ID, updatedData);

        Assert.assertNotNull(updatedRecord);
        Assert.assertEquals(updatedRecord.getData().get(Integration.COLUMN_FIRST_NAME),
                updatedData.get(Integration.COLUMN_FIRST_NAME));
    }

    private RecordDataBatch batchOfOne() {
        RecordDataBatch batch = new RecordDataBatch();
        List<RecordData> records = new ArrayList<RecordData>();

        records.add(Integration.getIntegrationTestRecord1().getData());

        records.get(0).put(Integration.COLUMN_ID, System.currentTimeMillis()/1000);

        batch.setData(records);

        return batch;
    }
    @Test
    public void testDeleteRecord() throws Exception {
        TrackviaClient client = TrackviaClient.create(TEST_URI, TEST_USER, TEST_PASSWORD);
        RecordSet rs = client.createRecords(TEST_VIEW_ID, batchOfOne());

        Assert.assertNotNull(rs);
        Assert.assertEquals(1, rs.getTotalCount());
        Assert.assertEquals(1, rs.getData().size());

        RecordData recordData = rs.getData().get(0);
        client.deleteRecord(TEST_VIEW_ID, recordData.getRecordId());

        // verify
        Record record = client.getRecord(TEST_VIEW_ID, recordData.getRecordId());
        Assert.assertNull(record);
    }

    @Test
    public void testAddFile() throws Exception {
        TrackviaClient client = TrackviaClient.create(TEST_URI, TEST_USER, TEST_PASSWORD);
        Path filePath = null;

        try {
            RecordData testRecord = createOneTestRecord();

            // create a temporary text file for upload
            filePath = Files.createTempFile("trackvia-client-file", "txt");
            Files.write(filePath, "This is only a test".getBytes());

            Record record = client.addFile(TEST_VIEW_ID, testRecord.getRecordId(), Integration.COLUMN_FILE1, filePath);

            Assert.assertNotNull(record);
            Assert.assertNotNull(record.getData().get(Integration.COLUMN_FILE1));
        } finally {
            if (filePath != null && Files.exists(filePath)) Files.delete(filePath);
            if (client != null) client.shutdown();
        }
    }

    @Test
    public void testGetFile() throws Exception {
        TrackviaClient client = TrackviaClient.create(TEST_URI, TEST_USER, TEST_PASSWORD);
        String pathToFile = String.format("./trackvia-client-test-%d", System.currentTimeMillis() / 1000L);
        Path filePath = Paths.get(pathToFile);
        RecordData testRecord = createOneTestRecord();

        client.getFile(TEST_VIEW_ID, testRecord.getRecordId(), Integration.COLUMN_FILE1, filePath);
    }

    @Test
    public void testDeleteFile() throws Exception {
        TrackviaClient client = TrackviaClient.create(TEST_URI, TEST_USER, TEST_PASSWORD);
        Path filePath = null;

        try {
            // create a temporary text file for upload
            filePath = Files.createTempFile("trackvia-client-file", "txt");
            Files.write(filePath, "This is only a test".getBytes());

            RecordData originalRecord = createOneTestRecord();

            Record updatedRecord = client.addFile(TEST_VIEW_ID, originalRecord.getRecordId(), Integration.COLUMN_FILE1, filePath);

            Assert.assertNotNull(updatedRecord);
            Assert.assertNotNull(updatedRecord.getData().get(Integration.COLUMN_FILE1));

            client.deleteFile(TEST_VIEW_ID, updatedRecord.getRecordId(), Integration.COLUMN_FILE1);

        } finally {
            if (filePath != null && Files.exists(filePath)) Files.delete(filePath);
            if (client != null) client.shutdown();
        }
    }
}
