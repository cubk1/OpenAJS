package us.cubk.openajs.api.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class ResetPasswordResult {

    @SerializedName("Result")
    private String result;
    @SerializedName("Message")
    private String message;
    @SerializedName("Error")
    private String error;

    public boolean isSuccess() {
        return "Success".equals(result);
    }
}
