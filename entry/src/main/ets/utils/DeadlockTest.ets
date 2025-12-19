// 测试文件：验证死锁检测解析器
import { CodeAnalyzer } from './DeadlockDetector';

// 测试代码：经典死锁场景
const testCode = `
class DeadlockExample {
    private static final Object lock1 = new Object();
    private static final Object lock2 = new Object();

    public static void main(String[] args) {
        // 线程1
        new Thread(new Runnable() {
            public void run() {
                synchronized (lock1) {
                    System.out.println("Thread 1 holds lock1");
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {}
                    synchronized (lock2) {
                        System.out.println("Thread 1 holds lock2");
                    }
                }
            }
        }).start();

        // 线程2
        new Thread(new Runnable() {
            public void run() {
                synchronized (lock2) {
                    System.out.println("Thread 2 holds lock2");
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {}
                    synchronized (lock1) {
                        System.out.println("Thread 2 holds lock1");
                    }
                }
            }
        }).start();
    }
}`;

// 创建解析器实例
const analyzer = new CodeAnalyzer();

// 分析代码
const detector = analyzer.analyzeCode(testCode);

// 检测死锁
const result = detector.detectDeadlocks();

// 打印检测结果
console.log("死锁检测结果：");
console.log(detector.formatDeadlockInfo(result.cycles));

// 打印资源分配图信息
console.log("\n资源分配图节点信息：");
// 注意：由于nodes是私有成员，我们无法直接访问
// 这里只是一个示意，实际应用中可能需要添加调试方法
console.log("线程1: 等待lock1 -> 持有lock1 -> 等待lock2 -> 持有lock2");
console.log("线程2: 等待lock2 -> 持有lock2 -> 等待lock1 -> 持有lock1");
