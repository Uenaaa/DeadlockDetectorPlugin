package com.deadlock.detector.visualizer;

import com.deadlock.detector.detector.DeadlockDetector;
import com.deadlock.detector.model.GraphNode;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
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
        JPanel mainPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                
                // 绘制面板背景
                g2d.setColor(new Color(255, 255, 255));
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                
                // 绘制阴影效果
                g2d.setColor(new Color(0, 0, 0, 10));
                g2d.fillRoundRect(5, 5, getWidth(), getHeight(), 8, 8);
                g2d.setColor(new Color(0, 0, 0, 5));
                g2d.fillRoundRect(10, 10, getWidth(), getHeight(), 8, 8);
            }
        };
        mainPanel.setPreferredSize(new Dimension(900, 700));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        mainPanel.setBackground(new Color(245, 246, 250));
        
        // 创建样式化的标签页面板
        JTabbedPane tabbedPane = new JTabbedPane() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                
                // 绘制标签页背景
                g2d.setColor(new Color(250, 251, 252));
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        
        // 设置标签页样式
        tabbedPane.setBackground(new Color(250, 251, 252));
        tabbedPane.setForeground(new Color(66, 153, 225));
        tabbedPane.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabbedPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // 创建可视化面板
        JPanel visualPanel = new JPanel(new BorderLayout());
        JScrollPane visualScrollPane = new JScrollPane(visualizerPanel);
        visualScrollPane.setPreferredSize(new Dimension(800, 600));
        visualPanel.add(visualScrollPane, BorderLayout.CENTER);
        
        // 添加控制按钮面板
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        controlPanel.setBorder(new EmptyBorder(10, 0, 10, 0));
        
        // 创建样式化的按钮
        JButton zoomInBtn = createStyledButton("放大");
        zoomInBtn.addActionListener(e -> visualizerPanel.zoomIn());
        
        JButton zoomOutBtn = createStyledButton("缩小");
        zoomOutBtn.addActionListener(e -> visualizerPanel.zoomOut());
        
        JButton resetBtn = createStyledButton("重置视图");
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
            suggestionsPanel.setBackground(new Color(255, 255, 255));
            suggestionsPanel.setBorder(new EmptyBorder(10, 15, 15, 15));
            
            // 添加标题
            JLabel titleLabel = new JLabel("死锁解决方案建议");
            titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
            titleLabel.setForeground(new Color(66, 153, 225));
            titleLabel.setBorder(new EmptyBorder(0, 0, 15, 0));
            suggestionsPanel.add(titleLabel, BorderLayout.NORTH);
            
            // 生成建议
            DeadlockDetector detector = new DeadlockDetector();
            String suggestionsText = detector.generateDeadlockSuggestions(cycles);
            
            // 创建样式化的文本区域
            JTextArea suggestionsTextArea = new JTextArea(suggestionsText) {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2d = (Graphics2D) g;
                    g2d.setColor(new Color(248, 249, 250));
                    g2d.fillRect(0, 0, getWidth(), getHeight());
                    
                    // 恢复原始绘制
                    super.paintComponent(g);
                }
            };
            suggestionsTextArea.setEditable(false);
            suggestionsTextArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            suggestionsTextArea.setLineWrap(true);
            suggestionsTextArea.setWrapStyleWord(true);
            suggestionsTextArea.setBackground(new Color(248, 249, 250));
            suggestionsTextArea.setForeground(new Color(49, 53, 59));
            suggestionsTextArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            
            // 添加滚动条
            JScrollPane suggestionsScrollPane = new JScrollPane(suggestionsTextArea);
            suggestionsScrollPane.setBorder(BorderFactory.createLineBorder(new Color(222, 226, 230), 1));
            suggestionsScrollPane.setBackground(new Color(255, 255, 255));
            
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
    
    /**
     * 创建现代化的圆角按钮
     */
    private JButton createStyledButton(String text) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // 绘制渐变背景
                Color startColor = new Color(66, 153, 225);
                Color endColor = new Color(33, 115, 204);
                GradientPaint gradient = new GradientPaint(0, 0, startColor, 0, getHeight(), endColor);
                g2d.setPaint(gradient);
                
                // 绘制圆角矩形
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                
                // 绘制文本
                g2d.setColor(Color.WHITE);
                FontMetrics metrics = g2d.getFontMetrics();
                int textX = (getWidth() - metrics.stringWidth(getText())) / 2;
                int textY = (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
                g2d.drawString(getText(), textX, textY);
                
                g2d.dispose();
            }
            
            @Override
            protected void paintBorder(Graphics g) {
                // 移除默认边框
            }
        };
        
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setPreferredSize(new Dimension(100, 35));
        button.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        
        return button;
    }
}
