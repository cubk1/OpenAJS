package us.cubk.openajs.ui;

import us.cubk.openajs.api.AjsClient;
import us.cubk.openajs.api.model.CaptchaResult;
import us.cubk.openajs.api.model.RegisterResult;
import us.cubk.openajs.api.model.ResetPasswordResult;
import us.cubk.openajs.api.model.VerifyCodeResult;
import us.cubk.openajs.api.util.AccountUtil;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Window;

public class AuthDialog extends JDialog {

    private final AppController controller;
    private final boolean reset;
    private final JTextField accountField = new JTextField(20);
    private final JPasswordField passwordField = new JPasswordField(20);
    private final JTextField invitationField = new JTextField(20);
    private final JTextField captchaField = new JTextField(10);
    private final JLabel captchaImage = new JLabel("点击获取");
    private final JTextField verifyField = new JTextField(10);

    public AuthDialog(Window owner, AppController controller, boolean reset) {
        super(owner, reset ? "重置密码" : "注册新账号", ModalityType.APPLICATION_MODAL);
        this.controller = controller;
        this.reset = reset;
        build();
        pack();
        setLocationRelativeTo(owner);
    }

    private void build() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 12, 12, 12));
        int row = 0;
        add(panel, "手机号或邮箱", accountField, row++);
        add(panel, reset ? "新密码" : "密码", passwordField, row++);
        if (!reset) {
            add(panel, "邀请码(可选)", invitationField, row++);
        }

        JButton loadCaptcha = new JButton("获取图形码");
        loadCaptcha.addActionListener(e -> loadCaptcha());
        captchaImage.setPreferredSize(new java.awt.Dimension(150, 50));
        captchaImage.setBorder(javax.swing.BorderFactory.createLineBorder(java.awt.Color.GRAY));
        add(panel, "图形验证码", captchaImage, row++);
        add(panel, "", loadCaptcha, row++);
        add(panel, "输入图形码", captchaField, row++);

        JButton sendCode = new JButton("发送验证码");
        sendCode.addActionListener(e -> sendCode());
        add(panel, "", sendCode, row++);
        add(panel, "短信/邮箱验证码", verifyField, row++);

        JButton submit = new JButton(reset ? "重置密码" : "注册");
        submit.addActionListener(e -> submit());
        add(panel, "", submit, row++);

        setContentPane(panel);
    }

    private void add(JPanel panel, String label, java.awt.Component field, int row) {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.gridx = 0;
        c.gridy = row;
        c.anchor = GridBagConstraints.EAST;
        panel.add(new JLabel(label), c);
        c.gridx = 1;
        c.anchor = GridBagConstraints.WEST;
        panel.add(field, c);
    }

    private String account() {
        return accountField.getText().trim();
    }

    private void loadCaptcha() {
        String usage = reset ? "ResetPassword" : "Register";
        String mode = reset ? null : AccountUtil.mode(account());
        AjsClient client = controller.getClient();
        Ui.background(this, () -> client.captcha(usage, mode), this::showCaptcha);
    }

    private void showCaptcha(CaptchaResult result) {
        byte[] bytes = result.imageBytes();
        if (bytes.length == 0) {
            Ui.error(this, result.getErrorMessage() != null ? result.getErrorMessage() : "验证码为空");
            return;
        }
        Image image = new ImageIcon(bytes).getImage().getScaledInstance(150, 50, Image.SCALE_SMOOTH);
        captchaImage.setText("");
        captchaImage.setIcon(new ImageIcon(image));
    }

    private void sendCode() {
        String usage = reset ? "ResetPassword" : "Register";
        AjsClient client = controller.getClient();
        String account = account();
        String captchaText = captchaField.getText().trim();
        Ui.background(this, () -> client.sendVerifyCode(account, captchaText, usage), (VerifyCodeResult result) -> {
            Ui.info(this, result.isSuccess() ? ("已发送, 倒计时 " + result.getCountdown() + "s") : ("发送失败: " + result.getMessage()));
        });
    }

    private void submit() {
        AjsClient client = controller.getClient();
        String account = account();
        String password = new String(passwordField.getPassword());
        String verify = verifyField.getText().trim();
        if (reset) {
            Ui.background(this, () -> client.resetPassword(account, verify, password), (ResetPasswordResult result) -> {
                if (result.isSuccess()) {
                    Ui.info(this, "重置成功");
                    dispose();
                } else {
                    Ui.error(this, result.getMessage() != null ? result.getMessage() : "重置失败");
                }
            });
        } else {
            String invitation = invitationField.getText().trim();
            Ui.background(this, () -> client.register(account, password, verify, invitation), (RegisterResult result) -> {
                if (result.isSuccess()) {
                    Ui.background(this, () -> controller.login(result.getAccountName(), result.getPassword()), r -> {
                        Ui.info(this, "注册成功并已登录: " + r.getUserName());
                        dispose();
                    });
                } else {
                    Ui.error(this, result.getErrorMessage() != null ? result.getErrorMessage() : "注册失败");
                }
            });
        }
    }
}
