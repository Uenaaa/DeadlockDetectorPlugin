public class DeadlockDemo {
    // 创建两个资源锁
    private static final Object resource1 = new Object();
    private static final Object resource2 = new Object();

    public static void main(String[] args) {
        // 线程1：先获取resource1，再尝试获取resource2
        Thread thread1 = new Thread(() -> {
            synchronized (resource1) {
                System.out.println("线程1: 已获取资源1");
                
                try {
                    Thread.sleep(100); // 模拟处理时间
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                
                System.out.println("线程1: 尝试获取资源2...");
                synchronized (resource2) {
                    System.out.println("线程1: 已获取资源2");
                    // 执行一些操作
                }
            }
        });

        // 线程2：先获取resource2，再尝试获取resource1
        Thread thread2 = new Thread(() -> {
            synchronized (resource2) {
                System.out.println("线程2: 已获取资源2");
                
                try {
                    Thread.sleep(100); // 模拟处理时间
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                
                System.out.println("线程2: 尝试获取资源1...");
                synchronized (resource1) {
                    System.out.println("线程2: 已获取资源1");
                    // 执行一些操作
                }
            }
        });

        // 启动线程
        thread1.start();
        thread2.start();
    }
}