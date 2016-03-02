package trackvia.client.model;

import java.util.ArrayList;
import java.util.List;

public class RecordSet {
    private List<FieldMetadata> structure;
    private List<RecordData> data;
    private int totalCount;

    public RecordSet() {}

    public RecordSet(final List<FieldMetadata> structure, final List<RecordData> data, final int totalCount) {
        this.structure = new ArrayList<FieldMetadata>();
        for (FieldMetadata fm : structure) {
            this.structure.add(new FieldMetadata(fm.getName(), fm.getType().type(), fm.getRequired(), fm.getUnique(),
                    fm.getChoices()));
        }
        this.data = new ArrayList<RecordData>();
        for (RecordData rd : data) {
            this.data.add(new RecordData(rd));
        }
        this.totalCount = totalCount;
    }

    public RecordDataBatch asBatch() {
        RecordDataBatch batch = new RecordDataBatch();
        List<RecordData> rd = new ArrayList<RecordData>();

        rd.addAll(data);
        batch.setData(rd);

        return batch;
    }

    public List<FieldMetadata> getStructure() {
        return structure;
    }

    public void setStructure(List<FieldMetadata> structure) {
        this.structure = structure;
    }

    public List<RecordData> getData() {
        return data;
    }

    public void setData(List<RecordData> data) {
        this.data = data;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }
    
    public RecordData get(int index){
    	return data.get(index);
    }
    
    public void add(RecordData recordData){
    	data.add(recordData);
    }
}
