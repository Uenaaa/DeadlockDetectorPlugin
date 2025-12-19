// 调试无死锁场景
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
                    console.log(`发现循环: ${cycle.map(n => n.id).join(' → ')}`);
                    console.log(`循环长度: ${cycle.length}`);
                    
                    if (cycle.length >= 4 && this.isValidDeadlockCycle(cycle)) {
                        cycles.push(cycle);
                    } else {
                        console.log(`忽略无效循环: ${cycle.map(n => n.id).join(' → ')}`);
                    }
                }
            }
        }
        
        recursionStack.delete(current);
        path.pop();
    }
    
    isValidDeadlockCycle(cycle) {
        console.log(`验证循环: ${cycle.map(n => n.id).join(' → ')}`);
        
        // 检查是否交替出现进程和资源
        for (let i = 0; i < cycle.length - 1; i++) {
            const currentType = cycle[i].type;
            const nextType = cycle[i + 1].type;
            
            console.log(`  ${currentType} → ${nextType}`);
            
            if ((currentType === NodeType.PROCESS && nextType !== NodeType.RESOURCE) ||
                (currentType === NodeType.RESOURCE && nextType !== NodeType.PROCESS)) {
                console.log(`  无效: 类型不交替`);
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
        
        console.log(`  进程数量: ${processIds.size}`);
        
        return processIds.size >= 2;
    }
    
    printGraph() {
        console.log("\n=== 资源分配图 ===");
        this.nodes.forEach(node => {
            const typeStr = node.type === NodeType.PROCESS ? "进程" : "资源";
            console.log(`\n${node.id} (${typeStr}):`);
            if (node.outgoingEdges.length === 0) {
                console.log("  没有出边");
            } else {
                node.outgoingEdges.forEach(edge => {
                    const edgeTypeStr = edge.type === NodeType.PROCESS ? "进程" : "资源";
                    console.log(`  → ${edge.id} (${edgeTypeStr})`);
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
                result += `${node.id}(${node.type === NodeType.PROCESS ? '进程' : '资源'})`;
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
    }
    
    analyzeThreadCode(code, threadId) {
        console.log(`  分析线程 ${threadId} 的代码:`);
        this.parseSynchronizedBlocks(code, threadId);
    }
    
    parseSynchronizedBlocks(code, threadId) {
        const lockStack = [];
        const synchronizedRegex = /(synchronized\s*\(([^)]+)\)\s*\{)|\}/g;
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
}

// 测试无死锁场景
const noDeadlockCode = `Object lock1 = new Object();
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
}).start();`;

console.log("===== 测试无死锁场景 =====\n");

// 分析代码
const analyzer = new CodeAnalyzer();
const detector = analyzer.analyzeCode(noDeadlockCode);

// 打印图
detector.printGraph();

// 检测死锁
const result = detector.detectDeadlocks();
console.log("\n=== 死锁检测结果 ===");
console.log(detector.formatDeadlockInfo(result.cycles));
