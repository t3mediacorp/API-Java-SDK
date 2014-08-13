package trackvia.client.model;

import java.util.ArrayList;
import java.util.List;

public class DomainRecordDataBatch<T> {
    private List<T> data;

    public DomainRecordDataBatch() {}

    public DomainRecordDataBatch(final List<T> data) {
        this.data = new ArrayList<T>(data);
    }

    public List<T> getData() {
        return data;
    }

    public void setData(List<T> data) {
        this.data = data;
    }

}
