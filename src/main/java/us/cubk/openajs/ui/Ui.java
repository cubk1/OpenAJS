package us.cubk.openajs.ui;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public final class Ui {

    private Ui() {
    }

    public static <T> void background(Component owner, Callable<T> task, Consumer<T> onSuccess) {
        new Thread(() -> {
            try {
                T result = task.call();
                SwingUtilities.invokeLater(() -> onSuccess.accept(result));
            } catch (Exception e) {
                String message = e.getMessage() != null ? e.getMessage() : e.toString();
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(owner, message, "错误", JOptionPane.ERROR_MESSAGE));
            }
        }).start();
    }

    public static void error(Component owner, String message) {
        JOptionPane.showMessageDialog(owner, message, "错误", JOptionPane.ERROR_MESSAGE);
    }

    public static void info(Component owner, String message) {
        JOptionPane.showMessageDialog(owner, message, "提示", JOptionPane.INFORMATION_MESSAGE);
    }
}
