package com.example.portmonitor;

import burp.api.montoya.MontoyaApi;
import com.formdev.flatlaf.FlatLightLaf;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class PortMonitorTab extends JPanel {
    private final MontoyaApi api;
    private final JTextField hostField;
    private final JTextField portInputField;
    private final JButton startButton, stopButton, summaryButton, exportSummaryButton, exportChartButton;
    private final TimeSeriesCollection dataset;
    private final JFreeChart chart;
    private ExecutorService executorService;
    private final List<String> eventLog = Collections.synchronizedList(new ArrayList<>());

    public PortMonitorTab(MontoyaApi api) {
        this.api = api;
        
        // Apply FlatLaf to this panel specifically
        FlatLightLaf.setup();
        SwingUtilities.updateComponentTreeUI(this);

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- Top Panel ---
        JPanel topPanel = new JPanel(new BorderLayout());
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.X_AXIS));

        hostField = new JTextField("127.0.0.1", 15);
        portInputField = new JTextField(20);
        startButton = new JButton("Start");
        stopButton = new JButton("Stop");

        inputPanel.add(new JLabel("Host/IP:"));
        inputPanel.add(Box.createRigidArea(new Dimension(5, 0)));
        inputPanel.add(hostField);
        inputPanel.add(Box.createRigidArea(new Dimension(15, 0)));
        inputPanel.add(new JLabel("Ports:"));
        inputPanel.add(Box.createRigidArea(new Dimension(5, 0)));
        inputPanel.add(portInputField);
        inputPanel.add(Box.createHorizontalGlue());
        inputPanel.add(startButton);
        inputPanel.add(Box.createRigidArea(new Dimension(5, 0)));
        inputPanel.add(stopButton);

        // --- Controls Panel ---
        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        summaryButton = new JButton("Show Summary");
        exportSummaryButton = new JButton("Export Summary");
        exportChartButton = new JButton("Export Chart");

        controlsPanel.add(summaryButton);
        controlsPanel.add(exportSummaryButton);
        controlsPanel.add(exportChartButton);

        topPanel.add(inputPanel, BorderLayout.NORTH);
        topPanel.add(controlsPanel, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.NORTH);

        // --- Chart Panel ---
        dataset = new TimeSeriesCollection();
        chart = createChart(dataset);
        ChartPanel chartPanel = new ChartPanel(chart);
        add(chartPanel, BorderLayout.CENTER);

        // Initial button states
        stopButton.setEnabled(false);
        summaryButton.setEnabled(false);
        exportSummaryButton.setEnabled(false);
        exportChartButton.setEnabled(false);

        // --- Actions ---
        startButton.addActionListener(e -> startMonitoring());
        stopButton.addActionListener(e -> stopMonitoring());
        summaryButton.addActionListener(e -> showSummary());
        exportSummaryButton.addActionListener(e -> exportSummary());
        exportChartButton.addActionListener(e -> exportChart());
    }

    private JFreeChart createChart(TimeSeriesCollection dataset) {
        JFreeChart timeSeriesChart = ChartFactory.createTimeSeriesChart(
                "Port Status Monitor", "Time (HH:mm:ss)", "Port", dataset, true, true, false);
        XYPlot plot = (XYPlot) timeSeriesChart.getPlot();
        DateAxis axis = (DateAxis) plot.getDomainAxis();
        axis.setDateFormatOverride(new SimpleDateFormat("HH:mm:ss.SSS"));

        // Force Y-axis to use integer values instead of scientific notation
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        return timeSeriesChart;
    }

    private void startMonitoring() {
        dataset.removeAllSeries();
        eventLog.clear();
        String host = hostField.getText();
        String[] portStrings = portInputField.getText().split(",");

        executorService = Executors.newFixedThreadPool(portStrings.length);
        AtomicInteger portIndex = new AtomicInteger(1);

        for (String portString : portStrings) {
            try {
                int port = Integer.parseInt(portString.trim());
                int yLevel = portIndex.getAndIncrement();
                TimeSeries series = new TimeSeries("Port " + port);
                dataset.addSeries(series);
                executorService.submit(() -> monitorPort(host, port, series, yLevel));
            } catch (NumberFormatException ex) {
                api.logging().logToError("Invalid port: " + portString);
            }
        }
        setMonitoringState(true);
    }

    private void stopMonitoring() {
        if (executorService != null) {
            executorService.shutdownNow();
        }
        setMonitoringState(false);
    }

    private void setMonitoringState(boolean isMonitoring) {
        startButton.setEnabled(!isMonitoring);
        stopButton.setEnabled(isMonitoring);
        summaryButton.setEnabled(true); // Always enabled after first run
        exportSummaryButton.setEnabled(true);
        exportChartButton.setEnabled(true);
    }

    private void showSummary() {
        JTextArea textArea = new JTextArea(25, 60);
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        StringBuilder summary = new StringBuilder("Session Event Log:\n\n");
        for (String event : eventLog) {
            summary.append(event).append("\n");
        }
        textArea.setText(summary.toString());
        JScrollPane scrollPane = new JScrollPane(textArea);
        JDialog summaryDialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Session Summary", Dialog.ModalityType.MODELESS);
        summaryDialog.add(scrollPane);
        summaryDialog.pack();
        summaryDialog.setLocationRelativeTo(this);
        summaryDialog.setVisible(true);
    }

    private void exportSummary() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Summary");
        fileChooser.setSelectedFile(new File("port-summary.txt"));
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            try (FileWriter writer = new FileWriter(fileToSave)) {
                for (String event : eventLog) {
                    writer.write(event + "\n");
                }
                api.logging().logToOutput("Summary exported to " + fileToSave.getAbsolutePath());
            } catch (IOException e) {
                api.logging().logToError("Failed to export summary: " + e.getMessage());
            }
        }
    }

    private void exportChart() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Chart");
        fileChooser.setSelectedFile(new File("port-chart.png"));
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            try {
                ChartUtils.saveChartAsPNG(fileToSave, chart, 800, 600);
                api.logging().logToOutput("Chart exported to " + fileToSave.getAbsolutePath());
            } catch (IOException e) {
                api.logging().logToError("Failed to export chart: " + e.getMessage());
            }
        }
    }

    private void monitorPort(String host, int port, TimeSeries series, int yLevel) {
        boolean wasUp = false;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

        while (!Thread.currentThread().isInterrupted()) {
            boolean isUp = isPortOpen(host, port);
            int status = isUp ? yLevel : 0;
            Date now = new Date();
            SwingUtilities.invokeLater(() -> series.addOrUpdate(new Millisecond(now), status));

            if (isUp != wasUp) {
                String event = String.format("[%s] Port %d on %s went %s", sdf.format(now), port, host, isUp ? "UP" : "DOWN");
                eventLog.add(event);
                wasUp = isUp;
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private boolean isPortOpen(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 1000);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
