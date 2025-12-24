package com.deadlock.detector.analyzer;

import com.deadlock.detector.detector.DeadlockDetector;
import com.deadlock.detector.model.LockType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiMethodImpl;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtNamedFunction;
import org.jetbrains.kotlin.psi.KtObjectLiteralExpression;
import org.jetbrains.kotlin.psi.KtLambdaExpression;
import org.jetbrains.kotlin.psi.KtCallExpression;
import org.jetbrains.kotlin.psi.KtValueArgument;

import org.jetbrains.kotlin.psi.psiUtil.PsiUtilsKt;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于Psi API的Java代码锁解析器
 * 精准提取各类锁操作，构建资源分配图
 */
public class CodeAnalyzer {
    private DeadlockDetector detector;

    public CodeAnalyzer() {
        this.detector = new DeadlockDetector();
    }

    /**
     * 核心入口：解析PsiFile（支持Java和Kotlin），提取锁信息
     * @param psiFile 文件语法树根节点
     * @return 配置好的死锁检测器
     */
    public DeadlockDetector analyzePsiFile(PsiFile psiFile) {
        // 重置检测器
        detector.reset();
        // 重置线程计数器
        threadCounter = 0;

        // 1. 根据文件类型选择对应的分析方法
        if (psiFile instanceof PsiJavaFile) {
            // Java文件分析
            List<PsiMethod> threadRunMethods = extractThreadRunMethods(psiFile);
            for (PsiMethod runMethod : threadRunMethods) {
                String threadId = generateThreadId(runMethod);
                analyzeRunMethodPsi(runMethod, threadId);
            }
        } else if (psiFile instanceof KtFile) {
            // Kotlin文件分析
            analyzeKotlinFile((KtFile) psiFile);
        }

        return detector;
    }

    /**
     * 分析Kotlin文件，提取线程和锁信息
     * @param ktFile Kotlin文件语法树根节点
     */
    private void analyzeKotlinFile(KtFile ktFile) {
        Project project = ktFile.getProject();
        
        // 1. 提取Kotlin线程相关的run函数
        List<PsiElement> kotlinRunFunctions = extractKotlinThreadRunFunctions(ktFile, project);
        
        // 2. 逐个分析run函数中的锁操作
        for (PsiElement runElement : kotlinRunFunctions) {
            String threadId = generateKotlinThreadId(runElement);
            analyzeKotlinRunFunction(runElement, threadId, project);
        }
    }

    /**
     * 提取Kotlin中所有线程相关的run函数（支持Runnable/Thread构造函数/Lambda/对象表达式）
     */
    private List<PsiElement> extractKotlinThreadRunFunctions(KtFile ktFile, Project project) {
        List<PsiElement> runFunctions = new ArrayList<>();
        
        // 1. 提取普通类实现Runnable接口的run函数
        Collection<KtNamedFunction> namedFunctions = PsiTreeUtil.findChildrenOfType(ktFile, KtNamedFunction.class);
        for (KtNamedFunction function : namedFunctions) {
            if ("run".equals(function.getName()) && function.getValueParameters().isEmpty()) {
                // 检查是否实现了Runnable接口
                if (isKotlinRunnableImplementation(function)) {
                    runFunctions.add(function);
                }
            }
        }
        
        // 2. 提取对象表达式中的run函数（object : Runnable { override fun run() {} }）
        Collection<KtObjectLiteralExpression> objectExpressions = PsiTreeUtil.findChildrenOfType(ktFile, KtObjectLiteralExpression.class);
        for (KtObjectLiteralExpression objExpr : objectExpressions) {
            KtNamedFunction runFunction = findRunFunctionInKotlinObject(objExpr);
            if (runFunction != null) {
                runFunctions.add(runFunction);
            }
        }
        
        // 3. 提取Thread构造函数中的Lambda表达式（Thread { ... }）
        Collection<KtCallExpression> callExpressions = PsiTreeUtil.findChildrenOfType(ktFile, KtCallExpression.class);
        for (KtCallExpression callExpr : callExpressions) {
            if (isThreadConstructorCall(callExpr)) {
                // 检查是否有Lambda参数
                for (PsiElement arg : callExpr.getValueArguments()) {
                    if (arg.getFirstChild() instanceof KtLambdaExpression) {
                        runFunctions.add(arg.getFirstChild());
                    }
                }
            }
        }
        
        return runFunctions;
    }
    
