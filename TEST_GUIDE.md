# 死锁检测插件测试指南

## 1. 测试方法

### 方法一：使用DevEcoStudio环境测试

#### 步骤1：打开项目
- 启动DevEcoStudio
- 选择"打开项目"，导航到`/Users/wangyouyi/Desktop/DeadLock`目录
- 等待项目加载完成

#### 步骤2：运行测试文件
- 在项目结构中找到`entry/src/main/ets/utils/DeadlockTest.ets`
- 右键点击该文件，选择"运行 DeadlockTest.ets"
- 在控制台查看测试结果

#### 步骤3：修改测试用例
- 编辑`DeadlockTest.ets`中的`testCode`变量，替换为不同的测试场景
- 再次运行测试，验证死锁检测结果

### 方法二：使用Node.js独立测试脚本

#### 步骤1：创建测试脚本
- 参考项目中的`test_core.js`和`test_complex.js`文件
- 创建新的测试脚本，例如`custom_test.js`

#### 步骤2：运行测试
```bash
cd /Users/wangyouyi/Desktop/DeadLock
node custom_test.js
```

## 2. 测试场景示例

### 经典死锁场景
```java
class DeadlockExample {
    private static final Object lock1 = new Object();
    private static final Object lock2 = new Object();

    public static void main(String[] args) {
        new Thread(() -> {
            synchronized (lock1) {
                synchronized (lock2) {
                    // 死锁点
                }
            }
        }).start();

        new Thread(() -> {
            synchronized (lock2) {
                synchronized (lock1) {
                    // 死锁点
                }
            }
        }).start();
    }
}
```

### Null锁场景
```java
class NullLockExample {
    public static void main(String[] args) {
        final Object lock = null;
        
        new Thread(() -> {
            synchronized (lock) {
                // 可能导致死锁
            }
        }).start();
    }
}
```

### 字符串锁场景
```java
class StringLockExample {
    public static void main(String[] args) {
        new Thread(() -> {
            synchronized ("lockA") {
                synchronized ("lockB") {
                    // 死锁点
                }
            }
        }).start();

        new Thread(() -> {
            synchronized ("lockB") {
                synchronized ("lockA") {
                    // 死锁点
                }
            }
        }).start();
    }
}
```

### ReentrantLock场景
```java
import java.util.concurrent.locks.ReentrantLock;

class ReentrantLockExample {
    private static final ReentrantLock lock1 = new ReentrantLock();
    private static final ReentrantLock lock2 = new ReentrantLock();

    public static void main(String[] args) {
        new Thread(() -> {
            lock1.lock();
            try {
                lock2.lock();
                try {
                    // 死锁点
                } finally {
                    lock2.unlock();
                }
            } finally {
                lock1.unlock();
            }
        }).start();

        new Thread(() -> {
            lock2.lock();
            try {
                lock1.lock();
                try {
                    // 死锁点
                } finally {
                    lock1.unlock();
                }
            } finally {
                lock2.unlock();
            }
        }).start();
    }
}
```

## 3. 验证检测结果

### 死锁检测成功
当检测到死锁时，控制台将输出类似以下信息：
```
死锁检测结果：
检测到死锁！
死锁循环 1: thread1(进程) → lock1(资源) → thread2(进程) → lock2(资源) → thread1(进程)
```

### 无死锁情况
当没有检测到死锁时，控制台将输出：
```
死锁检测结果：
未检测到死锁
```

## 4. 扩展测试建议

1. **增加更多线程**：测试3个或更多线程的死锁场景
2. **混合锁类型**：同时使用synchronized和ReentrantLock
3. **复杂嵌套锁**：4层或更多层的嵌套锁结构
4. **长代码文件**：测试对大型代码文件的解析能力
5. **边界情况**：测试空锁、重复锁获取等边界情况

## 5. 调试技巧

- **查看资源分配图**：在`DeadlockDetector`类中添加调试方法，输出完整的资源分配图
- **跟踪锁操作**：在`CodeAnalyzer`类中添加日志，记录解析到的所有锁操作
- **分步执行**：使用断点调试，逐步跟踪死锁检测算法的执行过程

## 6. 性能测试

- 测试插件在大型项目中的运行速度
- 测试插件对高频代码修改的响应速度
- 测试插件对内存的占用情况

通过以上测试方法，您可以全面验证死锁检测插件的功能和性能，确保其能够在各种复杂场景下准确检测死锁。