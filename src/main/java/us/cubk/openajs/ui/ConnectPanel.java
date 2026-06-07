package us.cubk.openajs.ui;

import us.cubk.openajs.api.model.VpnServer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public class ConnectPanel extends JPanel {

    private final OpenAjsFrame frame;
    private final AppController controller;
    private final JLabel serverLabel = new JLabel("未选择节点");
    private final JTextField portField = new JTextField("1080", 6);
    private final JLabel statusLabel = new JLabel("未连接");
    private final JButton connectButton = new JButton("连接");
    private final JButton disconnectButton = new JButton("断开");
    private final JButton testButton = new JButton("测试出口 IP");

    public ConnectPanel(OpenAjsFrame frame, AppController controller) {
        super(new BorderLayout(8, 8));
        this.frame = frame;
        this.controller = controller;
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel form = new JPanel(new GridBagLayout());
        int row = 0;
        add(form, "节点", serverLabel, row++);
        add(form, "本地端口", portField, row++);
        add(form, "状态", statusLabel, row++);

        JPanel buttons = new JPanel();
        connectButton.addActionListener(e -> connect());
        disconnectButton.addActionListener(e -> disconnect());
        testButton.addActionListener(e -> test());
        buttons.add(connectButton);
        buttons.add(disconnectButton);
        buttons.add(testButton);

        add(form, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        refresh();
    }

    private void add(JPanel form, String label, java.awt.Component field, int row) {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.gridx = 0;
        c.gridy = row;
        c.anchor = GridBagConstraints.EAST;
        form.add(new JLabel(label), c);
        c.gridx = 1;
        c.anchor = GridBagConstraints.WEST;
        form.add(field, c);
    }

    private void connect() {
        VpnServer server = frame.getSelectedServer();
        if (server == null) {
            Ui.error(frame, "请先在节点页选择一个节点");
            return;
        }
        if (!controller.hasSession()) {
            Ui.error(frame, "请先登录账号");
            return;
        }
        int port = Integer.parseInt(portField.getText().trim());
        statusLabel.setText("连接中...");
        Ui.background(frame, () -> {
            controller.getProxy().start(controller.getClient(), server, port);
            return null;
        }, ignored -> refresh());
    }

    private void disconnect() {
        controller.getProxy().stop();
        refresh();
    }

    private void test() {
        if (!controller.getProxy().isConnected()) {
            Ui.error(frame, "尚未连接");
            return;
        }
        Ui.background(frame, () -> controller.getProxy().testExitIp(), ip -> Ui.info(frame, "出口 IP: " + ip));
    }

    public void refresh() {
        VpnServer server = frame.getSelectedServer();
        serverLabel.setText(server == null ? "未选择节点" : (server.getTit() + "  " + server.getSvr() + ":" + server.port()));
        ProxyService proxy = controller.getProxy();
        boolean connected = proxy.isConnected();
        statusLabel.setText(connected ? proxy.getMessage() : (proxy.getState() == ProxyService.State.CONNECTING ? "连接中..." : "未连接"));
        connectButton.setEnabled(!connected);
        disconnectButton.setEnabled(connected);
        testButton.setEnabled(connected);
    }
}
