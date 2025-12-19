import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 死锁测试用例类，包含各种复杂锁场景
 */
public class DeadlockTestCases {

    // 测试场景1：基本死锁
    static class BasicDeadlockTest {
        private final Object lock1 = new Object();
        private final Object lock2 = new Object();

        public void method1() {
            synchronized (lock1) {
                System.out.println("Thread 1: Holding lock 1");
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("Thread 1: Waiting for lock 2");
                synchronized (lock2) {
                    System.out.println("Thread 1: Holding lock 1 and lock 2");
                }
            }
        }

        public void method2() {
            synchronized (lock2) {
                System.out.println("Thread 2: Holding lock 2");
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("Thread 2: Waiting for lock 1");
                synchronized (lock1) {
                    System.out.println("Thread 2: Holding lock 2 and lock 1");
                }
            }
        }
    }

    // 测试场景2：字符串锁死锁
    static class StringLockDeadlockTest {
        // 注意：使用字符串字面量可能导致死锁，因为字符串常量池
        private final String lockA = "lockA";
        private final String lockB = "lockB";

        public void processA() {
            synchronized (lockA) {
                System.out.println("Process A: Holding lockA");
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("Process A: Waiting for lockB");
                synchronized (lockB) {
                    System.out.println("Process A: Holding lockA and lockB");
                }
            }
        }

        public void processB() {
            synchronized (lockB) {
                System.out.println("Process B: Holding lockB");
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("Process B: Waiting for lockA");
                synchronized (lockA) {
                    System.out.println("Process B: Holding lockB and lockA");
                }
            }
        }
    }

    // 测试场景3：嵌套锁死锁
    static class NestedLockDeadlockTest {
        private final Object outerLock = new Object();
        private final Object innerLock = new Object();
        private final Object sharedLock = new Object();

        public void outerMethod1() {
            synchronized (outerLock) {
                System.out.println("Thread 1: Holding outerLock");
                innerMethod1();
            }
        }

        public void innerMethod1() {
            synchronized (innerLock) {
                System.out.println("Thread 1: Holding innerLock");
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("Thread 1: Waiting for sharedLock");
                synchronized (sharedLock) {
                    System.out.println("Thread 1: Holding all locks");
                }
            }
        }

        public void outerMethod2() {
            synchronized (sharedLock) {
                System.out.println("Thread 2: Holding sharedLock");
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("Thread 2: Waiting for outerLock");
                synchronized (outerLock) {
                    System.out.println("Thread 2: Holding sharedLock and outerLock");
                }
            }
        }
    }

    // 测试场景4：null锁问题
    static class NullLockTest {
        private Object lock = null;

        public void methodWithNullLock() {
            // 危险！可能导致空指针异常
            synchronized (lock) {
                System.out.println("This will cause NullPointerException");
            }
        }

        public void methodWithLockAssignment() {
            lock = new Object();
            synchronized (lock) {
                System.out.println("Lock is now initialized");
            }
        }
    }

    // 测试场景5：ReentrantLock死锁
    static class ReentrantLockDeadlockTest {
        private final ReentrantLock lock1 = new ReentrantLock();
        private final ReentrantLock lock2 = new ReentrantLock();

