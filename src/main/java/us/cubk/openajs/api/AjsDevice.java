package us.cubk.openajs.api;

import us.cubk.openajs.api.crypto.FVSign;
import us.cubk.openajs.api.util.RandomUtil;
import lombok.Data;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;

@Data
public class AjsDevice {

    private String appVersion = "4.11.10.0";
    private String site = "ajs";
    private String platform = "android";
    private String osVersion = "13";
    private String osDevice = "Xiaomi 23013RK75C";
    private String osHardware = "qcom";
    private String osLang = "zh-Hans-CN";
    private String lang = "zh_cn";
    private String installMarket = "google";
    private String channel = "";
    private String timezone = String.valueOf(TimeZone.getDefault().getRawOffset() / 60000);
    private String osDim = "w=1080&h=2400&d=440";
    private String deviceId = RandomUtil.hex(16);
    private String uid = generateUid();

    public static String generateUid() {
        long time = System.currentTimeMillis();
        String base = String.format("%08x%02x%08x", (int) (time / 1000L), (int) (time & 0xFF), RandomUtil.nextInt());
        String checksum = FVSign.hex(FVSign.dualHash(base.getBytes(StandardCharsets.UTF_8))).substring(0, 4);
        return base + checksum;
    }

    public Map<String, String> commonParams() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("_site", site);
        params.put("_v", appVersion);
        params.put("_m", platform);
        params.put("_os_ver", osVersion);
        params.put("_os_dev", osDevice);
        params.put("_os_hw", osHardware);
        params.put("_os_did", deviceId);
        params.put("_os_lang", osLang);
        params.put("_tz", timezone);
        params.put("_os_dim", osDim);
        params.put("_inst_mkt", installMarket);
        params.put("_lang", lang);
        params.put("_vrf", channel);
        params.put("_uid", uid);
        return params;
    }
}
