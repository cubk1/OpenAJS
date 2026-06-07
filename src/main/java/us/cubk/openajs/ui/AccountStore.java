package us.cubk.openajs.ui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Data;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Data
public class AccountStore {

    private String deviceId;
    private String uid;
    private int currentIndex = -1;
    private List<Account> accounts = new ArrayList<>();

    public static Path file() {
        return Path.of(System.getProperty("user.home"), ".openajs", "accounts.json");
    }

    public static AccountStore load() {
        try {
            Path path = file();
            if (Files.exists(path)) {
                return new Gson().fromJson(Files.readString(path), AccountStore.class);
            }
        } catch (Exception ignored) {
        }
        return new AccountStore();
    }

    public void save() {
        try {
            Path path = file();
            Files.createDirectories(path.getParent());
            Files.writeString(path, new GsonBuilder().setPrettyPrinting().create().toJson(this));
        } catch (Exception ignored) {
        }
    }

    public Account current() {
        return (currentIndex >= 0 && currentIndex < accounts.size()) ? accounts.get(currentIndex) : null;
    }

    public Account findByUsername(String username) {
        for (Account account : accounts) {
            if (account.getUsername().equals(username)) {
                return account;
            }
        }
        return null;
    }
}
