package us.cubk.openajs.ui;

import lombok.Getter;
import us.cubk.openajs.api.AjsClient;
import us.cubk.openajs.api.AjsDevice;
import us.cubk.openajs.api.model.LoginResult;
import us.cubk.openajs.api.model.VpnServer;

import java.util.Map;

public class AppController {

    @Getter
    private final AccountStore store;
    @Getter
    private final AjsDevice device;
    @Getter
    private final AjsClient client;
    @Getter
    private final ProxyService proxy = new ProxyService();

    public AppController() {
        store = AccountStore.load();
        if (store.getDeviceId() == null || store.getUid() == null) {
            AjsDevice fresh = new AjsDevice();
            store.setDeviceId(fresh.getDeviceId());
            store.setUid(fresh.getUid());
            store.save();
        }
        device = new AjsDevice();
        device.setDeviceId(store.getDeviceId());
        device.setUid(store.getUid());
        client = new AjsClient(device);
        applyCurrent();
    }

    private void applyCurrent() {
        Account account = store.current();
        if (account != null) {
            client.setLoginToken(account.getToken());
            client.setUserName(account.getServerUser());
            client.setConnectPassword(account.getConnectPassword());
        }
    }

    public LoginResult login(String username, String password) throws Exception {
        LoginResult result = client.login(username, password);
        if (result.isOk()) {
            Account account = store.findByUsername(username);
            if (account == null) {
                account = new Account();
                account.setUsername(username);
                store.getAccounts().add(account);
            }
            account.setLabel(result.getUserName());
            account.setPassword(password);
            account.setToken(result.getLoginToken());
            account.setServerUser(result.getUserName());
            account.setConnectPassword(result.getConnectPassword());
            account.setMembershipStatus(result.getMembershipStatus());
            account.setGroupTitle(result.getGroupTitle());
            account.setExpireTime(result.getExpireTime());
            account.setEmail(result.getEmail());
            account.setPhoneNumber(result.getPhoneNumber());
            store.setCurrentIndex(store.getAccounts().indexOf(account));
            store.save();
            applyCurrent();
        }
        return result;
    }

    public void switchTo(int index) throws Exception {
        Account account = store.getAccounts().get(index);
        store.setCurrentIndex(index);
        store.save();
        if (account.getPassword() != null && !account.getPassword().isEmpty()) {
            login(account.getUsername(), account.getPassword());
        } else {
            applyCurrent();
        }
    }

    public void deleteAccount(int index) {
        proxy.stop();
        store.getAccounts().remove(index);
        if (store.getCurrentIndex() >= store.getAccounts().size()) {
            store.setCurrentIndex(store.getAccounts().size() - 1);
        }
        store.save();
        applyCurrent();
    }

    public Map<String, VpnServer> servers() throws Exception {
        return client.servers();
    }

    public boolean hasSession() {
        return client.getLoginToken() != null && !client.getLoginToken().isEmpty();
    }
}
