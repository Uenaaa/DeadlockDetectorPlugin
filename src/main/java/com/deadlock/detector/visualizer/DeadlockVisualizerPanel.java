package com.deadlock.detector.visualizer;

import com.deadlock.detector.model.GraphNode;
import com.deadlock.detector.model.NodeType;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeadlockVisualizerPanel extends JPanel {
    private final List<GraphNode> allNodes;
    private final List<List<GraphNode>> cycles;
    private final Map<GraphNode, Point> nodePositions;
    private final int nodeRadius = 35; // 增大节点大小
    private final int lockNodeWidth = 80; // 锁节点宽度
    private final int lockNodeHeight = 40; // 锁节点高度
    private final int padding = 80;
    private double scale = 1.0;
    private int offsetX = 0;
    private int offsetY = 0;
    
    // 现代化配色方案
    private static final Color BG_COLOR = new Color(245, 246, 250); // 浅灰色背景
    private static final Color PROCESS_NODE_COLOR = new Color(66, 153, 225); // 蓝色 - 进程
    private static final Color LOCK_NODE_COLOR = new Color(239, 83, 80); // 红色 - 锁
    private static final Color SELECTED_NODE_COLOR = new Color(255, 193, 7); // 黄色 - 选中
    private static final Color CYCLE_EDGE_COLOR = new Color(103, 58, 183); // 紫色 - 死锁边
    private static final Color NORMAL_EDGE_COLOR = new Color(171, 184, 195); // 浅灰色 - 普通边
    private static final Color ARROW_COLOR = new Color(99, 110, 114); // 深灰色 - 箭头
    private static final Color PROCESS_TEXT_COLOR = Color.WHITE;
    private static final Color LOCK_TEXT_COLOR = Color.WHITE;
    
    // 字体设置
    private static final Font NODE_FONT = new Font("Segoe UI", Font.BOLD, 14);
    private static final Font LABEL_FONT = new Font("Segoe UI", Font.PLAIN, 12);

    public DeadlockVisualizerPanel(List<GraphNode> allNodes, List<List<GraphNode>> cycles) {
        this.allNodes = allNodes != null ? allNodes : new ArrayList<>();
        this.cycles = cycles != null ? cycles : new ArrayList<>();
        this.nodePositions = new HashMap<>();
        this.setBackground(BG_COLOR);
        this.setPreferredSize(new Dimension(1000, 700));
        
        // 自动布局节点
        layoutNodes();
        
        // 添加测试代码，确保面板尺寸合适
        this.setMinimumSize(new Dimension(500, 300));
        this.setSize(1000, 700);
    }

    private void layoutNodes() {
        // 使用实际尺寸（如果可用），否则使用首选尺寸
        int actualWidth = getWidth();
        int actualHeight = getHeight();
        Dimension preferredSize = getPreferredSize();
        
        int width = (actualWidth > 2 * padding) ? (actualWidth - 2 * padding) : (preferredSize.width - 2 * padding);
        int height = (actualHeight > 2 * padding) ? (actualHeight - 2 * padding) : (preferredSize.height - 2 * padding);
        
        System.out.println("layoutNodes() called - allNodes size: " + allNodes.size());
        System.out.println("Actual panel size: " + getSize());
        System.out.println("Preferred panel size: " + preferredSize);
        System.out.println("Available drawing area: " + width + "x" + height);
        
        if (allNodes.isEmpty()) {
            System.out.println("No nodes to layout");
            return;
        }
        
        // 分离进程节点和锁节点
        List<GraphNode> processNodes = new ArrayList<>();
        List<GraphNode> lockNodes = new ArrayList<>();
        
        for (GraphNode node : allNodes) {
            if (node.getType() == NodeType.PROCESS) {
                processNodes.add(node);
            } else {
                lockNodes.add(node);
            }
        }
        
        int centerX = padding + width / 2;
        int centerY = padding + height / 2;
        
        // 布局进程节点（圆形）
        int processCount = processNodes.size();
        if (processCount > 0) {
            double processAngleStep = 2 * Math.PI / processCount;
            int processRadius = Math.min(width, height) / 4;
            
            for (int i = 0; i < processCount; i++) {
                GraphNode node = processNodes.get(i);
                double angle = i * processAngleStep;
                int x = centerX + (int) (Math.cos(angle) * processRadius);
                int y = centerY + (int) (Math.sin(angle) * processRadius);
                nodePositions.put(node, new Point(x, y));
            }
        }
        
        // 布局锁节点（水平排列在下方）
        int lockCount = lockNodes.size();
        if (lockCount > 0) {
            int lockY = centerY + height / 3;
            int lockStartX = centerX - (lockCount * (lockNodeWidth + 40)) / 2;
            
            for (int i = 0; i < lockCount; i++) {
                GraphNode node = lockNodes.get(i);
                int x = lockStartX + i * (lockNodeWidth + 40) + lockNodeWidth / 2;
                int y = lockY;
                nodePositions.put(node, new Point(x, y));
            }
        }
        
        // 如果只有一种类型的节点，使用更紧凑的布局
        if (processCount == 0 || lockCount == 0) {
            int totalNodes = allNodes.size();
            double angleStep = 2 * Math.PI / totalNodes;
            int radius = Math.min(width, height) / 3;
            
            for (int i = 0; i < totalNodes; i++) {
                GraphNode node = allNodes.get(i);
                double angle = i * angleStep;
                int x = centerX + (int) (Math.cos(angle) * radius);
                int y = centerY + (int) (Math.sin(angle) * radius);
                nodePositions.put(node, new Point(x, y));
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        
        System.out.println("paintComponent() called - actual panel size: " + getSize());
        System.out.println("Number of nodes: " + allNodes.size());
        System.out.println("Number of node positions: " + nodePositions.size());
        
        // 设置抗锯齿和高质量渲染
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        
        // 绘制背景
        g2d.setColor(BG_COLOR);
        g2d.fillRect(0, 0, getWidth(), getHeight());
        
        // 重置缩放和平移，避免绘制到不可见区域
        g2d.setTransform(new AffineTransform());
        
        // 确保节点位置已计算
        if (nodePositions.isEmpty() && !allNodes.isEmpty()) {
            layoutNodes();
        }
        
        // 绘制边
        for (GraphNode node : allNodes) {
            Point nodePos = nodePositions.get(node);
            for (GraphNode neighbor : node.getOutgoingEdges()) {
                Point neighborPos = nodePositions.get(neighbor);
                
                // 检查边是否在死锁循环中
                boolean isInCycle = isEdgeInCycle(node, neighbor);
                
                // 设置边的样式
                if (isInCycle) {
                    g2d.setColor(CYCLE_EDGE_COLOR);
                    g2d.setStroke(new BasicStroke(4.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                } else {
                    g2d.setColor(NORMAL_EDGE_COLOR);
                    g2d.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                }
                
                // 绘制边
                g2d.draw(new Line2D.Double(nodePos.x, nodePos.y, neighborPos.x, neighborPos.y));
                
                // 绘制箭头
                drawModernArrow(g2d, node, nodePos, neighbor, neighborPos, isInCycle);
            }
        }
        
        // 绘制节点
        for (GraphNode node : allNodes) {
            Point pos = nodePositions.get(node);
            boolean isInCycle = isNodeInCycle(node);
            
            if (node.getType() == NodeType.PROCESS) {
                // 绘制进程节点（圆形）
                drawProcessNode(g2d, node, pos, isInCycle);
            } else {
                // 绘制锁节点（圆角矩形）
                drawLockNode(g2d, node, pos, isInCycle);
            }
        }
    }
    
    /**
     * 检查边是否在死锁循环中
     */
    private boolean isEdgeInCycle(GraphNode from, GraphNode to) {
        for (List<GraphNode> cycle : cycles) {
            for (int i = 0; i < cycle.size() - 1; i++) {
                if (cycle.get(i) == from && cycle.get(i + 1) == to) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * 检查节点是否在死锁循环中
     */
    private boolean isNodeInCycle(GraphNode node) {
        for (List<GraphNode> cycle : cycles) {
            if (cycle.contains(node)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 绘制进程节点（圆形）
     */
    private void drawProcessNode(Graphics2D g2d, GraphNode node, Point pos, boolean isInCycle) {
        // 移除发光效果，保持界面简洁
        
        // 绘制节点阴影
        g2d.setColor(new Color(0, 0, 0, 30));
        g2d.fill(new Ellipse2D.Double(
                pos.x - nodeRadius + 3,
                pos.y - nodeRadius + 3,
                2 * nodeRadius,
                2 * nodeRadius
        ));
        
        // 绘制节点主体
        g2d.setColor(PROCESS_NODE_COLOR);
        g2d.fill(new Ellipse2D.Double(
                pos.x - nodeRadius,
                pos.y - nodeRadius,
                2 * nodeRadius,
                2 * nodeRadius
        ));
        
        // 绘制节点边框
        g2d.setColor(new Color(41, 128, 185)); // 深蓝色边框
        g2d.setStroke(new BasicStroke(3.0f));
        g2d.draw(new Ellipse2D.Double(
                pos.x - nodeRadius,
                pos.y - nodeRadius,
                2 * nodeRadius,
                2 * nodeRadius
        ));
        
        // 绘制节点标签
        drawNodeLabel(g2d, node, pos, PROCESS_TEXT_COLOR);
    }
    
    /**
     * 绘制锁节点（圆角矩形）
     */
    private void drawLockNode(Graphics2D g2d, GraphNode node, Point pos, boolean isInCycle) {
        // 移除发光效果，保持界面简洁
        
        // 绘制节点阴影
        g2d.setColor(new Color(0, 0, 0, 30));
        g2d.fill(new RoundRectangle2D.Double(
                pos.x - lockNodeWidth/2 + 3,
                pos.y - lockNodeHeight/2 + 3,
                lockNodeWidth,
                lockNodeHeight,
                15, 15
        ));
        
        // 绘制节点主体
        g2d.setColor(LOCK_NODE_COLOR);
        g2d.fill(new RoundRectangle2D.Double(
                pos.x - lockNodeWidth/2,
                pos.y - lockNodeHeight/2,
                lockNodeWidth,
                lockNodeHeight,
                15, 15
        ));
        
        // 绘制节点边框
        g2d.setColor(new Color(183, 28, 28)); // 深红色边框
        g2d.setStroke(new BasicStroke(3.0f));
        g2d.draw(new RoundRectangle2D.Double(
                pos.x - lockNodeWidth/2,
                pos.y - lockNodeHeight/2,
                lockNodeWidth,
                lockNodeHeight,
                15, 15
        ));
        
        // 绘制节点标签
        drawNodeLabel(g2d, node, pos, LOCK_TEXT_COLOR);
    }
    
    /**
     * 绘制节点标签
     */
    private void drawNodeLabel(Graphics2D g2d, GraphNode node, Point pos, Color textColor) {
        g2d.setColor(textColor);
        g2d.setFont(NODE_FONT);
        FontMetrics metrics = g2d.getFontMetrics();
        
        // 处理长标签
        String label = node.getId();
        if (label.length() > 12) {
            label = label.substring(0, 9) + "...";
        }
        
        int textWidth = metrics.stringWidth(label);
        int textHeight = metrics.getAscent();
        
        // 绘制文字阴影
        g2d.setColor(new Color(0, 0, 0, 50));
        g2d.drawString(label, pos.x - textWidth / 2 + 1, pos.y + textHeight / 2 + 1);
        
        // 绘制文字
        g2d.setColor(textColor);
        g2d.drawString(label, pos.x - textWidth / 2, pos.y + textHeight / 2);
    }

    /**
     * 绘制现代化箭头
     */
    private void drawModernArrow(Graphics2D g2d, GraphNode fromNode, Point from, GraphNode toNode, Point to, boolean isInCycle) {
        double angle = Math.atan2(to.y - from.y, to.x - from.x);
        int arrowLength = 20;
        int arrowAngle = 25;
        
        // 计算箭头起点（在边上，距离节点有一段距离）
        double distance = Math.sqrt(Math.pow(to.x - from.x, 2) + Math.pow(to.y - from.y, 2));
        double ratio = isInCycle ? 1.0 : 1.0;
        
        // 根据节点类型调整箭头起点
        double offset;
        if (toNode.getType() == NodeType.RESOURCE) {
            // 锁节点是圆角矩形，计算合适的偏移量
            offset = Math.sqrt(Math.pow(lockNodeWidth/2, 2) + Math.pow(lockNodeHeight/2, 2));
        } else {
            // 进程节点是圆形
            offset = nodeRadius;
        }
        
        double startX = to.x - offset * Math.cos(angle) * ratio;
        double startY = to.y - offset * Math.sin(angle) * ratio;
        
        // 绘制箭头头部
        g2d.setStroke(new BasicStroke(isInCycle ? 3.0f : 2.0f));
        g2d.setColor(isInCycle ? CYCLE_EDGE_COLOR : ARROW_COLOR);
        
        // 创建箭头路径
        Path2D arrowHead = new Path2D.Double();
        arrowHead.moveTo(startX, startY);
        arrowHead.lineTo(
                startX - arrowLength * Math.cos(angle - Math.toRadians(arrowAngle)),
                startY - arrowLength * Math.sin(angle - Math.toRadians(arrowAngle))
        );
        arrowHead.lineTo(
                startX - arrowLength * Math.cos(angle + Math.toRadians(arrowAngle)),
                startY - arrowLength * Math.sin(angle + Math.toRadians(arrowAngle))
        );
        arrowHead.closePath();
        
        // 填充箭头头部
        g2d.fill(arrowHead);
        g2d.draw(arrowHead);
    }
    


    public void zoomIn() {
        scale *= 1.1;
        repaint();
    }

    public void zoomOut() {
        scale /= 1.1;
        repaint();
    }

    public void resetView() {
        scale = 1.0;
        offsetX = 0;
        offsetY = 0;
        repaint();
    }

    public void setOffset(int x, int y) {
        this.offsetX = x;
        this.offsetY = y;
        repaint();
    }
}
