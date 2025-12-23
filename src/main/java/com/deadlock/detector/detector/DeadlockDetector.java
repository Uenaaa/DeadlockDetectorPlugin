package com.deadlock.detector.detector;
import com.deadlock.detector.model.GraphNode;
import com.deadlock.detector.model.LockType;
import com.deadlock.detector.model.NodeType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class DeadlockDetector {
    private final Map<String, GraphNode> nodes;

    public DeadlockDetector() {
        this.nodes = new ConcurrentHashMap<>();
    }

    public void addProcessHoldsResource(String processId, String resourceId, LockType lockType) {
        GraphNode process = getOrCreateNode(processId, NodeType.PROCESS, null);
        GraphNode resource = getOrCreateNode(resourceId, NodeType.RESOURCE, lockType);
        resource.addEdge(process);
    }

    public void addProcessHoldsResource(String processId, String resourceId) {
        addProcessHoldsResource(processId, resourceId, null);
    }

    public void addProcessWaitsForResource(String processId, String resourceId, LockType lockType) {
        GraphNode process = getOrCreateNode(processId, NodeType.PROCESS, null);
        GraphNode resource = getOrCreateNode(resourceId, NodeType.RESOURCE, lockType);
        process.addEdge(resource);
    }

    public void addProcessWaitsForResource(String processId, String resourceId) {
        addProcessWaitsForResource(processId, resourceId, null);
    }

    private GraphNode getOrCreateNode(String id, NodeType type, LockType lockType) {
        return nodes.computeIfAbsent(id, k -> new GraphNode(k, type, lockType));
    }

    public DeadlockDetectionResult detectDeadlocks() {
        System.out.println("Starting deadlock detection...");
        System.out.println("Total nodes: " + nodes.size());
        
        // Print all nodes and edges for debugging
        for (GraphNode node : nodes.values()) {
            System.out.println("Node: " + node.getId() + " (" + node.getType() + ")");
            for (GraphNode edge : node.getOutgoingEdges()) {
                System.out.println("  -> " + edge.getId() + " (" + edge.getType() + ")");
            }
        }
        
        Set<GraphNode> visited = new HashSet<>();
        Set<GraphNode> recursionStack = new HashSet<>();
        List<List<GraphNode>> cycles = new ArrayList<>();

        for (GraphNode node : nodes.values()) {
            if (!visited.contains(node)) {
                detectCycle(node, visited, recursionStack, new ArrayList<>(), cycles);
            }
        }

        System.out.println("Detected cycles: " + cycles.size());
        return new DeadlockDetectionResult(!cycles.isEmpty(), cycles);
    }

    private void detectCycle(GraphNode current, Set<GraphNode> visited, Set<GraphNode> recursionStack,
                             List<GraphNode> path, List<List<GraphNode>> cycles) {
        System.out.println("Visiting node: " + current.getId() + " (" + current.getType() + ")");
        visited.add(current);
        recursionStack.add(current);
        path.add(current);

        for (GraphNode neighbor : current.getOutgoingEdges()) {
            System.out.println("  Neighbor: " + neighbor.getId() + " (" + neighbor.getType() + ")");
            if (!visited.contains(neighbor)) {
                detectCycle(neighbor, visited, recursionStack, path, cycles);
            } else if (recursionStack.contains(neighbor)) {
                System.out.println("  Found cycle: " + neighbor.getId() + " is in recursion stack");
                int cycleStartIndex = path.indexOf(neighbor);
                if (cycleStartIndex != -1) {
                    List<GraphNode> cycle = new ArrayList<>(path.subList(cycleStartIndex, path.size()));
                    cycle.add(neighbor);
                    
                    System.out.println("  Cycle found: " + cycleToString(cycle));
                    
                    if (cycle.size() >= 4 && isValidDeadlockCycle(cycle)) {
                        System.out.println("  Valid deadlock cycle, adding to result");
                        cycles.add(cycle);
                    } else {
                        System.out.println("  Invalid deadlock cycle, skipping");
                    }
                }
            }
        }

        recursionStack.remove(current);
        path.remove(path.size() - 1);
    }
    
    private String cycleToString(List<GraphNode> cycle) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cycle.size(); i++) {
            sb.append(cycle.get(i).getId());
            if (i < cycle.size() - 1) {
                sb.append(" -> ");
            }
        }
        return sb.toString();
    }

    private boolean isValidDeadlockCycle(List<GraphNode> cycle) {
        for (int i = 0; i < cycle.size() - 1; i++) {
            NodeType currentType = cycle.get(i).getType();
            NodeType nextType = cycle.get(i + 1).getType();
            if ((currentType == NodeType.PROCESS && nextType != NodeType.RESOURCE) ||
                    (currentType == NodeType.RESOURCE && nextType != NodeType.PROCESS)) {
                return false;
            }
        }
        Set<String> processIds = new HashSet<>();
        for (GraphNode node : cycle) {
            if (node.getType() == NodeType.PROCESS) {
                processIds.add(node.getId());
            }
        }
        return processIds.size() >= 2;
    }

    public String formatDeadlockInfo(List<List<GraphNode>> cycles) {
        if (cycles.isEmpty()) {
            return "未检测到死锁";
        }

        StringBuilder result = new StringBuilder("检测到死锁！\n");
        for (int i = 0; i < cycles.size(); i++) {
            List<GraphNode> cycle = cycles.get(i);
            result.append(String.format("死锁循环 %d: ", i + 1));

            for (int j = 0; j < cycle.size(); j++) {
                GraphNode node = cycle.get(j);
                String nodeDesc = String.format("%s(%s)", node.getId(),
                        node.getType() == NodeType.PROCESS ? "进程" : "资源");
                result.append(nodeDesc);

                if (j < cycle.size() - 1) {
                    result.append(" → ");
                }
            }
            result.append("\n");
        }

        return result.toString();
    }

    public Map<String, GraphNode> getNodes() {
        return nodes;
    }

    public void reset() {
        nodes.clear();
    }
}