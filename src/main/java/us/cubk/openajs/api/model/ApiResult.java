package us.cubk.openajs.api.model;

import com.google.gson.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ApiResult {

    private int httpCode;
    private String rawBody;
    private JsonObject json;
    private String url;
}
