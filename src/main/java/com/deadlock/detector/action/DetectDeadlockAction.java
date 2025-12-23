package com.deadlock.detector.action;

import com.deadlock.detector.analyzer.CodeAnalyzer;
import com.deadlock.detector.detector.DeadlockDetector;
import com.deadlock.detector.detector.DeadlockDetectionResult;
import com.deadlock.detector.model.GraphNode;
import com.deadlock.detector.visualizer.DeadlockVisualizerDialog;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import java.util.ArrayList;

public class DetectDeadlockAction extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        // 1. 获取当前Java文件
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        if (psiFile == null || !(psiFile instanceof PsiJavaFile)) {
            Messages.showInfoMessage("请先打开Java文件！", "死锁检测提示");
            return;
        }

        // 2. Psi API解析+死锁检测
        CodeAnalyzer analyzer = new CodeAnalyzer();
        DeadlockDetector detector = analyzer.analyzePsiFile(psiFile);
        DeadlockDetectionResult result = detector.detectDeadlocks();

        // 3. 展示结果
        if (result.isHasDeadlock()) {
            // 创建所有节点的列表
            ArrayList<GraphNode> allNodes = new ArrayList<>(detector.getNodes().values());
            
            // 显示可视化对话框
            DeadlockVisualizerDialog dialog = new DeadlockVisualizerDialog(allNodes, result.getCycles(), true);
            dialog.show();
        } else {
            Messages.showInfoMessage("未检测到死锁", "死锁检测结果");
        }
    }
}