package us.cubk.openajs.api.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class LoginResult {

    @SerializedName("LoginToken")
    private String loginToken;
    @SerializedName("AuthResult")
    private String authResult;
    @SerializedName("AuthMessage")
    private String authMessage;
    @SerializedName("UserName")
    private String userName;
    @SerializedName("ConnectPassword")
    private String connectPassword;
    @SerializedName("Email")
    private String email;
    @SerializedName("PhoneNumber")
    private String phoneNumber;
    @SerializedName("MembershipStatus")
    private String membershipStatus;
    @SerializedName("GroupTitle")
    private String groupTitle;
    @SerializedName("ExpireTime")
    private long expireTime;
    @SerializedName("Expired")
    private boolean expired;
    @SerializedName("UseCount")
    private int useCount;

    public boolean isOk() {
        return "OK".equals(authResult);
    }
}
