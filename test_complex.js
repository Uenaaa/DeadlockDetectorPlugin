// 复杂场景测试：验证更新后的解析器
// 模拟更新后的DeadlockDetector.ets逻辑

// 节点类型枚举
const NodeType = {
  PROCESS: "PROCESS",
  RESOURCE: "RESOURCE"
};

// 图节点类
class GraphNode {
  constructor(id, type) {
    this.id = id;
    this.type = type;
    this.outgoingEdges = [];
  }

  getId() {
    return this.id;
  }

  getType() {
    return this.type;
  }

  getOutgoingEdges() {
    return this.outgoingEdges;
  }

  addEdge(to) {
    this.outgoingEdges.push(to);
  }
}

// 死锁检测器类
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

// 简化的代码解析器类
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
    
    // 处理lambda表达式创建的线程
    const lambdaRegex = /new\s+Thread\s*\(\s*\(\)\s*->\s*\{([\s\S]*?)\}\s*\)\s*\.start\s*\(\)\s*;/g;
    while ((match = lambdaRegex.exec(code)) !== null) {
      const threadId = `thread${++threadCount}`;
      const runContent = match[1];
      this.analyzeThreadCode(runContent, threadId);
    }
    
    // 处理匿名内部类创建的线程
    const anonymousRegex = /new\s+Thread\s*\(\s*new\s+Runnable\s*\(\s*\)\s*\{\s*public\s+void\s+run\s*\(\)\s*\{([\s\S]*?)\}\s*\}\s*\)\s*\.start\s*\(\)\s*;/g;
    while ((match = anonymousRegex.exec(code)) !== null) {
      const threadId = `thread${++threadCount}`;
      const runContent = match[1];
      this.analyzeThreadCode(runContent, threadId);
    }
    
    // 处理简化的匿名内部类
    const simplifiedAnonymousRegex = /new\s+Thread\s*\(\s*new\s+Runnable\s*\(\s*\)\s*\{([\s\S]*?)\}\s*\)\s*\.start\s*\(\)\s*;/g;
    while ((match = simplifiedAnonymousRegex.exec(code)) !== null) {
      const threadId = `thread${++threadCount}`;
      const runContent = match[1];
      this.analyzeThreadCode(runContent, threadId);
    }
    
    // 处理常规的run方法定义
    const runMethodRegex = /public\s+void\s+run\s*\(\)\s*\{([\s\S]*?)\}/g;
    while ((match = runMethodRegex.exec(code)) !== null) {
      const contextStart = Math.max(0, match.index - 100);
      const context = code.substring(contextStart, match.index);
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
    const synchronizedRegex = /(synchronized\s*\(([^)]+)\)\s*\{)|\}/g;
    let match;
    
    while ((match = synchronizedRegex.exec(code)) !== null) {
      if (match[1]) {
        const lockObject = match[2].trim();
        if (lockStack.length > 0) {
          this.detector.addProcessWaitsForResource(threadId, lockObject);
        }
        
        if (lockObject === 'null') {
          this.detector.addProcessWaitsForResource(threadId, 'null');
        } else if (lockObject.startsWith('"') || lockObject.startsWith("'")) {
          this.detector.addProcessWaitsForResource(threadId, lockObject);
        }
        
        lockStack.push(lockObject);
        this.detector.addProcessHoldsResource(threadId, lockObject);
      } else if (match[0] === '}') {
        if (lockStack.length > 0) {
          lockStack.pop();
        }
      }
    }
  }

  parseReentrantLocks(code, threadId) {
    const lockStack = [];
    const lockRegex = /(\w+)\.(lock|unlock)\(\)/g;
    const lockOperations = [];
    let match;
    
    while ((match = lockRegex.exec(code)) !== null) {
      const lockObject = match[1].trim();
      const operation = match[2];
      lockOperations.push({ type: operation, object: lockObject, index: match.index });
    }
    
    lockOperations.forEach(op => {
      if (op.type === 'lock') {
        if (lockStack.length > 0) {
          this.detector.addProcessWaitsForResource(threadId, op.object);
        }
        lockStack.push(op.object);
        this.detector.addProcessHoldsResource(threadId, op.object);
      } else if (op.type === 'unlock') {
        const lockIndex = lockStack.lastIndexOf(op.object);
        if (lockIndex !== -1) {
          lockStack.splice(lockIndex);
        }
      }
    });
  }
}

