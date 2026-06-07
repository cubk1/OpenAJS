package us.cubk.openajs.ui;

import us.cubk.openajs.api.AjsClient;
import us.cubk.openajs.api.model.AnnounceResult;
import us.cubk.openajs.api.model.NotificationCard;
import us.cubk.openajs.api.model.NotificationListResult;
import us.cubk.openajs.api.model.UnreadCountResult;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

public class MessagesPanel extends JPanel {

    private final OpenAjsFrame frame;
    private final AppController controller;
    private final JTextArea area = new JTextArea();

    public MessagesPanel(OpenAjsFrame frame, AppController controller) {
        super(new BorderLayout(8, 8));
        this.frame = frame;
        this.controller = controller;
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        area.setEditable(false);
        add(new JScrollPane(area), BorderLayout.CENTER);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refresh = new JButton("刷新消息");
        refresh.addActionListener(e -> load());
        top.add(refresh);
        add(top, BorderLayout.NORTH);
    }

    private void load() {
        if (!controller.hasSession()) {
            Ui.error(frame, "请先登录账号");
            return;
        }
        AjsClient client = controller.getClient();
        StringBuilder sb = new StringBuilder();
        Ui.background(frame, () -> {
            UnreadCountResult unread = client.getUnreadNotificationCount();
            AnnounceResult announce = client.getAnnounce();
            NotificationListResult list = client.getNotificationList(20, null);
            sb.append("未读通知: ").append(unread.getCount()).append("\n\n");
            sb.append("=== 公告 ===\n").append(announce.getContent() != null ? announce.getContent() : "(无)").append("\n\n");
            sb.append("=== 通知列表 ===\n");
            if (list.getList() != null) {
                for (NotificationCard card : list.getList()) {
                    sb.append("[").append(card.getId()).append("] ").append(card.getNotificationTitle()).append("\n");
                    sb.append(card.getNotificationContent()).append("\n\n");
                }
            }
            return sb.toString();
        }, area::setText);
    }
}
