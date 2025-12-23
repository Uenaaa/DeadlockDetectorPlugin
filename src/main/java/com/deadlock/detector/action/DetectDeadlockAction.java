package com.deadlock.detector.action;

import com.deadlock.detector.analyzer.CodeAnalyzer;
import com.deadlock.detector.detector.DeadlockDetector;
import com.deadlock.detector.detector.DeadlockDetectionResult;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;

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
            String deadlockInfo = detector.formatDeadlockInfo(result.getCycles());
            Messages.showWarningDialog(
                    "死锁检测结果", // 标题
                    deadlockInfo   // 内容
            );
        } else {
            Messages.showInfoMessage("未检测到死锁", "死锁检测结果");
        }
    }
}