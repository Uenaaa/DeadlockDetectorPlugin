// 简化的死锁检测核心功能测试
// 模拟DeadlockDetector.ets的核心逻辑

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

// 测试死锁检测功能
console.log("=== 死锁检测算法测试 ===");

// 测试用例1：经典死锁场景
console.log("\n1. 经典死锁场景测试：");
const detector1 = new DeadlockDetector();
detector1.addProcessWaitsForResource("thread1", "lock1");
detector1.addProcessHoldsResource("thread1", "lock1");
detector1.addProcessWaitsForResource("thread1", "lock2");
detector1.addProcessHoldsResource("thread1", "lock2");

detector1.addProcessWaitsForResource("thread2", "lock2");
detector1.addProcessHoldsResource("thread2", "lock2");
detector1.addProcessWaitsForResource("thread2", "lock1");
detector1.addProcessHoldsResource("thread2", "lock1");

const result1 = detector1.detectDeadlocks();
console.log(detector1.formatDeadlockInfo(result1.cycles));

// 测试用例2：嵌套锁场景
console.log("\n2. 嵌套锁场景测试：");
const detector2 = new DeadlockDetector();
detector2.addProcessWaitsForResource("thread1", "lock1");
detector2.addProcessHoldsResource("thread1", "lock1");
detector2.addProcessWaitsForResource("thread1", "lock2");
detector2.addProcessHoldsResource("thread1", "lock2");
detector2.addProcessWaitsForResource("thread1", "lock3");
detector2.addProcessHoldsResource("thread1", "lock3");

detector2.addProcessWaitsForResource("thread2", "lock3");
detector2.addProcessHoldsResource("thread2", "lock3");
detector2.addProcessWaitsForResource("thread2", "lock2");
detector2.addProcessHoldsResource("thread2", "lock2");
detector2.addProcessWaitsForResource("thread2", "lock1");
detector2.addProcessHoldsResource("thread2", "lock1");

const result2 = detector2.detectDeadlocks();
console.log(detector2.formatDeadlockInfo(result2.cycles));

// 测试用例3：字符串锁场景
console.log("\n3. 字符串锁场景测试：");
const detector3 = new DeadlockDetector();
detector3.addProcessWaitsForResource("thread1", "\"lockA\"");
detector3.addProcessHoldsResource("thread1", "\"lockA\"");
detector3.addProcessWaitsForResource("thread1", "\"lockB\"");
detector3.addProcessHoldsResource("thread1", "\"lockB\"");

detector3.addProcessWaitsForResource("thread2", "\"lockB\"");
detector3.addProcessHoldsResource("thread2", "\"lockB\"");
detector3.addProcessWaitsForResource("thread2", "\"lockA\"");
detector3.addProcessHoldsResource("thread2", "\"lockA\"");

const result3 = detector3.detectDeadlocks();
console.log(detector3.formatDeadlockInfo(result3.cycles));

// 测试用例4：无死锁场景
console.log("\n4. 无死锁场景测试：");
const detector4 = new DeadlockDetector();
detector4.addProcessWaitsForResource("thread1", "lock1");
detector4.addProcessHoldsResource("thread1", "lock1");
detector4.addProcessWaitsForResource("thread1", "lock2");
detector4.addProcessHoldsResource("thread1", "lock2");

detector4.addProcessWaitsForResource("thread2", "lock1");
detector4.addProcessHoldsResource("thread2", "lock1");

const result4 = detector4.detectDeadlocks();
console.log(detector4.formatDeadlockInfo(result4.cycles));

console.log("\n=== 测试完成 ===");
