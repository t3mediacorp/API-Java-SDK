package trackvia.client.model;

import java.util.ArrayList;
import java.util.List;

public class RecordDataBatch {
    private List<RecordData> data;

    public RecordDataBatch() {}

    public RecordDataBatch(final List<RecordData> data) {
        this.data = new ArrayList<RecordData>(data);
    }

    public List<RecordData> getData() {
        return data;
    }

    public void setData(List<RecordData> data) {
        this.data = data;
    }
}
