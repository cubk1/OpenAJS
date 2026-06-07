package us.cubk.openajs.api.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.Base64;

@Data
public class CaptchaResult {

    @SerializedName("Captcha")
    private String captcha;
    @SerializedName("ErrorMessage")
    private String errorMessage;

    public byte[] imageBytes() {
        return (captcha == null || captcha.isEmpty()) ? new byte[0] : Base64.getDecoder().decode(captcha);
    }
}
