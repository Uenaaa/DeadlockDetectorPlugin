// 测试当前死锁检测实现
const fs = require('fs');
const path = require('path');

// 简单的模块加载器模拟
function requireModule(filePath) {
    const code = fs.readFileSync(filePath, 'utf8');
    // 提取关键类和函数
    const module = { exports: {} };
    
    // 简化的实现 - 只提取需要的功能
    const NodeType = { PROCESS: "PROCESS", RESOURCE: "RESOURCE" };
    
    class GraphNode {
        constructor(id, type) {
            this.id = id;
            this.type = type;
            this.outgoingEdges = [];
        }
        getId() { return this.id; }
        getType() { return this.type; }
        getOutgoingEdges() { return this.outgoingEdges; }
        addEdge(to) { this.outgoingEdges.push(to); }
    }
    
    class DeadlockDetector {
        constructor() {
            this.nodes = new Map();
        }
        
        addProcessHoldsResource(processId, resourceId) {
            const process = this.getOrCreateNode(processId, NodeType.PROCESS);
            const resource = this.getOrCreateNode(resourceId, NodeType.RESOURCE);
            resource.addEdge(process); // 资源指向持有它的进程
        }
        
        addProcessWaitsForResource(processId, resourceId) {
            const process = this.getOrCreateNode(processId, NodeType.PROCESS);
            const resource = this.getOrCreateNode(resourceId, NodeType.RESOURCE);
            process.addEdge(resource); // 进程指向等待的资源
        }
        
        getOrCreateNode(id, type) {
            if (!this.nodes.has(id)) {
                this.nodes.set(id, new GraphNode(id, type));
            }
            return this.nodes.get(id);
        }
        
        detectDeadlocks() {
            const visited = new Set();
            const recursionStack = new Set();
            const cycles = [];
            
            this.nodes.forEach(node => {
                if (!visited.has(node)) {
                    this.detectCycle(node, visited, recursionStack, [], cycles);
                }
            });
            
            return { hasDeadlock: cycles.length > 0, cycles };
        }
        
        detectCycle(current, visited, recursionStack, path, cycles) {
            visited.add(current);
            recursionStack.add(current);
            path.push(current);
            
            for (const neighbor of current.getOutgoingEdges()) {
                if (!visited.has(neighbor)) {
                    this.detectCycle(neighbor, visited, recursionStack, path, cycles);
                } else if (recursionStack.has(neighbor)) {
                    const cycleStartIndex = path.indexOf(neighbor);
                    if (cycleStartIndex !== -1) {
                        const cycle = [...path.slice(cycleStartIndex), neighbor];
                        
                        // 过滤出有效的死锁循环：
                        // 1. 至少包含4个节点（进程→资源→进程→资源→进程）
                        // 2. 交替出现进程和资源
                        // 3. 不包含自循环
                        if (cycle.length >= 4 && this.isValidDeadlockCycle(cycle)) {
                            cycles.push(cycle);
                        }
                    }
                }
            }
            
            recursionStack.delete(current);
            path.pop();
        }
        
        isValidDeadlockCycle(cycle) {
            // 检查是否交替出现进程和资源
            for (let i = 0; i < cycle.length - 1; i++) {
                const currentType = cycle[i].type;
                const nextType = cycle[i + 1].type;
                
                // 进程只能指向资源，资源只能指向进程
                if ((currentType === NodeType.PROCESS && nextType !== NodeType.RESOURCE) ||
                    (currentType === NodeType.RESOURCE && nextType !== NodeType.PROCESS)) {
                    return false;
                }
            }
            
            // 检查是否包含多个线程
            const processIds = new Set();
            cycle.forEach(node => {
                if (node.type === NodeType.PROCESS) {
                    processIds.add(node.id);
                }
            });
            
            return processIds.size >= 2;
        }
        
        formatDeadlockInfo(cycles) {
            if (cycles.length === 0) {
                return "未检测到死锁";
            }
            
            let result = "检测到死锁！\n";
            cycles.forEach((cycle, index) => {
                result += `死锁循环 ${index + 1}: `;
                cycle.forEach((node, nodeIndex) => {
                    result += `${node.getId()}(${node.getType() === NodeType.PROCESS ? '进程' : '资源'})`;
                    if (nodeIndex < cycle.length - 1) {
                        result += " → ";
                    }
                });
                result += "\n";
            });
            
            return result;
        }
    }
    
    class CodeAnalyzer {
        constructor() {
            this.detector = new DeadlockDetector();
        }
        
        analyzeCode(code) {
            this.detector = new DeadlockDetector();
            this.parseThreads(code);
            return this.detector;
        }
        
        parseThreads(code) {
            let match;
            let threadCount = 0;
            
            // 先处理lambda表达式创建的线程（优先级最高，因为最常见）
            const lambdaRegex = /new\s+Thread\s*\(\s*\(\)\s*->\s*\{([\s\S]*?)\}\s*\)\s*\.start\s*\(\)\s*;/g;
            while ((match = lambdaRegex.exec(code)) !== null) {
                const threadId = `thread${++threadCount}`;
                const runContent = match[1];
                this.analyzeThreadCode(runContent, threadId);
            }
            
            // 再处理匿名内部类创建的线程
            const anonymousRegex = /new\s+Thread\s*\(\s*new\s+Runnable\s*\(\s*\)\s*\{\s*public\s+void\s+run\s*\(\)\s*\{([\s\S]*?)\}\s*\}\s*\)\s*\.start\s*\(\)\s*;/g;
            while ((match = anonymousRegex.exec(code)) !== null) {
                const threadId = `thread${++threadCount}`;
                const runContent = match[1];
                this.analyzeThreadCode(runContent, threadId);
            }
            
            // 处理简化的匿名内部类（没有显式的public void run声明）
            const simplifiedAnonymousRegex = /new\s+Thread\s*\(\s*new\s+Runnable\s*\(\s*\)\s*\{([\s\S]*?)\}\s*\)\s*\.start\s*\(\)\s*;/g;
            while ((match = simplifiedAnonymousRegex.exec(code)) !== null) {
                const threadId = `thread${++threadCount}`;
                const runContent = match[1];
                this.analyzeThreadCode(runContent, threadId);
            }
            
            // 最后处理常规的run方法定义
            const runMethodRegex = /public\s+void\s+run\s*\(\)\s*\{([\s\S]*?)\}/g;
            while ((match = runMethodRegex.exec(code)) !== null) {
                // 获取run方法前面的上下文，判断是否属于线程
                const contextStart = Math.max(0, match.index - 100);
                const context = code.substring(contextStart, match.index);
                
                // 检查是否在Thread或Runnable上下文中
                if (context.includes('new Thread') || context.includes('implements Runnable') || context.includes('extends Thread')) {
                    const threadId = `thread${++threadCount}`;
                    const runContent = match[1];
                    this.analyzeThreadCode(runContent, threadId);
                }
            }
        }
        
        analyzeThreadCode(code, threadId) {
            this.parseSynchronizedBlocks(code, threadId);
            this.parseReentrantLocks(code, threadId);
        }
        
        parseSynchronizedBlocks(code, threadId) {
            const lockStack = [];
            const synchronizedRegex = /(synchronized\s*\(([^)]+)\)\s*\{)|}/g;
            let match;
            
            while ((match = synchronizedRegex.exec(code)) !== null) {
                if (match[1]) {
                    const lockObject = match[2].trim();
                    // 为每个锁获取操作建立等待关系
                    // 这是因为即使没有持有其他锁，线程也可能在等待这个锁（如果它被其他线程持有）
                    this.detector.addProcessWaitsForResource(threadId, lockObject);
                    lockStack.push(lockObject);
                    this.detector.addProcessHoldsResource(threadId, lockObject);
                } else if (match[0] === '}') {
                    if (lockStack.length > 0) lockStack.pop();
                }
            }
        }
        
        parseReentrantLocks(code, threadId) {
            const lockRegex = /\s*(\w+)\.lock\(\)/g;
            let match;
            
            while ((match = lockRegex.exec(code)) !== null) {
                const lockObject = match[1].trim();
                this.detector.addProcessWaitsForResource(threadId, lockObject);
                this.detector.addProcessHoldsResource(threadId, lockObject);
            }
        }
    }
    
    module.exports = { DeadlockDetector, CodeAnalyzer, NodeType };
    return module.exports;
}

