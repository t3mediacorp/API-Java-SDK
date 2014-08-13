package trackvia.client.examples;

import trackvia.client.TrackviaApiException;
import trackvia.client.TrackviaClient;
import trackvia.client.TrackviaClientException;
import trackvia.client.model.RecordData;
import trackvia.client.model.RecordDataBatch;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ContactManagementDataManager {
    /**
     * Contacts table
     */
    public static final String CONTACTS_CONTACT_NAME =      "Contact Name";
    public static final String CONTACTS_COMPANY_NAME =      "Company Name";
    public static final String CONTACTS_SPECIALTY =         "Specialty";
    public static final String CONTACTS_TITLE =             "Title";
    public static final String CONTACTS_PHONE_NUMBER =      "Phone Number";
    public static final String CONTACTS_EMAIL_ADDRESS =     "Email Address";
    public static final String CONTACTS_NOTES =             "Notes";

    /**
     * Activities table
     */
    public static final String ACTIVITIES_ACTIVITY_NUMBER = "Activity Number";
    public static final String ACTIVITIES_TYPE =            "Activity Type";
    public static final String ACTIVITIES_STATUS =          "Status";
    public static final String ACTIVITIES_NOTES =           "Notes";

    public RecordData createContact(final String name, final String company, final String specialty,
                                    final String title, final String phone, final String email, final String notes) {
        RecordData data = new RecordData();

        data.put(CONTACTS_CONTACT_NAME, name);
        data.put(CONTACTS_COMPANY_NAME, company);
        data.put(CONTACTS_SPECIALTY, specialty);
        data.put(CONTACTS_TITLE, title);
        data.put(CONTACTS_PHONE_NUMBER, phone);
        data.put(CONTACTS_EMAIL_ADDRESS, email);
        data.put(CONTACTS_NOTES, notes);

        return data;
    }

    public RecordData createActivity(final String type, final String status, final String notes) {
        RecordData data = new RecordData();

        data.put(ACTIVITIES_TYPE, type);
        data.put(ACTIVITIES_STATUS, status);
        data.put(ACTIVITIES_NOTES, notes);

        return data;
    }

    public void importData(final Path dataFile) {
        final int CONTACTS_VIEW_ID = 1;
        final int ACTIVITIES_VIEW_ID = 2;

        TrackviaClient client = null;

        // Contact data
        RecordDataBatch contactsBatch = new RecordDataBatch();
        List<RecordData> contacts = new ArrayList<RecordData>();
        contactsBatch.setData(contacts);
        contacts.add(createContact("Joe Smith", "IBM", "Software", "Sr. Manager", "555-555-5555", "joe@example.com", ""));
        contacts.add(createContact("Patty Barker", "Google", "Networks", "VP Network Operations", "555-555-5555", "patty@gmail.com", ""));

        // Activities data
        RecordDataBatch activitiesBatch = new RecordDataBatch();
        List<RecordData> activities = new ArrayList<RecordData>();
        activities.add(createActivity("Phone Call", "Open", "Joe Smith of IBM is interested in starting a pilot program"));
        activities.add(createActivity("Email", "Open", "Trying to initiate contact with Patty Smith @ Google"));

        try {
            client = TrackviaClient.create("go.api.trackvia.com", "tom.michaud@gmail.com", "password");

            client.createRecords(CONTACTS_VIEW_ID, contactsBatch);
            client.createRecords(ACTIVITIES_VIEW_ID, activitiesBatch);

        } catch (TrackviaClientException e) {

        } catch (TrackviaApiException e) {

        } finally {
            if (null != client) client.shutdown();
        }
    }

    public ContactManagementDataManager() {}


    public static void main(String[] args) {
        final Path dataFile = Paths.get(args[0]);

        ContactManagementDataManager manager = new ContactManagementDataManager();
        manager.importData(dataFile);
    }
}
