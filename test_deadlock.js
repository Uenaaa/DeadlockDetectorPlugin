// 测试死锁检测功能
const { CodeAnalyzer, NodeType } = require('./entry/src/main/ets/utils/DeadlockDetector.ets');

// 测试经典死锁场景
const testCode1 = `
class DeadlockExample {
    private static final Object lock1 = new Object();
    private static final Object lock2 = new Object();
    
    public static void main(String[] args) {
        // Thread 1: lock1 -> lock2
        new Thread(new Runnable() {
            public void run() {
                synchronized (lock1) {
                    synchronized (lock2) {
                        System.out.println("Thread 1 got both locks");
                    }
                }
            }
        }).start();
        
        // Thread 2: lock2 -> lock1
        new Thread(new Runnable() {
            public void run() {
                synchronized (lock2) {
                    synchronized (lock1) {
                        System.out.println("Thread 2 got both locks");
                    }
                }
            }
        }).start();
    }
}`;

// 测试无死锁场景
const testCode2 = `
class NoDeadlockExample {
    private static final Object lock1 = new Object();
    private static final Object lock2 = new Object();
    
    public static void main(String[] args) {
        // Thread 1: lock1 -> lock2
        new Thread(new Runnable() {
            public void run() {
                synchronized (lock1) {
                    synchronized (lock2) {
                        System.out.println("Thread 1 got both locks");
                    }
                }
            }
        }).start();
        
        // Thread 2: lock1 -> lock2 (same order)
        new Thread(new Runnable() {
            public void run() {
                synchronized (lock1) {
                    synchronized (lock2) {
                        System.out.println("Thread 2 got both locks");
                    }
                }
            }
        }).start();
    }
}`;

// 测试嵌套锁场景
const testCode3 = `
class NestedLockExample {
    private static final Object lock1 = new Object();
    private static final Object lock2 = new Object();
    private static final Object lock3 = new Object();
    
    public static void main(String[] args) {
        // Thread 1: lock1 -> lock2 -> lock3
        new Thread(new Runnable() {
            public void run() {
                synchronized (lock1) {
                    synchronized (lock2) {
                        synchronized (lock3) {
                            System.out.println("Thread 1 got all locks");
                        }
                    }
                }
            }
        }).start();
        
        // Thread 2: lock3 -> lock2 -> lock1
        new Thread(new Runnable() {
            public void run() {
                synchronized (lock3) {
                    synchronized (lock2) {
                        synchronized (lock1) {
                            System.out.println("Thread 2 got all locks");
                        }
                    }
                }
            }
        }).start();
    }
}`;

// 执行测试
function runTest(name, code) {
    console.log(`\n=== ${name} ===`);
    console.log(code);
    
    const analyzer = new CodeAnalyzer();
    const detector = analyzer.analyzeCode(code);
    const result = detector.detectDeadlocks();
    
    console.log(`\n检测结果:`);
    console.log(detector.formatDeadlockInfo(result.cycles));
}

// 运行所有测试
runTest("经典死锁场景", testCode1);
runTest("无死锁场景", testCode2);
runTest("嵌套锁死锁场景", testCode3);