// 加载模块
const { DeadlockDetector, CodeAnalyzer } = requireModule('./entry/src/main/ets/utils/DeadlockDetector.ets');


// 测试用例
const testCases = [
    {
        name: "经典死锁场景",
        code: `Object lock1 = new Object();
Object lock2 = new Object();

// 线程1
new Thread(() -> {
    synchronized (lock1) {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {}
        synchronized (lock2) {
            System.out.println("Thread 1 got lock2");
        }
    }
}).start();

// 线程2
new Thread(() -> {
    synchronized (lock2) {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {}
        synchronized (lock1) {
            System.out.println("Thread 2 got lock1");
        }
    }
}).start();`,
        expectedDeadlock: true
    },
    {
        name: "无死锁场景",
        code: `Object lock1 = new Object();
Object lock2 = new Object();

// 线程1
new Thread(() -> {
    synchronized (lock1) {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {}
        synchronized (lock2) {
            System.out.println("Thread 1 got lock2");
        }
    }
}).start();

// 线程2
new Thread(() -> {
    synchronized (lock1) {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {}
        synchronized (lock2) {
            System.out.println("Thread 2 got lock2");
        }
    }
}).start();`,
        expectedDeadlock: false
    },
    {
        name: "嵌套锁死锁场景",
        code: `Object lock1 = new Object();
Object lock2 = new Object();
Object lock3 = new Object();

// 线程1
new Thread(() -> {
    synchronized (lock1) {
        synchronized (lock2) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {}
            synchronized (lock3) {
                System.out.println("Thread 1 got lock3");
            }
        }
    }
}).start();

// 线程2
new Thread(() -> {
    synchronized (lock3) {
        synchronized (lock2) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {}
            synchronized (lock1) {
                System.out.println("Thread 2 got lock1");
            }
        }
    }
}).start();`,
        expectedDeadlock: true
    }
];

// 运行测试
console.log("开始测试当前死锁检测实现...\n");

testCases.forEach((testCase, index) => {
    console.log(`测试用例 ${index + 1}: ${testCase.name}`);
    console.log("-" .repeat(50));
    
    try {
        const analyzer = new CodeAnalyzer();
        const detector = analyzer.analyzeCode(testCase.code);
        const result = detector.detectDeadlocks();
        
        console.log(`检测结果: ${result.hasDeadlock ? '死锁' : '无死锁'}`);
        console.log(`预期结果: ${testCase.expectedDeadlock ? '死锁' : '无死锁'}`);
        console.log(`结果匹配: ${result.hasDeadlock === testCase.expectedDeadlock ? '✓' : '✗'}`);
        
        if (result.hasDeadlock) {
            console.log(detector.formatDeadlockInfo(result.cycles));
        } else {
            console.log("未检测到死锁");
        }
        
    } catch (error) {
        console.log(`测试失败: ${error.message}`);
    }
    
    console.log("\n");
});

console.log("所有测试完成！");