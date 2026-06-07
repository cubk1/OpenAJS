package us.cubk.openajs.api.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class VerifyCodeResult {

    @SerializedName("Result")
    private String result;
    @SerializedName("Message")
    private String message;
    @SerializedName("Countdown")
    private int countdown;

    public boolean isSuccess() {
        return "Success".equals(result);
    }
}
