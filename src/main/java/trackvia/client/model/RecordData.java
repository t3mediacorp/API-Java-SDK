package trackvia.client.model;

import java.util.HashMap;

public class RecordData extends HashMap<String, Object> implements Identifiable {
    public RecordData() {}

    public RecordData(final RecordData copy) {
        putAll(copy);
    }

    public Long getId() {
        String idString = get(Identifiable.INTERNAL_ID_FIELD_NAME).toString();

        return Long.valueOf(idString);
    }

    public void setId(Long id) {
        throw new IllegalArgumentException("Internal identifier values are assigned by the Trackvia Service");
    }
}
