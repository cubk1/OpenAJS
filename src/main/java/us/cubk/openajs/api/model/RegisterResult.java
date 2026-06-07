package us.cubk.openajs.api.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class RegisterResult {

    @SerializedName("AccountName")
    private String accountName;
    @SerializedName("Password")
    private String password;
    @SerializedName("ErrorMessage")
    private String errorMessage;

    public boolean isSuccess() {
        return accountName != null && !accountName.isEmpty() && password != null && !password.isEmpty();
    }
}
