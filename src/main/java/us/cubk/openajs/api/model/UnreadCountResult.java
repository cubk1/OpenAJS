package us.cubk.openajs.api.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class UnreadCountResult {

    @SerializedName("Result")
    private String result;
    @SerializedName("Count")
    private int count;

    public boolean isSuccess() {
        return "Success".equals(result);
    }
}
