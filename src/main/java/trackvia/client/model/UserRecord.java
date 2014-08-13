package trackvia.client.model;

import java.util.ArrayList;
import java.util.List;

public class UserRecord {
    private List<FieldMetadata> structure;
    private User data;

    public UserRecord() {}

    public UserRecord(final List<FieldMetadata> structure, final User data) {
        this.structure = new ArrayList<FieldMetadata>(structure);
        this.data = data;
    }

    public List<FieldMetadata> getStructure() {
        return structure;
    }

    public void setStructure(List<FieldMetadata> structure) {
        this.structure = structure;
    }

    public User getData() {
        return data;
    }

    public void setData(User data) {
        this.data = data;
    }
}
