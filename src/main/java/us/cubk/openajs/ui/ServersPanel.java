package us.cubk.openajs.ui;

import us.cubk.openajs.api.model.VpnServer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ServersPanel extends JPanel {

    private final OpenAjsFrame frame;
    private final AppController controller;
    private final List<VpnServer> all = new ArrayList<>();
    private final List<VpnServer> shown = new ArrayList<>();
    private final ServerTableModel model = new ServerTableModel();
    private final JTable table = new JTable(model);
    private final JTextField search = new JTextField(24);
    private final JLabel count = new JLabel("未加载");

    public ServersPanel(OpenAjsFrame frame, AppController controller) {
        super(new BorderLayout(8, 8));
        this.frame = frame;
        this.controller = controller;
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        table.setRowHeight(24);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton load = new JButton("加载节点列表");
        load.addActionListener(e -> load());
        top.add(load);
        top.add(new JLabel("搜索"));
        top.add(search);
        top.add(count);
        add(top, BorderLayout.NORTH);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton choose = new JButton("选为连接节点");
        choose.addActionListener(e -> choose());
        bottom.add(choose);
        add(bottom, BorderLayout.SOUTH);

        search.getDocument().addDocumentListener((SimpleDocumentListener) e -> filter());
    }

    private void load() {
        count.setText("加载中...");
        Ui.background(frame, controller::servers, (Map<String, VpnServer> servers) -> {
            all.clear();
            all.addAll(servers.values());
            filter();
        });
    }

    private void filter() {
        String keyword = search.getText().trim().toLowerCase();
        shown.clear();
        for (VpnServer server : all) {
            String hay = (server.getTit() + " " + server.getCtry() + " " + server.getSvr() + " " + server.getCmt()).toLowerCase();
            if (keyword.isEmpty() || hay.contains(keyword)) {
                shown.add(server);
            }
        }
        model.fireTableDataChanged();
        count.setText(shown.size() + " / " + all.size() + " 节点");
    }

    private void choose() {
        int row = table.getSelectedRow();
        if (row < 0) {
            return;
        }
        frame.setSelectedServer(shown.get(row));
        frame.refreshAll();
    }

    private interface SimpleDocumentListener extends DocumentListener {
        void update(DocumentEvent e);

        default void insertUpdate(DocumentEvent e) {
            update(e);
        }

        default void removeUpdate(DocumentEvent e) {
            update(e);
        }

        default void changedUpdate(DocumentEvent e) {
            update(e);
        }
    }

    private class ServerTableModel extends AbstractTableModel {

        private final String[] columns = {"名称", "国家", "地址", "端口", "状态"};

        @Override
        public int getRowCount() {
            return shown.size();
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
            VpnServer server = shown.get(row);
            return switch (column) {
                case 0 -> server.getTit();
                case 1 -> server.getCtry();
                case 2 -> server.getSvr();
                case 3 -> server.port();
                case 4 -> server.getSts();
                default -> "";
            };
        }
    }
}