    /**
     * 检查Kotlin函数是否实现了Runnable接口
     */
    private boolean isKotlinRunnableImplementation(KtNamedFunction function) {
        // 简单实现：检查函数是否有override关键字
        return function.hasModifier(KtTokens.OVERRIDE_KEYWORD);
    }
    
    /**
     * 在Kotlin对象表达式中查找run函数
     */
    private KtNamedFunction findRunFunctionInKotlinObject(KtObjectLiteralExpression objExpr) {
        return PsiTreeUtil.findChildOfType(objExpr, KtNamedFunction.class, true);
    }
    
    /**
     * 检查是否是Thread构造函数调用
     */
    private boolean isThreadConstructorCall(KtCallExpression callExpr) {
        String calleeName = null;
        PsiElement calleeExpr = callExpr.getCalleeExpression();
        if (calleeExpr != null) {
            calleeName = calleeExpr.getText();
        }
        return "Thread".equals(calleeName);
    }
    
    /**
     * 生成Kotlin线程的唯一ID
     */
    private String generateKotlinThreadId(PsiElement runElement) {
        String elementType = runElement.getClass().getSimpleName();
        threadCounter++;
        return String.format("KotlinThread_%s_%d_%d", elementType, runElement.getTextOffset(), threadCounter);
    }
    
    /**
     * 分析Kotlin run函数中的锁操作
     */
    private void analyzeKotlinRunFunction(PsiElement runElement, String threadId, Project project) {
        // 解析所有函数调用，包括synchronized和lock/unlock
        Collection<KtCallExpression> callExpressions = PsiTreeUtil.findChildrenOfType(runElement, KtCallExpression.class);
        for (KtCallExpression callExpr : callExpressions) {
            String methodName = null;
            PsiElement calleeExpr = callExpr.getCalleeExpression();
            if (calleeExpr != null) {
                methodName = calleeExpr.getText();
            }
            if (methodName == null) continue;
            
            // 1. 处理synchronized函数调用
            if ("synchronized".equals(methodName)) {
                handleKotlinSynchronizedCall(callExpr, threadId);
            }
            // 2. 处理lock/unlock操作
            else if ("lock".equals(methodName) || "unlock".equals(methodName) || "tryLock".equals(methodName)) {
                handleKotlinLockCall(callExpr, methodName, threadId);
            }
        }
    }
    
    /**
     * 处理Kotlin中的synchronized函数调用
     */
    private void handleKotlinSynchronizedCall(KtCallExpression callExpr, String threadId) {
        // 获取synchronized函数的参数
        List<KtValueArgument> arguments = callExpr.getValueArguments();
        if (arguments.size() > 0) {
            // 第一个参数是锁对象
            KtValueArgument lockArg = arguments.get(0);
            PsiElement lockExpr = lockArg.getArgumentExpression();
            if (lockExpr != null) {
                String lockObject = lockExpr.getText().trim();
                LockType lockType = LockType.SYNCHRONIZED;
                
                // 识别类锁
                if (lockObject.endsWith(".class")) {
                    lockType = LockType.CLASS_LOCK;
                    lockObject = "CLASS_" + lockObject;
                }
                
                System.out.println("Found Kotlin synchronized with lock: " + lockObject);
                detector.addProcessWaitsForResource(threadId, lockObject, lockType);
                detector.addProcessHoldsResource(threadId, lockObject, lockType);
            }
        }
    }
    
    /**
     * 处理Kotlin中的锁调用
     */
    private void handleKotlinLockCall(KtCallExpression callExpr, String methodName, String threadId) {
        // 获取锁对象表达式
        PsiElement receiver = null;
        PsiElement calleeExpr = callExpr.getCalleeExpression();
        
        // 简化实现：对于像 lock.lock() 这样的调用，直接获取锁对象名
        String callText = callExpr.getText();
        if (callText.contains(".")) {
            int dotIndex = callText.indexOf(".");
            String lockObject = callText.substring(0, dotIndex).trim();
            LockType lockType = LockType.REENTRANT_LOCK;
            
            System.out.println("Found Kotlin lock call: " + methodName + " on " + lockObject);
            
            if ("lock".equals(methodName) || "tryLock".equals(methodName)) {
                detector.addProcessWaitsForResource(threadId, lockObject, lockType);
                detector.addProcessHoldsResource(threadId, lockObject, lockType);
            }
            return;
        }
        
        // 备用方案：尝试从父节点获取
        if (calleeExpr != null) {
            receiver = calleeExpr.getPrevSibling();
        }
        
        if (receiver != null) {
            String lockObject = receiver.getText().trim();
            LockType lockType = LockType.REENTRANT_LOCK;
            
            System.out.println("Found Kotlin lock call: " + methodName + " on " + lockObject);
            
            if ("lock".equals(methodName) || "tryLock".equals(methodName)) {
                detector.addProcessWaitsForResource(threadId, lockObject, lockType);
                detector.addProcessHoldsResource(threadId, lockObject, lockType);
            }
        }
    }
    
