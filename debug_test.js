// 调试测试 - 详细打印锁关系
const fs = require('fs');

// 简化的实现
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
        console.log(`  添加关系: ${resourceId}(资源) → ${processId}(进程) [持有]`);
    }
    
    addProcessWaitsForResource(processId, resourceId) {
        const process = this.getOrCreateNode(processId, NodeType.PROCESS);
        const resource = this.getOrCreateNode(resourceId, NodeType.RESOURCE);
        process.addEdge(resource); // 进程指向等待的资源
        console.log(`  添加关系: ${processId}(进程) → ${resourceId}(资源) [等待]`);
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
        for (let i = 0; i < cycle.length - 1; i++) {
            const currentType = cycle[i].getType();
            const nextType = cycle[i + 1].getType();
            
            if ((currentType === NodeType.PROCESS && nextType !== NodeType.RESOURCE) ||
                (currentType === NodeType.RESOURCE && nextType !== NodeType.PROCESS)) {
                return false;
            }
        }
        
        const processIds = new Set();
        cycle.forEach(node => {
            if (node.getType() === NodeType.PROCESS) {
                processIds.add(node.getId());
            }
        });
        
        return processIds.size >= 2;
    }
    
    printGraph() {
        console.log("\n=== 资源分配图 ===");
        this.nodes.forEach(node => {
            const typeStr = node.getType() === NodeType.PROCESS ? "进程" : "资源";
            console.log(`\n${node.getId()} (${typeStr}):`);
            if (node.getOutgoingEdges().length === 0) {
                console.log("  没有出边");
            } else {
                node.getOutgoingEdges().forEach(edge => {
                    const edgeTypeStr = edge.getType() === NodeType.PROCESS ? "进程" : "资源";
                    console.log(`  → ${edge.getId()} (${edgeTypeStr})`);
                });
            }
        });
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
        console.log("=== 解析线程 ===");
        const runMethodRegex = /public\s+void\s+run\s*\(\)\s*\{([\s\S]*?)\}/g;
        let match;
        let threadCount = 0;
        
        // 先处理lambda表达式创建的线程
        console.log("\n1. 处理lambda表达式线程:");
        const lambdaRegex = /new\s+Thread\s*\(\s*\(\)\s*->\s*\{([\s\S]*?)\}\s*\)\s*\.start\s*\(\)\s*;/g;
        while ((match = lambdaRegex.exec(code)) !== null) {
            const threadId = `thread${++threadCount}`;
            const runContent = match[1];
            console.log(`  找到线程 ${threadId}`);
            this.analyzeThreadCode(runContent, threadId);
        }
        
        // 再处理匿名内部类创建的线程
        console.log("\n2. 处理匿名内部类线程:");
        const anonymousRegex = /new\s+Thread\s*\(\s*new\s+Runnable\s*\(\s*\)\s*\{\s*public\s+void\s+run\s*\(\)\s*\{([\s\S]*?)\}\s*\}\s*\)\s*\.start\s*\(\)\s*;/g;
        while ((match = anonymousRegex.exec(code)) !== null) {
            const threadId = `thread${++threadCount}`;
            const runContent = match[1];
            console.log(`  找到线程 ${threadId}`);
            this.analyzeThreadCode(runContent, threadId);
        }
        
        // 处理简化的匿名内部类
        console.log("\n3. 处理简化匿名内部类线程:");
        const simplifiedAnonymousRegex = /new\s+Thread\s*\(\s*new\s+Runnable\s*\(\s*\)\s*\{([\s\S]*?)\}\s*\)\s*\.start\s*\(\)\s*;/g;
        while ((match = simplifiedAnonymousRegex.exec(code)) !== null) {
            const threadId = `thread${++threadCount}`;
            const runContent = match[1];
            console.log(`  找到线程 ${threadId}`);
            this.analyzeThreadCode(runContent, threadId);
        }
        
        // 处理run方法
        console.log("\n4. 处理run方法线程:");
        while ((match = runMethodRegex.exec(code)) !== null) {
            const contextStart = Math.max(0, match.index - 100);
            const context = code.substring(contextStart, match.index);
            
            if (context.includes('new Thread') || context.includes('implements Runnable') || context.includes('extends Thread')) {
                const threadId = `thread${++threadCount}`;
                const runContent = match[1];
                console.log(`  找到线程 ${threadId}`);
                this.analyzeThreadCode(runContent, threadId);
            }
        }
    }
    
    analyzeThreadCode(code, threadId) {
        console.log(`  分析线程 ${threadId} 的代码:`);
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
                console.log(`    synchronized (${lockObject}) {`);
                
                // 为每个锁建立等待关系
                this.detector.addProcessWaitsForResource(threadId, lockObject);
                
                lockStack.push(lockObject);
                this.detector.addProcessHoldsResource(threadId, lockObject);
            } else if (match[0] === '}') {
                if (lockStack.length > 0) {
                    const lock = lockStack.pop();
                    console.log(`    } // 释放 ${lock}`);
                }
            }
        }
    }
    
    parseReentrantLocks(code, threadId) {
        const lockRegex = /\s*(\w+)\.lock\(\)/g;
        let match;
        
        while ((match = lockRegex.exec(code)) !== null) {
            const lockObject = match[1].trim();
            console.log(`    ${lockObject}.lock()`);
            this.detector.addProcessWaitsForResource(threadId, lockObject);
            this.detector.addProcessHoldsResource(threadId, lockObject);
        }
    }
}

// 测试经典死锁场景
const classicDeadlockCode = `Object lock1 = new Object();
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
}).start();`;

console.log("===== 测试经典死锁场景 =====\n");

// 分析代码
const analyzer = new CodeAnalyzer();
const detector = analyzer.analyzeCode(classicDeadlockCode);

// 打印图
console.log("\n=== 最终资源分配图 ===");
detector.printGraph();

// 检测死锁
const result = detector.detectDeadlocks();
console.log("\n=== 死锁检测结果 ===");
console.log(detector.formatDeadlockInfo(result.cycles));