package trackvia.client;

import trackvia.client.model.*;

import java.util.*;

public class TestData {
    static class Integration {
        // Table name
        public static final String INTEGRATION_TEST_TABLE_NAME = "Test";

        // View names
        public static final String INTEGRATION_TEST_VIEW_NAME = "Test View";

        // Column names
        public static final String COLUMN_ID = "id";
        public static final String COLUMN_FIRST_NAME = "firstName";
        public static final String COLUMN_LAST_NAME = "lastName";
        public static final String COLUMN_EMAIL = "email";
        public static final String COLUMN_PHONE = "phone";
        public static final String COLUMN_FILE1 = "file1";

        public static Record getIntegrationTestRecord1() {
            RecordData data;

            data = new RecordData();

            data.put(COLUMN_ID, "1");
            data.put(COLUMN_FIRST_NAME, "Rodney");
            data.put(COLUMN_LAST_NAME, "Dangerously");
            data.put(COLUMN_EMAIL, "rod@danger-never-dies.com");
            data.put(COLUMN_PHONE, "212-555-1212");
            data.put(COLUMN_FILE1, null);

            return new Record(data);
        }

        public static Record getIntegrationTestRecord2() {
            RecordData data;

            data = new RecordData();

            data.put(COLUMN_ID, "2");
            data.put(COLUMN_FIRST_NAME, "Larry");
            data.put(COLUMN_LAST_NAME, "Lounging");
            data.put(COLUMN_EMAIL, "larry@easy-does.us");
            data.put(COLUMN_PHONE, "303-555-1212");
            data.put(COLUMN_FILE1, null);

            return new Record(data);
        }
    }

    static class Unit {
        public static Record getUnitTestRecord1() {
            List<FieldMetadata> structure = Arrays.asList(new FieldMetadata[]{
                    new FieldMetadata("Contact Name", TrackviaDataType.ShortAnswer.type(), true, false, Collections.<String>emptyList()),
                    new FieldMetadata("Company Name", TrackviaDataType.ShortAnswer.type(), true, false, Collections.<String>emptyList()),
                    new FieldMetadata("Locations", TrackviaDataType.CheckBox.type(), true, false, Arrays.asList(new String[]{"CO", "CA"})),
                    new FieldMetadata("Is Customer", TrackviaDataType.Number.type(), true, false, Collections.<String>emptyList()),
                    new FieldMetadata("Revenue", TrackviaDataType.Currency.type(), true, false, Collections.<String>emptyList()),
                    new FieldMetadata("Revenue Captured", TrackviaDataType.Percentage.type(), true, false, Collections.<String>emptyList()),
                    new FieldMetadata("Test File", TrackviaDataType.Document.type(), true, false, Collections.<String>emptyList()),
            });
            RecordData data = new RecordData();
            data.put("id", 1L);
            data.put("Contact Name", "James Randall");
            data.put("Company Name", "Cryogenic Futures");
            data.put("Locations", Arrays.asList(new String[]{"CA"}));
            data.put("Is Customer", true);
            data.put("Revenue", 100000.0);
            data.put("Revenue Captured", 0.35);
            data.put("Test File", 222L);

            return new Record(structure, data);
        }

        public static UserRecordSet getUnitTestUserRecordSet1() {
            List<FieldMetadata> structure = getUnitTestUserFieldMetaData1();
            User u1 = getUnitTestUser1();
            List<User> data = new ArrayList<User>(Arrays.asList(new User[]{u1}));

            return new UserRecordSet(structure, data, 1);
        }

        public static UserRecord getUnitTestUserRecord1() {
            return new UserRecord(getUnitTestUserFieldMetaData1(), getUnitTestUser1());
        }

        public static List<FieldMetadata> getUnitTestUserFieldMetaData1() {
            return Arrays.asList(new FieldMetadata[]{
                    new FieldMetadata("First Name", TrackviaDataType.ShortAnswer.type(), true, false, Collections.<String>emptyList()),
                    new FieldMetadata("Last Name", TrackviaDataType.ShortAnswer.type(), true, false, Collections.<String>emptyList()),
                    new FieldMetadata("Status", TrackviaDataType.ShortAnswer.type(), true, false, Collections.<String>emptyList()),
                    new FieldMetadata("Email", TrackviaDataType.Email.type(), true, false, Collections.<String>emptyList()),
                    new FieldMetadata("Timezone", TrackviaDataType.ShortAnswer.type(), true, false, Collections.<String>emptyList()),
                    new FieldMetadata("Created", TrackviaDataType.DateTime.type(), true, false, Collections.<String>emptyList()),
            });
        }

        public static User getUnitTestUser1() {
            return new User("Joe", "Black", "ACTIVE", "joe@gmail.com", "MST", new Date(System.currentTimeMillis()));
        }

        public static RecordSet getUnitTestRecordSet1() {
            List<FieldMetadata> structure = Arrays.asList(new FieldMetadata[]{
                    new FieldMetadata("Contact Name", TrackviaDataType.ShortAnswer.type(), true, false, Collections.<String>emptyList()),
                    new FieldMetadata("Company Name", TrackviaDataType.ShortAnswer.type(), true, false, Collections.<String>emptyList())});
            List<RecordData> data = Arrays.asList(new RecordData[]{new RecordData(), new RecordData()});

            data.get(0).put("id", 1L);
            data.get(0).put("Contact Name", "James Randall");
            data.get(0).put("Company Name", "Cryogenic Futures");

            data.get(1).put("id", 2L);
            data.get(1).put("Contact Name", "Simon Black");
            data.get(1).put("Company Name", "Sunshine Industries");

            return new RecordSet(structure, data, 2);
        }

        public static RecordSet getUnitTestRecordSet2() {
            List<FieldMetadata> structure = Arrays.asList(new FieldMetadata[]{
                    new FieldMetadata("Contact Name", TrackviaDataType.ShortAnswer.type(), true, false, Collections.<String>emptyList()),
                    new FieldMetadata("Company Name", TrackviaDataType.ShortAnswer.type(), true, false, Collections.<String>emptyList())});
            List<RecordData> data = Arrays.asList(new RecordData[]{new RecordData()});

            data.get(0).put("id", 1L);
            data.get(0).put("Contact Name", "James Randall");
            data.get(0).put("Company Name", "Cryogenic Futures");

            return new RecordSet(structure, data, 1);
        }
    }
}
