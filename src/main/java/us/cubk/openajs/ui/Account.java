package us.cubk.openajs.ui;

import lombok.Data;

@Data
public class Account {

    private String label;
    private String username;
    private String password;
    private String token;
    private String serverUser;
    private String connectPassword;
    private String membershipStatus;
    private String groupTitle;
    private long expireTime;
    private String email;
    private String phoneNumber;
}