// 测试代码
console.log("=== 复杂锁场景测试 ===");

// 测试用例1：null锁场景
console.log("\n1. Null锁场景测试：");
const testNullLock = `
class NullLockExample {
    public static void main(String[] args) {
        final Object lock = null;
        
        new Thread(() -> {
            synchronized (lock) {
                System.out.println("Thread 1 holds null lock");
            }
        }).start();
        
        new Thread(() -> {
            synchronized (lock) {
                System.out.println("Thread 2 holds null lock");
            }
        }).start();
    }
}`;

const analyzer1 = new CodeAnalyzer();
const detector1 = analyzer1.analyzeCode(testNullLock);
const result1 = detector1.detectDeadlocks();
console.log(detector1.formatDeadlockInfo(result1.cycles));

// 测试用例2：字符串锁场景
console.log("\n2. 字符串锁场景测试：");
const testStringLock = `
class StringLockExample {
    public static void main(String[] args) {
        new Thread(() -> {
            synchronized ("lockA") {
                System.out.println("Thread 1 holds 'lockA'");
                synchronized ("lockB") {
                    System.out.println("Thread 1 holds 'lockB'");
                }
            }
        }).start();
        
        new Thread(() -> {
            synchronized ("lockB") {
                System.out.println("Thread 2 holds 'lockB'");
                synchronized ("lockA") {
                    System.out.println("Thread 2 holds 'lockA'");
                }
            }
        }).start();
    }
}`;

const analyzer2 = new CodeAnalyzer();
const detector2 = analyzer2.analyzeCode(testStringLock);
const result2 = detector2.detectDeadlocks();
console.log(detector2.formatDeadlockInfo(result2.cycles));

// 测试用例3：混合锁类型（synchronized + ReentrantLock）
console.log("\n3. 混合锁类型场景测试：");
const testMixedLock = `
import java.util.concurrent.locks.ReentrantLock;

class MixedLockExample {
    private static final Object syncLock = new Object();
    private static final ReentrantLock reentrantLock = new ReentrantLock();
    
    public static void main(String[] args) {
        new Thread(() -> {
            synchronized (syncLock) {
                System.out.println("Thread 1 holds syncLock");
                reentrantLock.lock();
                try {
                    System.out.println("Thread 1 holds reentrantLock");
                } finally {
                    reentrantLock.unlock();
                }
            }
        }).start();
        
        new Thread(() -> {
            reentrantLock.lock();
            try {
                System.out.println("Thread 2 holds reentrantLock");
                synchronized (syncLock) {
                    System.out.println("Thread 2 holds syncLock");
                }
            } finally {
                reentrantLock.unlock();
            }
        }).start();
    }
}`;

const analyzer3 = new CodeAnalyzer();
const detector3 = analyzer3.analyzeCode(testMixedLock);
const result3 = detector3.detectDeadlocks();
console.log(detector3.formatDeadlockInfo(result3.cycles));

// 测试用例4：复杂嵌套锁场景
console.log("\n4. 复杂嵌套锁场景测试：");
const testNestedLock = `
class NestedLockExample {
    private static final Object lock1 = new Object();
    private static final Object lock2 = new Object();
    private static final Object lock3 = new Object();
    
    public static void main(String[] args) {
        new Thread(() -> {
            synchronized (lock1) {
                System.out.println("Thread 1 holds lock1");
                synchronized (lock2) {
                    System.out.println("Thread 1 holds lock2");
                    synchronized (lock3) {
                        System.out.println("Thread 1 holds lock3");
                    }
                }
            }
        }).start();
        
        new Thread(() -> {
            synchronized (lock3) {
                System.out.println("Thread 2 holds lock3");
                synchronized (lock2) {
                    System.out.println("Thread 2 holds lock2");
                    synchronized (lock1) {
                        System.out.println("Thread 2 holds lock1");
                    }
                }
            }
        }).start();
    }
}`;

const analyzer4 = new CodeAnalyzer();
const detector4 = analyzer4.analyzeCode(testNestedLock);
const result4 = detector4.detectDeadlocks();
console.log(detector4.formatDeadlockInfo(result4.cycles));

console.log("\n=== 测试完成 ===");
