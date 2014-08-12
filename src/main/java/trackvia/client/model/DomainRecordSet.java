package trackvia.client.model;

import java.util.ArrayList;
import java.util.List;

public class DomainRecordSet<T> {
    private List<FieldMetadata> structure;
    private List<T> data;
    private int totalCount;

    public DomainRecordSet() {}

    public DomainRecordSet(final List<FieldMetadata> structure, final List<T> data, final int totalCount) {
        this.structure = new ArrayList<FieldMetadata>();
        for (FieldMetadata fm : structure) {
            this.structure.add(new FieldMetadata(fm.getName(), fm.getType().type(), fm.getRequired(), fm.getUnique(),
                    fm.getChoices()));
        }
        this.data = new ArrayList<T>();
        for (T rd : data) {
            this.data.add(rd);
        }
        this.totalCount = totalCount;
    }

    public List<FieldMetadata> getStructure() {
        return structure;
    }

    public void setStructure(List<FieldMetadata> structure) {
        this.structure = structure;
    }

    public List<T> getData() {
        return data;
    }

    public void setData(List<T> data) {
        this.data = data;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }
}
