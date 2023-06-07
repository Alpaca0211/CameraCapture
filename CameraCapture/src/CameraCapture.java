import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;
import org.opencv.imgproc.Imgproc;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class CameraCapture extends JFrame {

    private static final int FRAME_WIDTH = 640;
    private static final int FRAME_HEIGHT = 480;
    private static final int CAPTURE_INTERVAL_MINUTES = 1;
    private static final Scalar FONT_COLOR = new Scalar(255, 255, 255);
    private static final int FONT_THICKNESS = 2;

    private VideoCapture videoCapture;
    private Timer captureTimer;
    private int captureCounter;

    private JButton startButton;
    private JButton stopButton;
    private JLabel statusLabel;
    private JLabel previewLabel;
    private JComboBox<String> resolutionComboBox;
    private JTextField intervalTextField;

    private Dimension[] resolutions = {
            new Dimension(640, 480),
            new Dimension(1280, 720),
            new Dimension(1920, 1080)
    };

    public CameraCapture() {
        setTitle("定点カメラくん");
        setSize(FRAME_WIDTH, FRAME_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        startButton = new JButton("撮影開始");
        stopButton = new JButton("撮影終了");
        statusLabel = new JLabel("Status: Not Started");
        previewLabel = new JLabel();
        resolutionComboBox = new JComboBox<>();
        intervalTextField = new JTextField(String.valueOf(CAPTURE_INTERVAL_MINUTES));

        for (Dimension resolution : resolutions) {
            resolutionComboBox.addItem(resolution.width + "x" + resolution.height);
        }
        resolutionComboBox.setSelectedIndex(0);

        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startCapture();
                updateButtonStates(true);
                statusLabel.setText("Status: Started");
            }
        });

        stopButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stopCapture();
                updateButtonStates(false);
                statusLabel.setText("Status: Stopped");
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(startButton);
        buttonPanel.add(stopButton);

        JPanel settingPanel = new JPanel();
        settingPanel.add(new JLabel("解像度:"));
        settingPanel.add(resolutionComboBox);
        settingPanel.add(new JLabel("撮影間隔（分）:"));
        settingPanel.add(intervalTextField);

        setLayout(new BorderLayout());
        add(buttonPanel, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
        add(previewLabel, BorderLayout.NORTH);
        add(settingPanel, BorderLayout.EAST);
    }

    private void startCapture() {
        Dimension resolution = resolutions[resolutionComboBox.getSelectedIndex()];
        int captureInterval = Integer.parseInt(intervalTextField.getText()) * 60 * 1000;

        videoCapture = new VideoCapture(0); // 0はカメラデバイスのIDで、複数のカメラが接続されている場合に適宜変更する

        if (videoCapture.isOpened()) {
            videoCapture.set(3, resolution.getWidth()); // Width
            videoCapture.set(4, resolution.getHeight()); // Height
            captureCounter = 1;

            // タイマータスクを作成して指定の間隔で撮影を行う
            captureTimer = new Timer();
            captureTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    Mat frame = new Mat();
                    videoCapture.read(frame);

                    // 画像に時刻と撮影番号を挿入
                    addTimestampText(frame);

                    // 画像を保存
                    saveImage(frame);

                    // プレビューを更新
                    updatePreview(frame);

                    captureCounter++;
                }
            }, 0, captureInterval);
        } else {
            JOptionPane.showMessageDialog(this, "Failed to open camera.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void stopCapture() {
        if (captureTimer != null) {
            captureTimer.cancel();
            captureTimer = null;
        }

        if (videoCapture != null && videoCapture.isOpened()) {
            videoCapture.release();
            videoCapture = null;
        }
    }

    private void addTimestampText(Mat frame) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String timestamp = dateFormat.format(new Date());
        String text = timestamp + "_" + captureCounter;

        int baseline[] = {0};
        Size textSize = Imgproc.getTextSize(text, Imgproc.FONT_HERSHEY_SIMPLEX, FONT_THICKNESS, FONT_THICKNESS, baseline);
        org.opencv.core.Point textOrg = new org.opencv.core.Point(
                frame.cols() - textSize.width - 10,
                frame.rows() - 10);

        Imgproc.putText(
                frame,
                text,
                textOrg,
                Imgproc.FONT_HERSHEY_SIMPLEX,
                FONT_THICKNESS,
                FONT_COLOR,
                FONT_THICKNESS);
    }

    private void saveImage(Mat frame) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String timestamp = dateFormat.format(new Date());
        String fileName = timestamp + "_" + captureCounter + ".jpg";
        String outputPath = "D:/capture/" + fileName; // 保存先のフォルダパスを適宜変更する

        Imgcodecs.imwrite(outputPath, frame);
    }

    private void updatePreview(Mat frame) {
        BufferedImage image = matToBufferedImage(frame);
        ImageIcon icon = new ImageIcon(image.getScaledInstance(FRAME_WIDTH, FRAME_HEIGHT, Image.SCALE_DEFAULT));
        previewLabel.setIcon(icon);
    }

    private BufferedImage matToBufferedImage(Mat frame) {
        int type = BufferedImage.TYPE_BYTE_GRAY;
        if (frame.channels() > 1) {
            type = BufferedImage.TYPE_3BYTE_BGR;
        }
        BufferedImage image = new BufferedImage(frame.cols(), frame.rows(), type);
        frame.get(0, 0, ((DataBufferByte) image.getRaster().getDataBuffer()).getData());
        return image;
    }

    private void updateButtonStates(boolean capturing) {
        startButton.setEnabled(!capturing);
        stopButton.setEnabled(capturing);
        resolutionComboBox.setEnabled(!capturing);
        intervalTextField.setEditable(!capturing);
    }
    //メインメソッド
    public static void main(String[] args) {
        // OpenCVの初期化
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                CameraCapture gui = new CameraCapture();
                gui.setVisible(true);
            }
        });
    }
}
