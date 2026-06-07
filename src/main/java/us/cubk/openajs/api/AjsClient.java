package us.cubk.openajs.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import us.cubk.openajs.api.crypto.FVSign;
import us.cubk.openajs.api.model.AnnounceResult;
import us.cubk.openajs.api.model.ApiResult;
import us.cubk.openajs.api.model.AutoMatchResult;
import us.cubk.openajs.api.model.CaptchaResult;
import us.cubk.openajs.api.model.FavoriteListResult;
import us.cubk.openajs.api.model.LoginResult;
import us.cubk.openajs.api.model.NotificationListResult;
import us.cubk.openajs.api.model.RegisterResult;
import us.cubk.openajs.api.model.ResetPasswordResult;
import us.cubk.openajs.api.model.SimpleResult;
import us.cubk.openajs.api.model.UnreadCountResult;
import us.cubk.openajs.api.model.VerifyCodeResult;
import us.cubk.openajs.api.model.VpnServer;
import us.cubk.openajs.api.net.HttpClient;
import us.cubk.openajs.api.net.InsecureTLS;
import us.cubk.openajs.api.util.AccountUtil;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class AjsClient {

    @Getter
    private final AjsDevice device;
    @Getter
    private final String secret;
    private final List<String> hosts;
    private final Gson gson = new Gson();
    private int connectTimeout = 15000;
    private int readTimeout = 20000;
    @Getter
    @Setter
    private String loginToken;
    @Getter
    @Setter
    private String userName;
    @Getter
    @Setter
    private String connectPassword;

    public AjsClient() {
        this(new AjsDevice());
    }

    public AjsClient(AjsDevice device) {
        this(device, "84:C1:12:45:0B:68:F9:B2:06:95:00:44:74:37:55:71:60:B5:98:31", List.of("https://login.ajs-app.com", "https://login.mmegg.com", "https://login.mmegg.com:4443", "https://login.mbclubsh.com"));
    }

    public AjsClient(AjsDevice device, String secret, List<String> hosts) {
        this.device = device;
        this.secret = secret;
        this.hosts = hosts;
        InsecureTLS.enableGlobal();
    }

    public ApiResult call(String cmd, Map<String, String> extra) throws IOException {
        TreeMap<String, String> params = new TreeMap<>(device.commonParams());
        params.put("cmd", cmd);
        if (extra != null) {
            params.putAll(extra);
        }
        String body = FVSign.buildQuery(params);
        String timestamp = Long.toString(System.currentTimeMillis());
        String sig = FVSign.sign(secret, body, timestamp);
        IOException last = null;
        for (String host : hosts) {
            HttpClient http = null;
            try {
                http = new HttpClient(new URL(host + "/client.php")).connectTimeout(connectTimeout).readTimeout(readTimeout);
                http.header("Content-Type", "application/x-www-form-urlencoded");
                http.header("Accept-Encoding", "gzip");
                http.header("User-Agent", "okhttp/4.9.3");
                http.header("X-Timestamp", timestamp);
                http.header("X-Sig", sig);
                http.post(body);
                int code = http.responseCode();
                String response = http.body();
                JsonObject json = null;
                try {
                    json = JsonParser.parseString(response).getAsJsonObject();
                } catch (RuntimeException ignored) {
                }
                return new ApiResult(code, response, json, http.finalUrl());
            } catch (IOException e) {
                last = e;
            } finally {
                if (http != null) {
                    http.disconnect();
                }
            }
        }
        throw last != null ? last : new IOException("no hosts available");
    }

    public LoginResult login(String username, String password) throws IOException {
        TreeMap<String, String> params = new TreeMap<>();
        params.put("username", username);
        params.put("password", password);
        params.put("trigger", "login");
        ApiResult result = call("ClientApiLogin/Login", params);
        JsonObject json = result.getJson();
        LoginResult login = (json != null && json.has("Login")) ? gson.fromJson(json.get("Login"), LoginResult.class) : new LoginResult();
        if (login.isOk() && login.getLoginToken() != null) {
            this.loginToken = login.getLoginToken();
            this.userName = login.getUserName();
            this.connectPassword = login.getConnectPassword();
        }
        return login;
    }

    public ApiResult callAuth(String cmd, Map<String, String> extra) throws IOException {
        if (loginToken == null || loginToken.isEmpty()) {
            throw new IllegalStateException("login required");
        }
        String user = (userName != null && !userName.isEmpty()) ? userName
                : (loginToken.contains("|") ? loginToken.substring(0, loginToken.indexOf('|')) : loginToken);
        TreeMap<String, String> params = new TreeMap<>();
        params.put("_client_login", loginToken);
        params.put("username", user);
        if (extra != null) {
            params.putAll(extra);
        }
        return call(cmd, params);
    }

    public ApiResult loginSync() throws IOException {
        TreeMap<String, String> params = new TreeMap<>();
        params.put("trigger", "login");
        ApiResult result = callAuth("ClientApiLogin/Login", params);
        JsonObject json = result.getJson();
        if (json != null && json.has("Login") && json.getAsJsonObject("Login").has("ConnectPassword")) {
            this.connectPassword = json.getAsJsonObject("Login").get("ConnectPassword").getAsString();
        }
        return result;
    }

    public Map<String, VpnServer> servers() throws IOException {
        JsonObject json = loginSync().getJson();
        if (json == null || !json.has("Servers")) {
            return new LinkedHashMap<>();
        }
        return gson.fromJson(json.get("Servers"), new TypeToken<LinkedHashMap<String, VpnServer>>() {
        }.getType());
    }

    public FavoriteListResult userVpnServerFavoriteList() throws IOException {
        return parse(callAuth("ClientApiUser/UserVpnServerFavoriteList", null), FavoriteListResult.class);
    }

    public SimpleResult vpnServerFavorite(String serverId, boolean favorite) throws IOException {
        TreeMap<String, String> params = new TreeMap<>();
        params.put("Action", favorite ? "favorite" : "unfavorite");
        params.put("ServerId", serverId);
        return parse(callAuth("ClientApiUser/VpnServerFavorite", params), SimpleResult.class);
    }

    public AutoMatchResult autoMatch(int locationLevel, String locationCode) throws IOException {
        TreeMap<String, String> params = new TreeMap<>();
        params.put("LocationLevel", String.valueOf(locationLevel));
        params.put("LocationCode", locationCode);
        return parse(callAuth("ClientApiVpnServer/AutoMatch", params), AutoMatchResult.class);
    }

    public NotificationListResult getNotificationList(int pageSize, Integer lastNotificationId) throws IOException {
        TreeMap<String, String> params = new TreeMap<>();
        params.put("pageSize", String.valueOf(pageSize));
        if (lastNotificationId != null) {
            params.put("lastNotificationId", String.valueOf(lastNotificationId));
        }
        return parse(callAuth("ClientApiMessageCenter/GetNotificationList", params), NotificationListResult.class);
    }

    public UnreadCountResult getUnreadNotificationCount() throws IOException {
        return parse(callAuth("ClientApiMessageCenter/GetUnreadNotificationCount", null), UnreadCountResult.class);
    }

    public AnnounceResult getAnnounce() throws IOException {
        return parse(callAuth("ClientApiMessageCenter/GetAnnounce", null), AnnounceResult.class);
    }

    public SimpleResult closeAnnounce(String msgid) throws IOException {
        TreeMap<String, String> params = new TreeMap<>();
        params.put("msgid", msgid);
        return parse(callAuth("ClientApiMessageCenter/CloseAnnounce", params), SimpleResult.class);
    }

    public SimpleResult clientSetting(Map<String, String> settings) throws IOException {
        TreeMap<String, String> params = new TreeMap<>();
        if (settings != null) {
            for (Map.Entry<String, String> entry : settings.entrySet()) {
                params.put("ClientSettings[" + entry.getKey() + "]", entry.getValue());
            }
        }
        return parse(callAuth("ClientApiClientSetting/ClientSetting", params), SimpleResult.class);
    }

    public SimpleResult reportUpgradeRecord(String upgradeType, String priorVersion, String upgradeDatetime, String popupInterval) throws IOException {
        TreeMap<String, String> params = new TreeMap<>();
        params.put("UpgradeType", upgradeType);
        params.put("PriorVersion", priorVersion);
        params.put("UpgradeDatetime", upgradeDatetime);
        if (userName != null) {
            params.put("username", userName);
        }
        if (popupInterval != null && !popupInterval.isEmpty()) {
            params.put("PopupInterval", popupInterval);
        }
        return parse(call("ClientApiUpgrade/UpgradeRecord", params), SimpleResult.class);
    }

    public CaptchaResult captcha(String usage, String mode) throws IOException {
        TreeMap<String, String> params = new TreeMap<>();
        params.put("Usage", usage);
        if (mode != null) {
            params.put("Mode", mode);
        }
        return parse(call("ClientApiVerifyCode/Captcha", params), CaptchaResult.class);
    }

    public VerifyCodeResult sendVerifyCode(String account, String captchaText, String usage) throws IOException {
        TreeMap<String, String> params = new TreeMap<>();
        params.put("Mode", AccountUtil.mode(account));
        params.put("Usage", usage);
        params.put("CaptchaText", captchaText);
        params.put("AccountName", AccountUtil.accountName(account));
        return parse(call("ClientApiVerifyCode/SendVerifyCode", params), VerifyCodeResult.class);
    }

    public RegisterResult register(String account, String password, String verifyCode, String invitationCode) throws IOException {
        TreeMap<String, String> params = new TreeMap<>();
        params.put("AccountName", AccountUtil.accountName(account));
        params.put("Password", password);
        params.put("Mode", AccountUtil.mode(account));
        params.put("InvitationCode", invitationCode == null ? "" : invitationCode);
        params.put("VerifyCode", verifyCode);
        return parse(call("ClientApiRegister/Register", params), RegisterResult.class);
    }

    public ResetPasswordResult resetPassword(String account, String verifyCode, String newPassword) throws IOException {
        TreeMap<String, String> params = new TreeMap<>();
        params.put("Mode", AccountUtil.mode(account));
        params.put("AccountName", AccountUtil.accountName(account));
        params.put("VerifyCode", verifyCode);
        params.put("NewPassword", newPassword);
        return parse(call("ClientApiUser/ResetPassword", params), ResetPasswordResult.class);
    }

    private <T> T parse(ApiResult result, Class<T> type) {
        return gson.fromJson(result.getJson() != null ? result.getJson() : new JsonObject(), type);
    }
}
