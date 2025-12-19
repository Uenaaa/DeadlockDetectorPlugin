// 死锁检测器 - ArkTS版本

export enum NodeType {
  PROCESS = "PROCESS",
  RESOURCE = "RESOURCE"
}

export class GraphNode {
  private id: string;
  private type: NodeType;
  private outgoingEdges: GraphNode[];

  constructor(id: string, type: NodeType) {
    this.id = id;
    this.type = type;
    this.outgoingEdges = [];
  }

  getId(): string {
    return this.id;
  }

  getType(): NodeType {
    return this.type;
  }

  getOutgoingEdges(): GraphNode[] {
    return this.outgoingEdges;
  }

  addEdge(to: GraphNode): void {
    this.outgoingEdges.push(to);
  }
}

// 定义接口
export interface DeadlockDetectionResult {
  hasDeadlock: boolean;
  cycles: GraphNode[][];
}

// 锁操作接口
export interface LockOperation {
  type: 'lock' | 'unlock';
  object: string;
  index: number;
}

export interface CombinedLockOperation {
  type: 'synchronized' | 'lock' | 'unlock';
  object: string;
  index: number;
}

export class DeadlockDetector {
  private nodes: Map<string, GraphNode>;

  constructor() {
    this.nodes = new Map<string, GraphNode>();
  }

  // 添加进程持有资源的关系
  addProcessHoldsResource(processId: string, resourceId: string): void {
    const process = this.getOrCreateNode(processId, NodeType.PROCESS);
    const resource = this.getOrCreateNode(resourceId, NodeType.RESOURCE);
    resource.addEdge(process); // 资源指向持有它的进程
  }

  // 添加进程等待资源的关系
  addProcessWaitsForResource(processId: string, resourceId: string): void {
    const process = this.getOrCreateNode(processId, NodeType.PROCESS);
    const resource = this.getOrCreateNode(resourceId, NodeType.RESOURCE);
    process.addEdge(resource); // 进程指向等待的资源
  }

  // 获取或创建节点
  private getOrCreateNode(id: string, type: NodeType): GraphNode {
    if (!this.nodes.has(id)) {
      this.nodes.set(id, new GraphNode(id, type));
    }
    return this.nodes.get(id)!;
  }

  // 检测死锁
  detectDeadlocks(): DeadlockDetectionResult {
    const visited = new Set<GraphNode>();
    const recursionStack = new Set<GraphNode>();
    const cycles: GraphNode[][] = [];

    this.nodes.forEach(node => {
      if (!visited.has(node)) {
        this.detectCycle(node, visited, recursionStack, [], cycles);
      }
    });

    return { hasDeadlock: cycles.length > 0, cycles };
  }

