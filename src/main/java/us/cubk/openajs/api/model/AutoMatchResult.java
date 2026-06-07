package us.cubk.openajs.api.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class AutoMatchResult {

    @SerializedName("Result")
    private String result;
    @SerializedName("Error")
    private String error;
    @SerializedName("PopupType")
    private String popupType;
    @SerializedName("Data")
    private Data data;

    public boolean isSuccess() {
        return "Success".equals(result);
    }

    public String vpnServerId() {
        return data != null ? data.vpnServerId : null;
    }

    @lombok.Data
    public static class Data {
        @SerializedName("VpnServerId")
        private String vpnServerId;
    }
}
