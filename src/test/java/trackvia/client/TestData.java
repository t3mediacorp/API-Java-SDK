package trackvia.client;

import trackvia.client.model.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class TestData {
    public static class Integration {
        // Table name
        public static final String INTEGRATION_TEST_TABLE_NAME = "Test";

        // View names
        public static final String INTEGRATION_TEST_VIEW_NAME = "Test View";

        // Column names
        public static final String COLUMN_FIRST_NAME = "firstName";
        public static final String COLUMN_LAST_NAME = "lastName";
        public static final String COLUMN_EMAIL = "email";
        public static final String COLUMN_PHONE = "phone";
        public static final String COLUMN_FILE1 = "file1";

        public static Record getIntegrationTestRecord1() {
            RecordData data;

            data = new RecordData();

            data.put(Identifiable.INTERNAL_ID_FIELD_NAME, "1");
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

            data.put(Identifiable.INTERNAL_ID_FIELD_NAME, "2");
            data.put(COLUMN_FIRST_NAME, "Larry");
            data.put(COLUMN_LAST_NAME, "Lounging");
            data.put(COLUMN_EMAIL, "larry@easy-does.us");
            data.put(COLUMN_PHONE, "303-555-1212");
            data.put(COLUMN_FILE1, null);

            return new Record(data);
        }
    }

    public static class Unit {

        public static class Contact implements Identifiable {
            private Long id;
            private String contactName;
            private String companyName;
            private List<String> locations;
            private Boolean isCustomer;
            private Double revenue;
            private Double revenueCaptured;
            private Long testFile;
            private Date lastContactDate;

            public Contact() {}

            /**
             * Implements Identifiable
              */
            public Long getId() {
                return id;
            }

            public void setId(Long id) {
                this.id = id;
            }


            public String getContactName() {
                return contactName;
            }

            public void setContactName(String contactName) {
                this.contactName = contactName;
            }

            public String getCompanyName() {
                return companyName;
            }

            public void setCompanyName(String companyName) {
                this.companyName = companyName;
            }

            public List<String> getLocations() {
                return locations;
            }

            public void setLocations(List<String> locations) {
                this.locations = locations;
            }

            public Boolean getIsCustomer() {
                return isCustomer;
            }

            public void setIsCustomer(Boolean isCustomer) {
                this.isCustomer = isCustomer;
            }

            public Double getRevenue() {
                return revenue;
            }

            public void setRevenue(Double revenue) {
                this.revenue = revenue;
            }

            public Double getRevenueCaptured() {
                return revenueCaptured;
            }

            public void setRevenueCaptured(Double revenueCaptured) {
                this.revenueCaptured = revenueCaptured;
            }

            public Long getTestFile() {
                return testFile;
            }

            public void setTestFile(Long testFile) {
                this.testFile = testFile;
            }

            public Date getLastContactDate() {
                return lastContactDate;
            }

            public void setLastContactDate(Date lastContactDate) {
                this.lastContactDate = lastContactDate;
            }
        }

        public static Contact getUnitTestContact1() {
            Contact contact = new Contact();
            contact.setCompanyName("James Randall");
            contact.setContactName("Cryogenic Futures");
            contact.setId(1L);
            contact.setIsCustomer(true);
            contact.setLastContactDate(new Date());
            contact.setLocations(Arrays.asList(new String[] { "CA" }));
            contact.setRevenue(100000.0d);
            contact.setRevenueCaptured(10000.0d);
            contact.setTestFile(222L);

            return contact;
        }

        public static Record getUnitTestRecord1() {
            List<FieldMetadata> structure = Arrays.asList(new FieldMetadata[]{
                    new FieldMetadata(Identifiable.INTERNAL_ID_FIELD_NAME, TrackviaDataType.Number.type(), true, false, Collections.<String>emptyList()),
                    new FieldMetadata("ContactName", TrackviaDataType.ShortAnswer.type(), true, false, Collections.<String>emptyList()),
                    new FieldMetadata("CompanyName", TrackviaDataType.ShortAnswer.type(), true, false, Collections.<String>emptyList()),
                    new FieldMetadata("Locations", TrackviaDataType.CheckBox.type(), true, false, Arrays.asList(new String[]{"CO", "CA"})),
                    new FieldMetadata("IsCustomer", TrackviaDataType.Number.type(), true, false, Collections.<String>emptyList()),
                    new FieldMetadata("Revenue", TrackviaDataType.Currency.type(), true, false, Collections.<String>emptyList()),
                    new FieldMetadata("RevenueCaptured", TrackviaDataType.Percentage.type(), true, false, Collections.<String>emptyList()),
                    new FieldMetadata("TestFile", TrackviaDataType.Document.type(), true, false, Collections.<String>emptyList()),
                    new FieldMetadata("LastContactDate", TrackviaDataType.Date.type(), true, false, Collections.<String>emptyList())
            });
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");
            RecordData data = new RecordData();
            data.put(Identifiable.INTERNAL_ID_FIELD_NAME, 1L);
            data.put("ContactName", "James Randall");
            data.put("CompanyName", "Cryogenic Futures");
            data.put("Locations", Arrays.asList(new String[]{"CA"}));
            data.put("IsCustomer", true);
            data.put("Revenue", 100000.0);
            data.put("RevenueCaptured", 0.35);
            data.put("TestFile", 222L);
            data.put("LastContactDate", sdf.format(new Date()));

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
                    new FieldMetadata(Identifiable.INTERNAL_ID_FIELD_NAME, TrackviaDataType.Number.type(), true, false, Collections.<String>emptyList()),
                    new FieldMetadata("FirstName", TrackviaDataType.ShortAnswer.type(), true, false, Collections.<String>emptyList()),
                    new FieldMetadata("LastName", TrackviaDataType.ShortAnswer.type(), true, false, Collections.<String>emptyList()),
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
                    new FieldMetadata(Identifiable.INTERNAL_ID_FIELD_NAME, TrackviaDataType.Number.type(), true, false, Collections.<String>emptyList()),
                    new FieldMetadata("ContactName", TrackviaDataType.ShortAnswer.type(), true, false, Collections.<String>emptyList()),
                    new FieldMetadata("CompanyName", TrackviaDataType.ShortAnswer.type(), true, false, Collections.<String>emptyList())});
            List<RecordData> data = Arrays.asList(new RecordData[]{new RecordData(), new RecordData()});

            data.get(0).put(Identifiable.INTERNAL_ID_FIELD_NAME, 1L);
            data.get(0).put("ContactName", "James Randall");
            data.get(0).put("CompanyName", "Cryogenic Futures");

            data.get(1).put(Identifiable.INTERNAL_ID_FIELD_NAME, 2L);
            data.get(1).put("ContactName", "Simon Black");
            data.get(1).put("CompanyName", "Sunshine Industries");

            return new RecordSet(structure, data, 2);
        }

        public static RecordSet getUnitTestRecordSet2() {
            List<FieldMetadata> structure = Arrays.asList(new FieldMetadata[]{
                    new FieldMetadata(Identifiable.INTERNAL_ID_FIELD_NAME, TrackviaDataType.Number.type(), true, false, Collections.<String>emptyList()),
                    new FieldMetadata("ContactName", TrackviaDataType.ShortAnswer.type(), true, false, Collections.<String>emptyList()),
                    new FieldMetadata("CompanyName", TrackviaDataType.ShortAnswer.type(), true, false, Collections.<String>emptyList())});
            List<RecordData> data = Arrays.asList(new RecordData[]{new RecordData()});

            data.get(0).put(Identifiable.INTERNAL_ID_FIELD_NAME, 1L);
            data.get(0).put("ContactName", "James Randall");
            data.get(0).put("CompanyName", "Cryogenic Futures");

            return new RecordSet(structure, data, 1);
        }

        public static RecordSet getUnitTestRecordSet3() {
            Record record = getUnitTestRecord1();
            List<RecordData> data = new ArrayList<RecordData>();
            data.add(record.getData());

            RecordSet rs = new RecordSet();
            rs.setStructure(record.getStructure());
            rs.setData(data);
            rs.setTotalCount(1);

            return rs;
        }
    }
}
