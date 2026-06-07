package us.cubk.openajs.api.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class AnnounceResult {

    @SerializedName("Content")
    private String content;
    @SerializedName("Error")
    private String error;
    @SerializedName("ErrorMessage")
    private String errorMessage;
}
