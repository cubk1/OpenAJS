package us.cubk.openajs.api.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class NotificationCard {

    @SerializedName("id")
    private int id;
    @SerializedName("NotificationTitle")
    private String notificationTitle;
    @SerializedName("NotificationContent")
    private String notificationContent;
    @SerializedName("ExtCreateTimestamp")
    private long extCreateTimestamp;
    @SerializedName("NotificationImage")
    private String notificationImage;
}
