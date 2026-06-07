package us.cubk.openajs.api.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;

@Data
public class NotificationListResult {

    @SerializedName("Result")
    private String result;
    @SerializedName("List")
    private List<NotificationCard> list;

    public boolean isSuccess() {
        return "Success".equals(result);
    }
}
