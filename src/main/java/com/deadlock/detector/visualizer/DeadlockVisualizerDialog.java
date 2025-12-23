package com.deadlock.detector.visualizer;

import com.deadlock.detector.detector.DeadlockDetector;
import com.deadlock.detector.model.GraphNode;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

public class DeadlockVisualizerDialog extends DialogWrapper {
    private final DeadlockVisualizerPanel visualizerPanel;
    private final boolean hasDeadlock;
    private final List<List<GraphNode>> cycles;

    public DeadlockVisualizerDialog(List<GraphNode> allNodes, List<List<GraphNode>> cycles, boolean hasDeadlock) {
        super(true); // 使用模态对话框
        this.visualizerPanel = new DeadlockVisualizerPanel(allNodes, cycles);
        this.hasDeadlock = hasDeadlock;
        this.cycles = cycles;
        setTitle(hasDeadlock ? "死锁检测结果 - 检测到死锁" : "死锁检测结果 - 未检测到死锁");
        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        // 创建主面板
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setPreferredSize(new Dimension(900, 700));
        
        // 创建标签页面板
        JTabbedPane tabbedPane = new JTabbedPane();
        
        // 创建可视化面板
        JPanel visualPanel = new JPanel(new BorderLayout());
        JScrollPane visualScrollPane = new JScrollPane(visualizerPanel);
        visualScrollPane.setPreferredSize(new Dimension(800, 600));
        visualPanel.add(visualScrollPane, BorderLayout.CENTER);
        
        // 添加控制按钮面板
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton zoomInBtn = new JButton("放大");
        zoomInBtn.addActionListener(e -> visualizerPanel.zoomIn());
        JButton zoomOutBtn = new JButton("缩小");
        zoomOutBtn.addActionListener(e -> visualizerPanel.zoomOut());
        JButton resetBtn = new JButton("重置视图");
        resetBtn.addActionListener(e -> visualizerPanel.resetView());
        controlPanel.add(zoomInBtn);
        controlPanel.add(zoomOutBtn);
        controlPanel.add(resetBtn);
        visualPanel.add(controlPanel, BorderLayout.SOUTH);
        
        // 添加可视化标签页
        tabbedPane.addTab("死锁可视化", visualPanel);
        
        // 创建建议面板
        if (hasDeadlock) {
            JPanel suggestionsPanel = new JPanel(new BorderLayout());
            
            // 生成建议
            DeadlockDetector detector = new DeadlockDetector();
            String suggestionsText = detector.generateDeadlockSuggestions(cycles);
            
            // 创建文本区域显示建议
            JTextArea suggestionsTextArea = new JTextArea(suggestionsText);
            suggestionsTextArea.setEditable(false);
            suggestionsTextArea.setFont(new Font("Arial", Font.PLAIN, 14));
            suggestionsTextArea.setLineWrap(true);
            suggestionsTextArea.setWrapStyleWord(true);
            
            // 添加滚动条
            JScrollPane suggestionsScrollPane = new JScrollPane(suggestionsTextArea);
            suggestionsScrollPane.setBorder(new EmptyBorder(10, 10, 10, 10));
            
            suggestionsPanel.add(suggestionsScrollPane, BorderLayout.CENTER);
            
            // 添加建议标签页
            tabbedPane.addTab("解决方案建议", suggestionsPanel);
        }
        
        // 添加标签页面板到主面板
        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        
        return mainPanel;
    }
    
    @Override
    protected void createDefaultActions() {
        super.createDefaultActions();
        // 只保留关闭按钮
        myOKAction.setEnabled(false);
        // 设置取消按钮文本为关闭
        setCancelButtonText("关闭");
    }
}
