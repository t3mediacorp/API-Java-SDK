package trackvia.client.model;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class Record {
    private List<FieldMetadata> structure;
    private RecordData data;

    public Record() {}

    public Record(final List<FieldMetadata> structure, final RecordData data) {
        this.structure = structure;
        this.data = data;
    }

    public Record(final RecordData data) {
        this.data = new RecordData(data);
    }

    public List<FieldMetadata> getStructure() {
        return structure;
    }

    public void setStructure(List<FieldMetadata> structure) {
        this.structure = structure;
    }

    public RecordData getData() {
        return data;
    }

    public void setData(RecordData data) {
        this.data = data;
    }

    public long getRecordId() { return (this.data != null) ? (this.data.getId()) : (null); }

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (!(o instanceof Record)) return false;

        Record otherRecord = (Record) o;

        if (otherRecord.getRecordId() != getRecordId()) return false;

        // Ensure both records have the same set of fields.  Having to do this isn't strictly
        // necessary to ensure equality.
        Set fieldNames1 = new TreeSet<String>();
        for (FieldMetadata fm : this.structure) {
            fieldNames1.add(fm.getName());
        }
        Set fieldNames2 = new TreeSet<String>();
        for (FieldMetadata fm : otherRecord.structure) {
            fieldNames2.add(fm.getName());
        }
        if (!fieldNames1.equals(fieldNames2)) return false;

        // Ensure all fields have the same value.
        for (FieldMetadata fm : this.structure) {
            String name = fm.getName();
            Object v1 = this.data.get(name);
            Object v2 = otherRecord.getData().get(name);

            if ((v1 == null || v2 == null) && (v1 != v2)) return false;
            if (!v1.equals(v2)) return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) getRecordId();
        return result;
    }
}
