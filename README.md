# DeadLockDetectorPlugin

这是一个用于检测IntelliJ IDEA/DevEcoStudio并发代码中死锁情况的插件。该插件可以检测用户编写的Java代码中是否存在死锁，并提供死锁检测结果。

## 功能特点

- **死锁检测**：使用资源分配图(RAG)和深度优先搜索(DFS)算法进行死锁检测
- **IDE集成**：在IntelliJ IDEA/DevEcoStudio中通过右键菜单触发死锁检测
- **代码分析**：解析Java代码结构识别线程创建、同步块和锁的获取模式
- **循环检测**：检测资源分配图中的循环等待条件

## 技术实现

- **核心算法**：资源分配图(RAG) + 深度优先搜索(DFS)循环检测
- **开发框架**：基于IntelliJ IDEA SDK开发的插件
- **代码分析**：使用Psi API解析Java代码结构（线程、同步块、Lambda表达式等）
- **构建系统**：基于Gradle和IntelliJ Platform Plugin构建

## 文件结构

```
.
├── .run/                   # Predefined Run/Debug Configurations
├── build/                  # Output build directory
├── gradle
│   ├── wrapper/            # Gradle Wrapper
├── src                     # Plugin sources
│   ├── main
│   │   ├── java/           # Java production sources
│   │   │   └── com/deadlock/detector/  # Plugin implementation
│   │   │       ├── action/      # IDE actions
│   │   │       ├── detector/    # Deadlock detection logic
│   │   │       └── model/       # Data models
│   │   └── resources/      # Resources - plugin.xml, icons, messages
│   │       └── META-INF/
│   │           └── plugin.xml  # Plugin configuration
├── .gitignore              # Git ignoring rules
├── build.gradle.kts        # Gradle build configuration
├── gradle.properties       # Gradle configuration properties
├── gradlew                 # *nix Gradle Wrapper script
├── gradlew.bat             # Windows Gradle Wrapper script
├── README.md               # README
└── settings.gradle.kts     # Gradle project settings
```

## 使用方法

1. 在IntelliJ IDEA/DevEcoStudio中打开该项目
2. 使用Gradle的`runIde`任务运行插件
3. 在打开的IDE中编写Java并发代码
4. 右键点击代码编辑器，选择"Detect Deadlock"菜单项
5. 插件会分析代码并显示死锁检测结果

## 测试用例

项目中包含DeadlockDemo.java测试文件，演示了经典死锁场景：

```java
public class DeadlockDemo {
    private static final Object resource1 = new Object();
    private static final Object resource2 = new Object();

    public static void main(String[] args) {
        // 线程1：先获取resource1，再尝试获取resource2
        Thread thread1 = new Thread(() -> {
            synchronized (resource1) {
                System.out.println("线程1: 已获取资源1");
                try { Thread.sleep(100); } catch (InterruptedException e) { e.printStackTrace(); }
                System.out.println("线程1: 尝试获取资源2...");
                synchronized (resource2) { System.out.println("线程1: 已获取资源2"); }
            }
        });
        
        // 线程2：先获取resource2，再尝试获取resource1
        Thread thread2 = new Thread(() -> {
            synchronized (resource2) {
                System.out.println("线程2: 已获取资源2");
                try { Thread.sleep(100); } catch (InterruptedException e) { e.printStackTrace(); }
                System.out.println("线程2: 尝试获取资源1...");
                synchronized (resource1) { System.out.println("线程2: 已获取资源1"); }
            }
        });
        
        thread1.start();
        thread2.start();
    }
}
```

## Plugin Configuration

The plugin configuration file is located at `src/main/resources/META-INF/plugin.xml`. It provides general information about the plugin, its dependencies, extensions, and listeners.

## Running the Plugin

Use the predefined Run/Debug configurations or run the following Gradle task:

```bash
./gradlew runIde
```

This will start a new instance of IntelliJ IDEA with the plugin installed.

## Building the Plugin

To build the plugin distribution:

```bash
./gradlew buildPlugin
```

The distribution will be created in the `build/distributions` directory.

## 作者

Uenaaa

## 许可证

MIT License
