package trackvia.client.model;

import java.util.HashMap;
import java.util.Map;

public enum TrackviaDataType {
    DateTime("datetime"),
    User("user"),
    Identifier("identifier"),
    ShortAnswer("shortAnswer"),
    Email("email"),
    UserStatus("userStatus"),
    TimeZone("timeZone"),
    Paragraph("paragraph"),
    Number("number"),
    Percentage("percentage"),
    Currency("currency"),
    AutoIncrement("autoIncrement"),
    DropDown("dropDown"),
    CheckBox("checkbox"),
    Date("date"),
    Document("document"),
    Image("image"),
    URL("url"),
    Point("point");

    private static Map<String, TrackviaDataType> lookupTable = new HashMap<String, TrackviaDataType>();

    static {
        for (TrackviaDataType dt : values()) {
            lookupTable.put(dt.type, dt);
        }
    }

    private String type;

    TrackviaDataType(String type) {
        this.type = type;
    }

    public String type() { return this.type; }

    public static TrackviaDataType get(final String type) {
        return lookupTable.get(type);
    }
}
