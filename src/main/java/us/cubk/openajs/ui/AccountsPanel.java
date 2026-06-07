package us.cubk.openajs.ui;

import us.cubk.openajs.api.model.LoginResult;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AccountsPanel extends JPanel {

    private final OpenAjsFrame frame;
    private final AppController controller;
    private final AccountTableModel model;
    private final JTable table;

    public AccountsPanel(OpenAjsFrame frame, AppController controller) {
        super(new BorderLayout(8, 8));
        this.frame = frame;
        this.controller = controller;
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        model = new AccountTableModel();
        table = new JTable(model);
        table.setRowHeight(26);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(button("登录/添加", this::doLogin));
        buttons.add(button("注册", () -> new AuthDialog(frame, controller, false).setVisible(true)));
        buttons.add(button("重置密码", () -> new AuthDialog(frame, controller, true).setVisible(true)));
        buttons.add(button("切换为当前", this::doSwitch));
        buttons.add(button("删除", this::doDelete));
        buttons.add(button("刷新", this::refresh));
        add(buttons, BorderLayout.SOUTH);
    }

    private JButton button(String text, Runnable action) {
        JButton b = new JButton(text);
        b.addActionListener(e -> action.run());
        return b;
    }

    private void doLogin() {
        JTextField username = new JTextField(18);
        JPasswordField password = new JPasswordField(18);
        JPanel panel = new JPanel(new GridLayout(2, 2, 6, 6));
        panel.add(new JLabel("用户名(手机/邮箱)"));
        panel.add(username);
        panel.add(new JLabel("密码"));
        panel.add(password);
        int result = JOptionPane.showConfirmDialog(frame, panel, "登录", JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }
        String user = username.getText().trim();
        String pass = new String(password.getPassword());
        Ui.background(frame, () -> controller.login(user, pass), (LoginResult login) -> {
            if (login.isOk()) {
                refresh();
                frame.refreshAll();
            } else {
                Ui.error(frame, login.getAuthMessage() != null ? login.getAuthMessage() : "登录失败");
            }
        });
    }

    private void doSwitch() {
        int row = table.getSelectedRow();
        if (row < 0) {
            return;
        }
        Ui.background(frame, () -> {
            controller.switchTo(row);
            return null;
        }, ignored -> {
            refresh();
            frame.refreshAll();
        });
    }

    private void doDelete() {
        int row = table.getSelectedRow();
        if (row < 0) {
            return;
        }
        controller.deleteAccount(row);
        refresh();
        frame.refreshAll();
    }

    public void refresh() {
        model.fireTableDataChanged();
    }

    private class AccountTableModel extends AbstractTableModel {

        private final String[] columns = {"当前", "标签", "用户名", "会员", "到期"};
        private final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

        @Override
        public int getRowCount() {
            return controller.getStore().getAccounts().size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int column) {
            return columns[column];
        }

        @Override
        public Object getValueAt(int row, int column) {
            Account account = controller.getStore().getAccounts().get(row);
            return switch (column) {
                case 0 -> row == controller.getStore().getCurrentIndex() ? "●" : "";
                case 1 -> account.getGroupTitle() != null ? account.getGroupTitle() : account.getLabel();
                case 2 -> account.getUsername();
                case 3 -> account.getMembershipStatus();
                case 4 -> account.getExpireTime() > 0 ? format.format(new Date(account.getExpireTime() * 1000L)) : "";
                default -> "";
            };
        }
    }
}
