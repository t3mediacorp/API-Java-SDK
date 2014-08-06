package trackvia.client.model;

import java.util.HashMap;

public class RecordData extends HashMap<String, Object> {
    public RecordData() {}

    public RecordData(final RecordData copy) {
        putAll(copy);
    }

    public Long getRecordId() {
        String idString = get("id").toString();

        return Long.valueOf(idString);
    }
}