    /**
     * 提取所有线程相关的run()方法（支持Thread/Runnable/匿名类/Lambda）
     */
    private List<PsiMethod> extractThreadRunMethods(PsiFile psiFile) {
        List<PsiMethod> runMethods = new ArrayList<>();
        Project project = psiFile.getProject(); // 获取当前Project对象

        // 1. 提取普通类中的run方法（继承Thread/实现Runnable）
        Collection<PsiClass> psiClassCollection = PsiTreeUtil.findChildrenOfType(psiFile, PsiClass.class);
        PsiClass[] psiClasses = psiClassCollection.toArray(new PsiClass[0]);
        for (PsiClass psiClass : psiClasses) {
            // 获取Thread类的PsiClass对象
            PsiClass threadClass = JavaPsiFacade.getInstance(project)
                    .findClass("java.lang.Thread", GlobalSearchScope.allScope(project));
            boolean isThreadClass = threadClass != null && psiClass.isInheritor(threadClass, true);

            // 获取Runnable类的PsiClass对象
            PsiClass runnableClass = JavaPsiFacade.getInstance(project)
                    .findClass("java.lang.Runnable", GlobalSearchScope.allScope(project));
            boolean isRunnableClass = runnableClass != null && psiClass.isInheritor(runnableClass, true);

            if (!isThreadClass && !isRunnableClass) {
                continue;
            }

            // 查找无参void返回值的run方法
            PsiMethod[] methods = psiClass.findMethodsByName("run", false);
            PsiMethod runMethod = methods.length > 0 ? methods[0] : null;
            if (runMethod != null && runMethod.getParameterList().getParametersCount() == 0
                    && PsiType.VOID.equals(runMethod.getReturnType())) {
                runMethods.add(runMethod);
            }
        }

        // 2. 提取匿名Runnable内部类的run方法
        extractAnonymousRunnableRunMethods(psiFile, runMethods, project);

        // 3. 提取Lambda表达式的线程逻辑（封装为虚拟run方法）
        extractLambdaThreadRunLogic(psiFile, runMethods, project);

        return runMethods;
    }

    /**
     * 提取匿名Runnable内部类的run方法
     */
    private void extractAnonymousRunnableRunMethods(PsiFile psiFile, List<PsiMethod> runMethods, Project project) {
        Collection<PsiAnonymousClass> anonymousClassCollection = PsiTreeUtil.findChildrenOfType(psiFile, PsiAnonymousClass.class);
        PsiAnonymousClass[] anonymousClasses = anonymousClassCollection.toArray(new PsiAnonymousClass[0]);
        for (PsiAnonymousClass anonymousClass : anonymousClasses) {
            PsiClass runnableClass = JavaPsiFacade.getInstance(project)
                    .findClass("java.lang.Runnable", GlobalSearchScope.allScope(project));
            if (runnableClass != null && anonymousClass.isInheritor(runnableClass, true)) {
                PsiMethod[] methods = anonymousClass.findMethodsByName("run", false);
                PsiMethod runMethod = methods.length > 0 ? methods[0] : null;
                if (runMethod != null && runMethod.getParameterList().getParametersCount() == 0
                        && PsiType.VOID.equals(runMethod.getReturnType())) {
                    runMethods.add(runMethod);
                }
            }
        }
    }