  // 深度优先搜索检测环
  private detectCycle(current: GraphNode, visited: Set<GraphNode>, recursionStack: Set<GraphNode>, 
                     path: GraphNode[], cycles: GraphNode[][]): void {
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
  
  // 验证是否是有效的死锁循环
  private isValidDeadlockCycle(cycle: GraphNode[]): boolean {
    // 检查是否交替出现进程和资源
    for (let i = 0; i < cycle.length - 1; i++) {
      const currentType = cycle[i].getType();
      const nextType = cycle[i + 1].getType();
      
      // 进程只能指向资源，资源只能指向进程
      if ((currentType === NodeType.PROCESS && nextType !== NodeType.RESOURCE) ||
          (currentType === NodeType.RESOURCE && nextType !== NodeType.PROCESS)) {
        return false;
      }
    }
    
    // 检查是否包含多个线程
    const processIds = new Set<string>();
    cycle.forEach(node => {
      if (node.getType() === NodeType.PROCESS) {
        processIds.add(node.getId());
      }
    });
    
    // 死锁至少需要2个线程
    return processIds.size >= 2;
  }

  // 格式化死锁信息
  formatDeadlockInfo(cycles: GraphNode[][]): string {
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

// 代码解析器，用于从用户输入的代码中提取锁信息
export class CodeAnalyzer {
  private detector: DeadlockDetector;

  constructor() {
    this.detector = new DeadlockDetector();
  }

  // 分析代码，提取锁信息
  analyzeCode(code: string): DeadlockDetector {
    // 重置检测器
    this.detector = new DeadlockDetector();

    // 识别线程上下文并分析
    this.parseThreads(code);

    return this.detector;
  }

  // 解析线程上下文（run方法和lambda表达式）
  private parseThreads(code: string): void {
    // 查找所有run方法，这些是线程执行的入口
    // 处理多种线程创建方式：
    // 1. class X implements Runnable
    // 2. new Thread(new Runnable() { run() {...} })
    // 3. new Thread() { run() {...} }
    // 4. new Thread(() -> {...}).start();
    
    let match: RegExpExecArray | null;
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

  // 分析线程代码中的锁操作
  private analyzeThreadCode(code: string, threadId: string): void {
    // 解析synchronized块
    this.parseSynchronizedBlocks(code, threadId);
    // 解析ReentrantLock操作
    this.parseReentrantLocks(code, threadId);
  }

  // 解析synchronized块，处理嵌套结构
  private parseSynchronizedBlocks(code: string, threadId: string): void {
    // 使用栈来跟踪嵌套的synchronized块
    const lockStack: string[] = [];
    
    // 匹配synchronized块的开始和结束
    const synchronizedRegex = /(synchronized\s*\(([^)]+)\)\s*\{)|\}/g;
    let match: RegExpExecArray | null;
    let lastIndex = 0;
    
    while ((match = synchronizedRegex.exec(code)) !== null) {
      if (match[1]) {
        // 找到synchronized块的开始
        const lockObject = match[2].trim();
        
        // 处理嵌套锁：当线程已经持有锁时，新的锁会建立等待关系
        // 为了更准确地模拟真实情况，我们需要考虑当前已持有的锁
        if (lockStack.length > 0) {
          // 当前线程已经持有其他锁，建立等待关系
          this.detector.addProcessWaitsForResource(threadId, lockObject);
        }
        
        // 处理特殊锁情况：null锁、字符串锁
        if (lockObject === 'null') {
          // 处理null锁，将null作为资源名称
          this.detector.addProcessWaitsForResource(threadId, 'null');
        } else if (lockObject.startsWith('"') || lockObject.startsWith('\'')) {
          // 处理字符串锁，保留引号作为标识符的一部分
          this.detector.addProcessWaitsForResource(threadId, lockObject);
        }
        
        // 模拟获取锁
        lockStack.push(lockObject);
        
        // 记录线程持有这个锁
        this.detector.addProcessHoldsResource(threadId, lockObject);
      } else if (match[0] === '}') {
        // 找到块的结束，弹出栈顶的锁
        if (lockStack.length > 0) {
          const releasedLock = lockStack.pop();
          // 在实际实现中，我们可能需要移除持有关系
          // 但为了简单起见，这里只关注锁的获取顺序
        }
      }
      
      lastIndex = match.index + match[0].length;
    }
  }

  // 解析ReentrantLock操作
  private parseReentrantLocks(code: string, threadId: string): void {
    // 使用栈来跟踪ReentrantLock的获取和释放
    const lockStack: string[] = [];
    
    // 匹配lock()和unlock()调用
    const lockRegex = /(\w+)\.(lock|unlock)\(\)/g;
    let match: RegExpExecArray | null;
    
    // 收集所有锁操作并按顺序处理
    const lockOperations: LockOperation[] = [];
    
    while ((match = lockRegex.exec(code)) !== null) {
      const lockObject = match[1].trim();
      const operation = match[2] as 'lock' | 'unlock';
      lockOperations.push({ type: operation, object: lockObject, index: match.index });
    }
    
    // 按顺序处理锁操作
    lockOperations.forEach(op => {
      if (op.type === 'lock') {
        // 当线程持有其他锁时，建立等待关系
        if (lockStack.length > 0) {
          this.detector.addProcessWaitsForResource(threadId, op.object);
        }
        
        // 模拟获取锁
        lockStack.push(op.object);
        
        // 记录线程持有这个锁
        this.detector.addProcessHoldsResource(threadId, op.object);
      } else if (op.type === 'unlock') {
        // 查找并弹出对应的lock
        const lockIndex = lockStack.lastIndexOf(op.object);
        if (lockIndex !== -1) {
          // 移除从lockIndex到栈顶的所有锁（处理重入锁）
          lockStack.splice(lockIndex);
        }
        // 在实际实现中，我们可能需要移除持有关系
        // 但为了简单起见，这里只关注锁的获取顺序
      }
    });
  }
}