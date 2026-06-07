package us.cubk.openajs.ui;

import com.formdev.flatlaf.FlatLightLaf;
import lombok.Getter;
import lombok.Setter;
import us.cubk.openajs.api.model.VpnServer;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.text.SimpleDateFormat;
import java.util.Date;

public class OpenAjsFrame extends JFrame {

    private final AppController controller = new AppController();
    @Getter
    @Setter
    private VpnServer selectedServer;

    private final JLabel accountLabel = new JLabel();
    private final JLabel statusLabel = new JLabel();
    private final AccountsPanel accountsPanel;
    private final ConnectPanel connectPanel;

    public OpenAjsFrame() {
        super("OpenAijiasu");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(820, 560);
        setLocationRelativeTo(null);

        JPanel top = new JPanel(new BorderLayout());
        top.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        top.add(accountLabel, BorderLayout.WEST);
        top.add(statusLabel, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        accountsPanel = new AccountsPanel(this, controller);
        connectPanel = new ConnectPanel(this, controller);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("账号", accountsPanel);
        tabs.addTab("节点", new ServersPanel(this, controller));
        tabs.addTab("连接", connectPanel);
        tabs.addTab("消息", new MessagesPanel(this, controller));
        add(tabs, BorderLayout.CENTER);

        refreshAll();
        Timer timer = new Timer(2000, e -> refreshStatus());
        timer.start();
    }

    public void refreshAll() {
        Account current = controller.getStore().current();
        if (current != null) {
            String expire = current.getExpireTime() > 0 ? new SimpleDateFormat("yyyy-MM-dd").format(new Date(current.getExpireTime() * 1000L)) : "-";
            accountLabel.setText("当前: " + current.getUsername() + "  |  " + nullable(current.getGroupTitle()) + "  |  到期 " + expire);
        } else {
            accountLabel.setText("未登录");
        }
        accountsPanel.refresh();
        connectPanel.refresh();
        refreshStatus();
    }

    private void refreshStatus() {
        ProxyService proxy = controller.getProxy();
        statusLabel.setText(proxy.isConnected() ? "● 已连接  " + proxy.getMessage() : "○ 未连接");
        connectPanel.refresh();
    }

    private String nullable(String value) {
        return value != null ? value : "";
    }

    public static void launch() {
        FlatLightLaf.setup();
        SwingUtilities.invokeLater(() -> new OpenAjsFrame().setVisible(true));
    }
}