    /**
     * 提取Lambda表达式的线程逻辑（封装为虚拟run方法）
     */
    private void extractLambdaThreadRunLogic(PsiFile psiFile, List<PsiMethod> runMethods, Project project) {
        Collection<PsiMethodCallExpression> methodCallCollection = PsiTreeUtil.findChildrenOfType(psiFile, PsiMethodCallExpression.class);
        PsiMethodCallExpression[] methodCalls = methodCallCollection.toArray(new PsiMethodCallExpression[0]);
        
        System.out.println("Extracting lambda thread run logic...");
        
        // 1. 处理直接链式调用的Lambda表达式 (new Thread(() -> {}).start())
        System.out.println("Processing direct chained thread calls...");
        for (PsiMethodCallExpression methodCall : methodCalls) {
            // 匹配Thread的start()方法调用
            if ("start".equals(methodCall.getMethodExpression().getReferenceName())) {
                System.out.println("Found start() method call");
                PsiExpression qualifier = methodCall.getMethodExpression().getQualifierExpression();
                System.out.println("Qualifier: " + (qualifier != null ? qualifier.getText() : "null"));
                if (qualifier instanceof PsiNewExpression) {
                    System.out.println("Qualifier is PsiNewExpression");
                    PsiNewExpression newThreadExpr = (PsiNewExpression) qualifier;
                    PsiExpressionList argList = newThreadExpr.getArgumentList();
                    if (argList != null && argList.getExpressions().length > 0) {
                        System.out.println("NewExpression has arguments");
                        PsiExpression arg = argList.getExpressions()[0];
                        // 处理Lambda表达式参数
                        if (arg instanceof PsiLambdaExpression) {
                            System.out.println("Argument is LambdaExpression");
                            PsiLambdaExpression lambda = (PsiLambdaExpression) arg;
                            PsiCodeBlock lambdaBody = getLambdaCodeBlock(lambda, project);
                            if (lambdaBody != null) {
                                System.out.println("Created lambda body code block");
                                // 创建虚拟run方法，统一解析逻辑
                                PsiMethod virtualRunMethod = createVirtualRunMethod(lambdaBody, project, lambda);
                                System.out.println("Created virtual run method: " + virtualRunMethod.getTextOffset());
                                runMethods.add(virtualRunMethod);
                            }
                        }
                    }
                }
            }
        }
        
        // 2. 处理分开声明和启动的线程 (Thread thread1 = new Thread(() -> {}); thread1.start())
        System.out.println("Processing separate thread declaration and start...");
        Collection<PsiVariable> variableCollection = PsiTreeUtil.findChildrenOfType(psiFile, PsiVariable.class);
        System.out.println("Found " + variableCollection.size() + " variables");
        for (PsiVariable variable : variableCollection) {
            // 检查变量是否是java.lang.Thread类型
            PsiType variableType = variable.getType();
            String typeText = variableType.getPresentableText();
            String canonicalText = variableType.getCanonicalText();
            System.out.println("Variable: " + variable.getName() + ", type: " + typeText + ", canonical: " + canonicalText);
            
            boolean isThreadType = false;
            
            // 检查方式1：直接检查类型文本
            if ("Thread".equals(typeText) || "java.lang.Thread".equals(typeText) || "java.lang.Thread".equals(canonicalText)) {
                isThreadType = true;
                System.out.println("Found Thread variable (using type text): " + variable.getName());
            }
            
            // 检查方式2：通过PsiClass解析（备用）
            if (!isThreadType && variableType instanceof PsiClassType) {
                PsiClassType classType = (PsiClassType) variableType;
                PsiClass psiClass = classType.resolve();
                System.out.println("Resolved class: " + (psiClass != null ? psiClass.getQualifiedName() : "null"));
                if (psiClass != null) {
                    String className = psiClass.getName();
                    String qualifiedName = psiClass.getQualifiedName();
                    isThreadType = "Thread".equals(className) || "java.lang.Thread".equals(qualifiedName);
                    if (isThreadType) {
                        System.out.println("Found Thread variable (using PsiClass): " + variable.getName());
                    }
                }
            }
            
            if (isThreadType) {
                // 检查变量初始化是否包含Lambda表达式或Runnable参数
                PsiExpression initializer = variable.getInitializer();
                System.out.println("Initializer: " + (initializer != null ? initializer.getText() : "null"));
                if (initializer instanceof PsiNewExpression) {
                    System.out.println("Initializer is PsiNewExpression");
                    PsiNewExpression newThreadExpr = (PsiNewExpression) initializer;
                    PsiExpressionList argList = newThreadExpr.getArgumentList();
                    if (argList != null && argList.getExpressions().length > 0) {
                        System.out.println("NewExpression has arguments");
                        PsiExpression arg = argList.getExpressions()[0];
                        // 处理Lambda表达式参数
                        if (arg instanceof PsiLambdaExpression) {
                            System.out.println("Argument is LambdaExpression");
                            PsiLambdaExpression lambda = (PsiLambdaExpression) arg;
                            PsiCodeBlock lambdaBody = getLambdaCodeBlock(lambda, project);
                            if (lambdaBody != null) {
                                System.out.println("Created lambda body code block");
                                // 创建虚拟run方法，统一解析逻辑
                                PsiMethod virtualRunMethod = createVirtualRunMethod(lambdaBody, project, lambda);
                                System.out.println("Created virtual run method: " + virtualRunMethod.getTextOffset());
                                runMethods.add(virtualRunMethod);
                            }
                        }
                        // 处理匿名Runnable参数
                        else if (arg instanceof PsiAnonymousClass) {
                            System.out.println("Argument is AnonymousClass");
                            PsiAnonymousClass anonymousClass = (PsiAnonymousClass) arg;
                            PsiMethod[] methods = anonymousClass.findMethodsByName("run", false);
                            if (methods.length > 0) {
                                System.out.println("Found run method in anonymous class");
                                runMethods.add(methods[0]);
                            }
                        }
                    }
                }
            }
        }
        
        System.out.println("Total run methods extracted: " + runMethods.size());
    }

