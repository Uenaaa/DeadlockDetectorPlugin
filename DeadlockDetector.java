import java.util.*;

/**
 * 死锁检测核心算法实现
 * 使用资源分配图(RAG)和深度优先搜索(DFS)检测循环
 */
public class DeadlockDetector {

    // 表示资源分配图中的节点类型
    public enum NodeType {
        PROCESS,  // 进程节点
        RESOURCE  // 资源节点
    }

    // 资源分配图节点
    public static class GraphNode {
        private String name;
        private NodeType type;
        private List<GraphNode> outgoingEdges;  // 出边列表

        public GraphNode(String name, NodeType type) {
            this.name = name;
            this.type = type;
            this.outgoingEdges = new ArrayList<>();
        }

        public String getName() {
            return name;
        }

        public NodeType getType() {
            return type;
        }

        public List<GraphNode> getOutgoingEdges() {
            return outgoingEdges;
        }

        public void addEdge(GraphNode target) {
            outgoingEdges.add(target);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            GraphNode graphNode = (GraphNode) o;
            return Objects.equals(name, graphNode.name) && type == graphNode.type;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, type);
        }

        @Override
        public String toString() {
            return type + ": " + name;
        }
    }

    // 资源分配图
    private Map<String, GraphNode> nodes;

    public DeadlockDetector() {
        this.nodes = new HashMap<>();
    }

    // 获取或创建节点
    private GraphNode getOrCreateNode(String name, NodeType type) {
        String key = type + ":" + name;
        return nodes.computeIfAbsent(key, k -> new GraphNode(name, type));
    }

    /**
     * 添加进程获取资源的关系
     * @param processName 进程名称
     * @param resourceName 资源名称
     */
    public void addProcessHoldsResource(String processName, String resourceName) {
        GraphNode process = getOrCreateNode(processName, NodeType.PROCESS);
        GraphNode resource = getOrCreateNode(resourceName, NodeType.RESOURCE);
        resource.addEdge(process);  // 资源指向进程（资源被进程持有）
    }

    /**
     * 添加进程等待资源的关系
     * @param processName 进程名称
     * @param resourceName 资源名称
     */
    public void addProcessWaitsForResource(String processName, String resourceName) {
        GraphNode process = getOrCreateNode(processName, NodeType.PROCESS);
        GraphNode resource = getOrCreateNode(resourceName, NodeType.RESOURCE);
        process.addEdge(resource);  // 进程指向资源（进程等待资源）
    }

    /**
     * 检测死锁
     * @return 包含死锁环路的列表，如果没有死锁则返回空列表
     */
    public List<List<GraphNode>> detectDeadlocks() {
        List<List<GraphNode>> deadlockCycles = new ArrayList<>();
        Set<GraphNode> visited = new HashSet<>();

        // 对每个未访问的节点进行DFS
        for (GraphNode node : nodes.values()) {
            if (!visited.contains(node)) {
                List<GraphNode> path = new ArrayList<>();
                detectCycle(node, visited, new HashSet<>(), path, deadlockCycles);
            }
        }

        return deadlockCycles;
    }

    /**
     * 使用DFS检测环
     * @param current 当前节点
     * @param visited 已访问节点集合
     * @param recursionStack 当前递归栈中的节点
     * @param path 当前路径
     * @param cycles 检测到的环列表
     */
    private void detectCycle(GraphNode current, Set<GraphNode> visited, Set<GraphNode> recursionStack, List<GraphNode> path, List<List<GraphNode>> cycles) {
        visited.add(current);
        recursionStack.add(current);
        path.add(current);

        for (GraphNode neighbor : current.getOutgoingEdges()) {
            if (!visited.contains(neighbor)) {
                detectCycle(neighbor, visited, recursionStack, path, cycles);
            } else if (recursionStack.contains(neighbor)) {
                // 找到环，提取环路径
                int cycleStartIndex = path.indexOf(neighbor);
                if (cycleStartIndex != -1) {
                    List<GraphNode> cycle = new ArrayList<>(path.subList(cycleStartIndex, path.size()));
                    cycle.add(neighbor);  // 闭合环
                    cycles.add(cycle);
                }
            }
        }

        // 回溯
        recursionStack.remove(current);
        path.remove(path.size() - 1);
    }

    /**
     * 清除图中的所有节点和边
     */
    public void clear() {
        nodes.clear();
    }

    /**
     * 测试死锁检测算法
     */
    public static void main(String[] args) {
        DeadlockDetector detector = new DeadlockDetector();

        // 创建一个简单的死锁场景：P1持有R1等待R2，P2持有R2等待R1
        detector.addProcessHoldsResource("P1", "R1");
        detector.addProcessWaitsForResource("P1", "R2");
        detector.addProcessHoldsResource("P2", "R2");
        detector.addProcessWaitsForResource("P2", "R1");

        // 检测死锁
        List<List<GraphNode>> deadlocks = detector.detectDeadlocks();

        // 输出结果
        if (deadlocks.isEmpty()) {
            System.out.println("未检测到死锁");
        } else {
            System.out.println("检测到死锁！");
            int cycleCount = 1;
            for (List<GraphNode> cycle : deadlocks) {
                System.out.println("死锁环 " + cycleCount + ":");
                for (int i = 0; i < cycle.size() - 1; i++) {
                    System.out.print(cycle.get(i) + " -> ");
                }
                System.out.println(cycle.get(cycle.size() - 1));
                cycleCount++;
            }
        }
    }
}