# DeadLockDetectorPlugin

这是一个用于检测DevEcoStudio并发代码中死锁情况的插件。该插件可以自动检测用户编写的代码中是否存在死锁，并支持鼠标点击特定位置进行检测。

## 功能特点

- **自动检测**：用户在编写代码时自动检测是否存在死锁情况
- **鼠标触发检测**：支持在特定位置点击鼠标进行死锁检测
- **经典算法**：使用资源分配图(RAG)和深度优先搜索(DFS)算法进行死锁检测
- **复杂锁场景处理**：支持处理null锁、字符串锁、嵌套锁等多种复杂情况

## 技术实现

- **核心算法**：资源分配图(RAG) + 深度优先搜索(DFS)循环检测
- **开发框架**：基于IntelliJ IDEA SDK开发，兼容DevEcoStudio
- **代码分析**：解析PsiElements识别锁的获取和释放模式
- **可视化界面**：提供直观的死锁可视化效果

## 文件结构

```
DeadLock/
├── AppScope/              # 应用范围配置
├── entry/                 # 主入口模块
│   ├── src/main/ets/      # ArkTS源代码
│   ├── src/main/resources/ # 资源文件
│   └── build-profile.json5 # 构建配置
├── DeadlockDetector.java  # 死锁检测核心算法
├── DeadlockTestCases.java # 测试用例
├── README.md             # 项目说明
└── .gitignore            # Git忽略文件
```

## 使用方法

1. 在DevEcoStudio中安装该插件
2. 在代码编辑区域编写并发代码
3. 插件会自动检测死锁情况并提示
4. 可以点击特定位置触发死锁检测

## 测试场景

- 基本死锁检测
- 字符串锁死锁检测
- 嵌套锁死锁检测
- null锁处理
- ReentrantLock死锁检测

## 开发环境

- DevEcoStudio
- Java
- ArkTS

## 上传到GitHub步骤

1. 初始化Git仓库：`git init`
2. 添加所有文件：`git add .`
3. 提交代码：`git commit -m "first commit"`
4. 添加远程仓库：`git remote add origin https://github.com/Jenaaaa/DeadlockDetectorPlugin.git`
5. 推送代码：`git push -u origin main`

## 作者

Jenaaaa

## 许可证

MIT License
