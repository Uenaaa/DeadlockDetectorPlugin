package com.deadlock.detector.visualizer;

import com.deadlock.detector.model.GraphNode;
import com.deadlock.detector.model.NodeType;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeadlockVisualizerPanel extends JPanel {
    private final List<GraphNode> allNodes;
    private final List<List<GraphNode>> cycles;
    private final Map<GraphNode, Point> nodePositions;
    private final int nodeRadius = 30;
    private final int padding = 50;
    private double scale = 1.0;
    private int offsetX = 0;
    private int offsetY = 0;

    public DeadlockVisualizerPanel(List<GraphNode> allNodes, List<List<GraphNode>> cycles) {
        this.allNodes = allNodes;
        this.cycles = cycles;
        this.nodePositions = new HashMap<>();
        this.setBackground(Color.WHITE);
        this.setPreferredSize(new Dimension(800, 600));
        
        // 自动布局节点
        layoutNodes();
    }

    private void layoutNodes() {
        int width = getPreferredSize().width - 2 * padding;
        int height = getPreferredSize().height - 2 * padding;
        
        // 简单的圆形布局
        int totalNodes = allNodes.size();
        if (totalNodes == 0) return;
        
        double angleStep = 2 * Math.PI / totalNodes;
        int centerX = padding + width / 2;
        int centerY = padding + height / 2;
        int radius = Math.min(width, height) / 3;
        
        for (int i = 0; i < allNodes.size(); i++) {
            GraphNode node = allNodes.get(i);
            double angle = i * angleStep;
            int x = centerX + (int) (Math.cos(angle) * radius);
            int y = centerY + (int) (Math.sin(angle) * radius);
            nodePositions.put(node, new Point(x, y));
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        
        // 设置抗锯齿
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // 应用缩放和平移
        g2d.scale(scale, scale);
        g2d.translate(offsetX, offsetY);
        
        // 绘制边
        for (GraphNode node : allNodes) {
            Point nodePos = nodePositions.get(node);
            for (GraphNode neighbor : node.getOutgoingEdges()) {
                Point neighborPos = nodePositions.get(neighbor);
                
                // 检查边是否在死锁循环中
                boolean isInCycle = false;
                for (List<GraphNode> cycle : cycles) {
                    for (int i = 0; i < cycle.size() - 1; i++) {
                        if (cycle.get(i) == node && cycle.get(i + 1) == neighbor) {
                            isInCycle = true;
                            break;
                        }
                    }
                    if (isInCycle) break;
                }
                
                // 设置边的颜色和粗细
                if (isInCycle) {
                    g2d.setColor(Color.RED);
                    g2d.setStroke(new BasicStroke(3.0f));
                } else {
                    g2d.setColor(Color.GRAY);
                    g2d.setStroke(new BasicStroke(1.0f));
                }
                
                // 绘制边
                g2d.draw(new Line2D.Double(nodePos.x, nodePos.y, neighborPos.x, neighborPos.y));
                
                // 绘制箭头
                drawArrow(g2d, nodePos, neighborPos);
            }
        }
        
        // 绘制节点
        for (GraphNode node : allNodes) {
            Point pos = nodePositions.get(node);
            
            // 设置节点颜色
            Color nodeColor;
            if (node.getType() == NodeType.PROCESS) {
                nodeColor = new Color(65, 105, 225); // Royal Blue
            } else {
                nodeColor = new Color(220, 20, 60); // Crimson
            }
            
            // 检查节点是否在死锁循环中
            boolean isInCycle = false;
            for (List<GraphNode> cycle : cycles) {
                if (cycle.contains(node)) {
                    isInCycle = true;
                    break;
                }
            }
            
            // 绘制节点
            if (isInCycle) {
                // 绘制发光效果
                g2d.setColor(new Color(255, 215, 0, 100)); // Gold with transparency
                g2d.fill(new Ellipse2D.Double(pos.x - nodeRadius - 5, pos.y - nodeRadius - 5, 
                        2 * (nodeRadius + 5), 2 * (nodeRadius + 5)));
            }
            
            g2d.setColor(nodeColor);
            g2d.fill(new Ellipse2D.Double(pos.x - nodeRadius, pos.y - nodeRadius, 
                    2 * nodeRadius, 2 * nodeRadius));
            
            // 绘制节点边框
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(2.0f));
            g2d.draw(new Ellipse2D.Double(pos.x - nodeRadius, pos.y - nodeRadius, 
                    2 * nodeRadius, 2 * nodeRadius));
            
            // 绘制节点标签
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            FontMetrics metrics = g2d.getFontMetrics();
            String label = node.getId().length() > 10 ? node.getId().substring(0, 10) + "..." : node.getId();
            int textWidth = metrics.stringWidth(label);
            int textHeight = metrics.getAscent();
            g2d.drawString(label, pos.x - textWidth / 2, pos.y + textHeight / 2);
        }
    }

    private void drawArrow(Graphics2D g2d, Point from, Point to) {
        double angle = Math.atan2(to.y - from.y, to.x - from.x);
        int arrowLength = 15;
        int arrowAngle = 30;
        
        // 箭头起点（在边上，距离节点有一段距离）
        double startX = to.x - nodeRadius * Math.cos(angle);
        double startY = to.y - nodeRadius * Math.sin(angle);
        
        // 箭头主线条
        g2d.draw(new Line2D.Double(from.x, from.y, startX, startY));
        
        // 绘制箭头头部
        double arrow1X = startX - arrowLength * Math.cos(angle - Math.toRadians(arrowAngle));
        double arrow1Y = startY - arrowLength * Math.sin(angle - Math.toRadians(arrowAngle));
        double arrow2X = startX - arrowLength * Math.cos(angle + Math.toRadians(arrowAngle));
        double arrow2Y = startY - arrowLength * Math.sin(angle + Math.toRadians(arrowAngle));
        
        g2d.draw(new Line2D.Double(startX, startY, arrow1X, arrow1Y));
        g2d.draw(new Line2D.Double(startX, startY, arrow2X, arrow2Y));
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