        public void method1() {
            lock1.lock();
            try {
                System.out.println("Thread 1: Holding lock 1");
                Thread.sleep(100);
                System.out.println("Thread 1: Waiting for lock 2");
                lock2.lock();
                try {
                    System.out.println("Thread 1: Holding lock 1 and lock 2");
                } finally {
                    lock2.unlock();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                lock1.unlock();
            }
        }

        public void method2() {
            lock2.lock();
            try {
                System.out.println("Thread 2: Holding lock 2");
                Thread.sleep(100);
                System.out.println("Thread 2: Waiting for lock 1");
                lock1.lock();
                try {
                    System.out.println("Thread 2: Holding lock 2 and lock 1");
                } finally {
                    lock1.unlock();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                lock2.unlock();
            }
        }
    }

    // 测试场景6：无死锁场景
    static class NoDeadlockTest {
        private final Object lock1 = new Object();
        private final Object lock2 = new Object();

        // 按相同顺序获取锁，避免死锁
        public void method1() {
            synchronized (lock1) {
                System.out.println("Thread 1: Holding lock 1");
                synchronized (lock2) {
                    System.out.println("Thread 1: Holding lock 1 and lock 2");
                }
            }
        }

        public void method2() {
            synchronized (lock1) { // 与method1相同的锁顺序
                System.out.println("Thread 2: Holding lock 1");
                synchronized (lock2) {
                    System.out.println("Thread 2: Holding lock 1 and lock 2");
                }
            }
        }
    }

    /**
     * 运行死锁检测算法测试
     */
    public static void main(String[] args) {
        System.out.println("=== 死锁检测算法测试 ===");

        // 测试基本死锁场景
        System.out.println("\n1. 测试基本死锁场景：");
        testDeadlockDetection("BasicDeadlock");

        // 测试字符串锁死锁场景
        System.out.println("\n2. 测试字符串锁死锁场景：");
        testDeadlockDetection("StringLockDeadlock");

        // 测试嵌套锁死锁场景
        System.out.println("\n3. 测试嵌套锁死锁场景：");
        testNestedLockDetection();

        // 测试null锁场景
        System.out.println("\n4. 测试null锁场景：");
        testNullLockDetection();

        // 测试ReentrantLock死锁场景
        System.out.println("\n5. 测试ReentrantLock死锁场景：");
        testReentrantLockDetection();

        // 测试无死锁场景
        System.out.println("\n6. 测试无死锁场景：");
        testNoDeadlockDetection();
    }

    /**
     * 测试基本死锁检测
     */
    private static void testDeadlockDetection(String scenarioName) {
        DeadlockDetector detector = new DeadlockDetector();

        // 添加测试场景的锁关系
        detector.addProcessHoldsResource("Thread1", "Lock1");
        detector.addProcessWaitsForResource("Thread1", "Lock2");
        detector.addProcessHoldsResource("Thread2", "Lock2");
        detector.addProcessWaitsForResource("Thread2", "Lock1");

        // 检测死锁
        List<List<DeadlockDetector.GraphNode>> deadlocks = detector.detectDeadlocks();

        if (!deadlocks.isEmpty()) {
            System.out.println(scenarioName + "场景检测到死锁！");
            for (List<DeadlockDetector.GraphNode> deadlock : deadlocks) {
                System.out.println("死锁环：" + formatCycle(deadlock));
            }
        } else {
            System.out.println(scenarioName + "场景未检测到死锁");
        }
    }

    /**
     * 测试嵌套锁检测
     */
    private static void testNestedLockDetection() {
        DeadlockDetector detector = new DeadlockDetector();

        // 添加嵌套锁关系
        detector.addProcessHoldsResource("Thread1", "OuterLock");
        detector.addProcessHoldsResource("Thread1", "InnerLock");
        detector.addProcessWaitsForResource("Thread1", "SharedLock");
        detector.addProcessHoldsResource("Thread2", "SharedLock");
        detector.addProcessWaitsForResource("Thread2", "OuterLock");

        List<List<DeadlockDetector.GraphNode>> deadlocks = detector.detectDeadlocks();

        if (!deadlocks.isEmpty()) {
            System.out.println("嵌套锁场景检测到死锁！");
            for (List<DeadlockDetector.GraphNode> deadlock : deadlocks) {
                System.out.println("死锁环：" + formatCycle(deadlock));
            }
        } else {
            System.out.println("嵌套锁场景未检测到死锁");
        }
    }

    /**
     * 测试null锁检测
     */
    private static void testNullLockDetection() {
        DeadlockDetector detector = new DeadlockDetector();

        // 添加null锁关系
        detector.addProcessWaitsForResource("Thread1", "null");
        detector.addProcessHoldsResource("Thread2", "Lock1");
        detector.addProcessWaitsForResource("Thread2", "null");

        List<List<DeadlockDetector.GraphNode>> deadlocks = detector.detectDeadlocks();

        if (!deadlocks.isEmpty()) {
            System.out.println("null锁场景检测到死锁！");
            for (List<DeadlockDetector.GraphNode> deadlock : deadlocks) {
                System.out.println("死锁环：" + formatCycle(deadlock));
            }
        } else {
            System.out.println("null锁场景未检测到死锁");
        }
    }

    /**
     * 测试ReentrantLock检测
     */
    private static void testReentrantLockDetection() {
        DeadlockDetector detector = new DeadlockDetector();

        // 添加ReentrantLock关系
        detector.addProcessHoldsResource("Thread1", "ReentrantLock1");
        detector.addProcessWaitsForResource("Thread1", "ReentrantLock2");
        detector.addProcessHoldsResource("Thread2", "ReentrantLock2");
        detector.addProcessWaitsForResource("Thread2", "ReentrantLock1");

        List<List<DeadlockDetector.GraphNode>> deadlocks = detector.detectDeadlocks();

        if (!deadlocks.isEmpty()) {
            System.out.println("ReentrantLock场景检测到死锁！");
            for (List<DeadlockDetector.GraphNode> deadlock : deadlocks) {
                System.out.println("死锁环：" + formatCycle(deadlock));
            }
        } else {
            System.out.println("ReentrantLock场景未检测到死锁");
        }
    }

    /**
     * 测试无死锁场景
     */
    private static void testNoDeadlockDetection() {
        DeadlockDetector detector = new DeadlockDetector();

        // 添加无死锁的锁关系
        detector.addProcessHoldsResource("Thread1", "Lock1");
        detector.addProcessHoldsResource("Thread1", "Lock2");
        detector.addProcessHoldsResource("Thread2", "Lock3");
        detector.addProcessHoldsResource("Thread2", "Lock4");

        List<List<DeadlockDetector.GraphNode>> deadlocks = detector.detectDeadlocks();

        if (!deadlocks.isEmpty()) {
            System.out.println("无死锁场景检测到死锁！");
            for (List<DeadlockDetector.GraphNode> deadlock : deadlocks) {
                System.out.println("死锁环：" + formatCycle(deadlock));
            }
        } else {
            System.out.println("无死锁场景未检测到死锁（正确）");
        }
    }

    /**
     * 格式化死锁环信息
     */
    private static String formatCycle(List<DeadlockDetector.GraphNode> cycle) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cycle.size() - 1; i++) {
            sb.append(cycle.get(i).getName()).append(" -> ");
        }
        sb.append(cycle.get(cycle.size() - 1).getName());
        return sb.toString();
    }
}