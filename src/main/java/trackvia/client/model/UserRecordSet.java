package trackvia.client.model;

import java.util.ArrayList;
import java.util.List;

public class UserRecordSet {
    private List<FieldMetadata> structure;
    private List<User> data;
    private int totalCount;

    public UserRecordSet() {}

    public UserRecordSet(final List<FieldMetadata> structure, final List<User> data, final int totalCount) {
        this.structure = new ArrayList<FieldMetadata>(structure);
        this.data = new ArrayList<User>(data);
        this.totalCount = totalCount;
    }

    public List<FieldMetadata> getStructure() {
        return structure;
    }

    public void setStructure(List<FieldMetadata> structure) {
        this.structure = structure;
    }

    public List<User> getData() {
        return data;
    }

    public void setData(List<User> data) {
        this.data = data;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }
}