    /**
     * 将Lambda表达式体转为PsiCodeBlock
     */
    private PsiCodeBlock getLambdaCodeBlock(PsiLambdaExpression lambda, Project project) {
        PsiElement body = lambda.getBody();
        if (body instanceof PsiCodeBlock) {
            return (PsiCodeBlock) body;
        } else {
            // 处理单行Lambda（无大括号）
            PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);
            return factory.createCodeBlockFromText("{" + body.getText() + ";}", lambda);
        }
    }

    /**
     * 创建虚拟run方法（用于统一处理Lambda逻辑）
     */
    private PsiMethod createVirtualRunMethod(PsiCodeBlock body, Project project, PsiLambdaExpression lambda) {
        PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);
        
        // 创建一个完整的类定义来包含run方法，确保语法树完整
        String classText = String.format(
            "class VirtualThreadClass implements Runnable {\n" +
            "    public void run() %s\n" +
            "}", 
            body.getText()
        );
        
        // 创建类并获取其中的run方法
        PsiClass virtualClass = factory.createClassFromText(classText, lambda);
        PsiMethod[] methods = virtualClass.findMethodsByName("run", false);
        
        if (methods.length > 0) {
            System.out.println("Created virtual run method with body: " + methods[0].getBody().getText());
            return methods[0];
        }
        
        // 回退到原来的方法创建方式
        String methodText = String.format("public void run() %s", body.getText());
        PsiMethod fallbackMethod = factory.createMethodFromText(methodText, lambda);
        System.out.println("Fallback: Created virtual run method with body: " + fallbackMethod.getBody().getText());
        return fallbackMethod;
    }

    /**
     * 生成唯一线程ID（基于方法位置、所属类和唯一计数器）
     */
    private int threadCounter = 0;
    
    private String generateThreadId(PsiMethod runMethod) {
        String className = "AnonymousThread";
        if (runMethod.getContainingClass() != null) {
            className = runMethod.getContainingClass().getName() != null
                    ? runMethod.getContainingClass().getName()
                    : "AnonymousClass";
        }
        // 使用全局计数器确保每个线程都有唯一ID
        threadCounter++;
        String threadId = String.format("Thread_%s_%d_%d", className, runMethod.getTextOffset(), threadCounter);
        System.out.println("Generated thread ID: " + threadId);
        return threadId;
    }

    /**
     * 分析单个run方法中的锁操作（Psi API核心解析逻辑）
     */
    private void analyzeRunMethodPsi(PsiMethod runMethod, String threadId) {
        System.out.println("Starting to analyze run method: " + runMethod.getTextOffset());
        System.out.println("Run method body: " + runMethod.getBody().getText());
        
        // 获取方法体中的所有元素
        PsiElement[] children = runMethod.getBody().getChildren();
        System.out.println("Number of children in run method: " + children.length);
        for (PsiElement child : children) {
            System.out.println("Child type: " + child.getClass().getSimpleName() + ", text: " + child.getText().substring(0, Math.min(20, child.getText().length())) + "...");
        }
        
        // 1. 解析Synchronized同步块
        System.out.println("Parsing synchronized blocks...");
        parseSynchronizedBlocksPsi(runMethod, threadId);
        // 2. 解析ReentrantLock/ReadWriteLock
        System.out.println("Parsing lock methods...");
        parseLockMethodsPsi(runMethod, threadId);
        // 3. 解析LockSupport
        System.out.println("Parsing lock support...");
        parseLockSupportPsi(runMethod, threadId);
        
        System.out.println("Finished analyzing run method: " + runMethod.getTextOffset());
    }

    /**
     * 解析Synchronized块（Psi API）
     */
    private void parseSynchronizedBlocksPsi(PsiMethod method, String threadId) {
        List<String> lockStack = new ArrayList<>();
        
        // 使用PsiTreeUtil直接查找所有同步块，确保能找到所有同步语句
        System.out.println("Using PsiTreeUtil to find synchronized statements...");
        Collection<PsiSynchronizedStatement> synchronizedStatementCollection = PsiTreeUtil.findChildrenOfType(method, PsiSynchronizedStatement.class);
        System.out.println("Found " + synchronizedStatementCollection.size() + " synchronized statements");
        
        if (synchronizedStatementCollection.isEmpty()) {
            // 尝试直接在方法体中查找
            System.out.println("No synchronized statements found in method, trying to search in method body...");
            PsiCodeBlock body = method.getBody();
            if (body != null) {
                synchronizedStatementCollection = PsiTreeUtil.findChildrenOfType(body, PsiSynchronizedStatement.class);
                System.out.println("Found " + synchronizedStatementCollection.size() + " synchronized statements in method body");
            }
        }
        
        // 直接处理找到的所有同步块
        for (PsiSynchronizedStatement statement : synchronizedStatementCollection) {
            PsiExpression lockExpr = statement.getLockExpression();
            if (lockExpr == null) {
                System.out.println("Skipping synchronized statement with null lock expression");
                continue;
            }

            // 提取锁对象信息
            String lockObject = lockExpr.getText().trim();
            LockType lockType = LockType.SYNCHRONIZED;

            System.out.println("Found synchronized statement with lock: " + lockObject);

            // 识别类锁
            if (lockObject.endsWith(".class")) {
                lockType = LockType.CLASS_LOCK;
                lockObject = "CLASS_" + lockObject;
            }

            // 锁重入判断
            if (lockStack.contains(lockObject)) {
                lockStack.add(lockObject);
                // 处理内部嵌套同步块
                    PsiCodeBlock block = statement.getBody();
                    if (block != null) {
                        Collection<PsiSynchronizedStatement> nestedStatements = PsiTreeUtil.findChildrenOfType(block, PsiSynchronizedStatement.class);
                        for (PsiSynchronizedStatement nestedStmt : nestedStatements) {
                            processSynchronizedStatement(nestedStmt, threadId, lockStack, lockType);
                        }
                    }
                // 嵌套块结束弹出
                lockStack.remove(lockStack.size() - 1);
                continue;
            }

            // 嵌套锁：建立等待关系
            if (!lockStack.isEmpty()) {
                System.out.println("Adding wait relationship: " + threadId + " -> " + lockObject);
                detector.addProcessWaitsForResource(threadId, lockObject, lockType);
            } else {
                System.out.println("First lock for thread: " + threadId + " -> " + lockObject);
            }

            // 特殊锁对象处理
            if ("null".equals(lockObject)) {
                System.out.println("Adding wait for null lock: " + threadId + " -> null");
                detector.addProcessWaitsForResource(threadId, "null", lockType);
            } else if (lockObject.startsWith("\"") || lockObject.startsWith("'")) {
                System.out.println("Adding wait for string lock: " + threadId + " -> " + lockObject);
                detector.addProcessWaitsForResource(threadId, lockObject, lockType);
            }

            // 记录锁持有关系
            lockStack.add(lockObject);
            System.out.println("Adding hold relationship: " + threadId + " -> " + lockObject);
            detector.addProcessHoldsResource(threadId, lockObject, lockType);

            // 处理内部嵌套同步块
            PsiCodeBlock block = statement.getBody();
            if (block != null) {
                Collection<PsiSynchronizedStatement> nestedStatements = PsiTreeUtil.findChildrenOfType(block, PsiSynchronizedStatement.class);
                for (PsiSynchronizedStatement nestedStmt : nestedStatements) {
                    processSynchronizedStatement(nestedStmt, threadId, lockStack, lockType);
                }
            }

            // 同步块结束：弹出锁栈
            lockStack.remove(lockStack.size() - 1);
        }
    }
    
    /**
     * 处理单个同步块（递归处理嵌套同步块）
     */
    private void processSynchronizedStatement(PsiSynchronizedStatement statement, String threadId, 
                                             List<String> lockStack, LockType parentLockType) {
        PsiExpression lockExpr = statement.getLockExpression();
        if (lockExpr == null) {
            return;
        }
        
        String lockObject = lockExpr.getText().trim();
        LockType lockType = LockType.SYNCHRONIZED;
        
        System.out.println("Processing nested synchronized statement with lock: " + lockObject);
        
        // 识别类锁
        if (lockObject.endsWith(".class")) {
            lockType = LockType.CLASS_LOCK;
            lockObject = "CLASS_" + lockObject;
        }
        
        // 嵌套锁：建立等待关系
        if (!lockStack.isEmpty()) {
            System.out.println("Adding nested wait relationship: " + threadId + " -> " + lockObject);
            detector.addProcessWaitsForResource(threadId, lockObject, lockType);
        }
        
        // 锁重入判断
        if (lockStack.contains(lockObject)) {
            lockStack.add(lockObject);
            // 处理更深层次的嵌套同步块
                PsiCodeBlock block = statement.getBody();
                if (block != null) {
                    Collection<PsiSynchronizedStatement> nestedStatements = PsiTreeUtil.findChildrenOfType(block, PsiSynchronizedStatement.class);
                    for (PsiSynchronizedStatement nestedStmt : nestedStatements) {
                        processSynchronizedStatement(nestedStmt, threadId, lockStack, lockType);
                    }
                }
            // 嵌套块结束弹出
            lockStack.remove(lockStack.size() - 1);
            return;
        }
        
        // 记录锁持有关系
        lockStack.add(lockObject);
        System.out.println("Adding nested hold relationship: " + threadId + " -> " + lockObject);
        detector.addProcessHoldsResource(threadId, lockObject, lockType);
        
        // 处理更深层次的嵌套同步块
        PsiCodeBlock block = statement.getBody();
        if (block != null) {
            Collection<PsiSynchronizedStatement> nestedStatements = PsiTreeUtil.findChildrenOfType(block, PsiSynchronizedStatement.class);
            for (PsiSynchronizedStatement nestedStmt : nestedStatements) {
                processSynchronizedStatement(nestedStmt, threadId, lockStack, lockType);
            }
        }
        
        // 同步块结束：弹出锁栈
        lockStack.remove(lockStack.size() - 1);
    }

    /**
     * 解析ReentrantLock/ReadWriteLock操作（Psi API）
     */
    private void parseLockMethodsPsi(PsiMethod method, String threadId) {
        List<String> lockStack = new ArrayList<>();

        method.accept(new PsiRecursiveElementVisitor() {

            public void visitMethodCallExpression(PsiMethodCallExpression callExpr) {
                String methodName = callExpr.getMethodExpression().getReferenceName();
                if (methodName == null) {
                    return;
                }

                // 1. 处理ReadLock/WriteLock（xxx.readLock().lock()）
                if (("readLock".equals(methodName) || "writeLock".equals(methodName))
                        && callExpr.getArgumentList().getExpressions().length == 0) {
                    handleReadWriteLock(callExpr, methodName, threadId, lockStack);
                }

                // 2. 处理ReentrantLock的lock()/unlock()/tryLock()
                if ("lock".equals(methodName) && callExpr.getArgumentList().getExpressions().length == 0) {
                    handleReentrantLockAcquire(callExpr, threadId, lockStack);
                } else if ("unlock".equals(methodName) && callExpr.getArgumentList().getExpressions().length == 0) {
                    handleReentrantLockRelease(callExpr, lockStack);
                } else if ("tryLock".equals(methodName)) {
                    handleTryLock(callExpr, threadId, lockStack);
                }
            }
        });
    }

    /**
     * 处理ReadWriteLock的获取与释放
     */
    private void handleReadWriteLock(PsiMethodCallExpression callExpr, String rwMethodName,
                                     String threadId, List<String> lockStack) {
        PsiExpression qualifier = callExpr.getMethodExpression().getQualifierExpression();
        if (qualifier == null) {
            return;
        }

        String lockObjName = qualifier.getText().trim();
        LockType lockType = "readLock".equals(rwMethodName) ? LockType.READ_LOCK : LockType.WRITE_LOCK;
        String lockKey = lockObjName + "_" + rwMethodName;

        // 查找后续的lock()/unlock()调用
        PsiElement parent = callExpr.getParent();
        if (parent instanceof PsiMethodCallExpression) {
            PsiMethodCallExpression parentCall = (PsiMethodCallExpression) parent;
            String parentMethodName = parentCall.getMethodExpression().getReferenceName();
            if ("lock".equals(parentMethodName)) {
                // 处理锁获取
                if (lockStack.contains(lockKey)) {
                    lockStack.add(lockKey);
                    return;
                }
                if (!lockStack.isEmpty()) {
                    detector.addProcessWaitsForResource(threadId, lockKey, lockType);
                }
                lockStack.add(lockKey);
                detector.addProcessHoldsResource(threadId, lockKey, lockType);
            } else if ("unlock".equals(parentMethodName)) {
                // 处理锁释放
                int lastIndex = lockStack.lastIndexOf(lockKey);
                if (lastIndex != -1) {
                    lockStack.subList(lastIndex, lockStack.size()).clear();
                }
            }
        }
    }

    /**
     * 处理ReentrantLock的lock()获取
     */
    private void handleReentrantLockAcquire(PsiMethodCallExpression callExpr,
                                            String threadId, List<String> lockStack) {
        PsiExpression qualifier = callExpr.getMethodExpression().getQualifierExpression();
        if (qualifier == null) {
            return;
        }

        String lockObjName = qualifier.getText().trim();
        LockType lockType = LockType.REENTRANT_LOCK;

        if (lockStack.contains(lockObjName)) {
            lockStack.add(lockObjName);
            return;
        }

        if (!lockStack.isEmpty()) {
            detector.addProcessWaitsForResource(threadId, lockObjName, lockType);
        }

        lockStack.add(lockObjName);
        detector.addProcessHoldsResource(threadId, lockObjName, lockType);
    }

    /**
     * 处理ReentrantLock的unlock()释放
     */
    private void handleReentrantLockRelease(PsiMethodCallExpression callExpr, List<String> lockStack) {
        PsiExpression qualifier = callExpr.getMethodExpression().getQualifierExpression();
        if (qualifier == null) {
            return;
        }

        String lockObjName = qualifier.getText().trim();
        int lastIndex = lockStack.lastIndexOf(lockObjName);
        if (lastIndex != -1) {
            lockStack.subList(lastIndex, lockStack.size()).clear();
        }
    }

    /**
     * 处理ReentrantLock的tryLock()
     */
    private void handleTryLock(PsiMethodCallExpression callExpr,
                               String threadId, List<String> lockStack) {
        PsiExpression qualifier = callExpr.getMethodExpression().getQualifierExpression();
        if (qualifier == null) {
            return;
        }

        // 带超时参数的tryLock视为锁获取
        if (callExpr.getArgumentList().getExpressions().length > 0) {
            String lockObjName = qualifier.getText().trim();
            LockType lockType = LockType.REENTRANT_LOCK;

            if (lockStack.contains(lockObjName)) {
                lockStack.add(lockObjName);
                return;
            }

            if (!lockStack.isEmpty()) {
                detector.addProcessWaitsForResource(threadId, lockObjName, lockType);
            }

            lockStack.add(lockObjName);
            detector.addProcessHoldsResource(threadId, lockObjName, lockType);
        }
    }

    /**
     * 解析LockSupport操作（Psi API）
     */
    private void parseLockSupportPsi(PsiMethod method, String threadId) {
        method.accept(new PsiRecursiveElementVisitor() {

            public void visitMethodCallExpression(PsiMethodCallExpression callExpr) {
                PsiReferenceExpression refExpr = callExpr.getMethodExpression();
                PsiElement qualifier = refExpr.getQualifier();

                // 匹配LockSupport类的方法调用
                if (qualifier instanceof PsiReferenceExpression) {
                    PsiReferenceExpression qualRef = (PsiReferenceExpression) qualifier;
                    if ("LockSupport".equals(qualRef.getReferenceName())) {
                        String methodName = refExpr.getReferenceName();
                        if (methodName == null) {
                            return;
                        }

                        // 处理park()
                        if ("park".equals(methodName) && callExpr.getArgumentList().getExpressions().length == 0) {
                            String resourceKey = "LockSupport_" + threadId;
                            detector.addProcessWaitsForResource(threadId, resourceKey, LockType.LOCK_SUPPORT);
                        }

                        // 处理unpark(thread)
                        if ("unpark".equals(methodName) && callExpr.getArgumentList().getExpressions().length == 1) {
                            PsiExpression targetThreadExpr = callExpr.getArgumentList().getExpressions()[0];
                            String targetThreadName = targetThreadExpr.getText().trim();
                            String targetThreadId = "Thread_" + targetThreadName;
                            String resourceKey = "LockSupport_" + targetThreadId;
                            // 此处可扩展detector添加移除等待关系的方法
                            // detector.removeProcessWaitsForResource(targetThreadId, resourceKey);
                        }
                    }
                }
            }
        });
    }

    /**
     * 保留原有字符串解析方法（兼容测试）
     */
    public DeadlockDetector analyzeCode(String code) {
        detector.reset();
        // 原有字符串正则解析逻辑（可保留或删除）
        return detector;
    }
}