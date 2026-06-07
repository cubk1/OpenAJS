package us.cubk.openajs.api.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;

@Data
public class FavoriteListResult {

    @SerializedName("Result")
    private String result;
    @SerializedName("Data")
    private List<String> data;

    public boolean isSuccess() {
        return "Success".equals(result);
    }
}
