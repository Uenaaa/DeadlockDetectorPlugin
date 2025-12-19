import { CodeAnalyzer, DeadlockDetector, GraphNode } from '../utils/DeadlockDetector';

// 定义接口类型
interface Position {
  x: number;
  y: number;
}

interface Process {
  id: string;
  name: string;
  status: string;
}

interface Resource {
  id: string;
  name: string;
  heldBy: string;
}

interface Edge {
  from: string;
  to: string;
  type: string;
}

@Entry
@Component
struct Index {
  @State deadlockDetected: boolean = false;
  @State statusMessage: string = '请输入代码进行死锁检测';
  @State userCode: string = `// 示例：基本死锁情况
Object lock1 = new Object();
Object lock2 = new Object();

// 线程1
new Thread(() -> {
    synchronized (lock1) {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {}
        synchronized (lock2) {
            System.out.println("Thread 1 got lock2");
        }
    }
}).start();

// 线程2
new Thread(() -> {
    synchronized (lock2) {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {}
        synchronized (lock1) {
            System.out.println("Thread 2 got lock1");
        }
    }
}).start();`;
  
  // 死锁检测结果数据
  @State processes: Process[] = [];
  @State resources: Resource[] = [];
  @State edges: Edge[] = [];

  // 节点位置信息
  private nodePositions: Record<string, Position> = {};

  build() {
    Scroll() {
      Column() {
        // 顶部状态栏
        Row() {
          Text('鸿蒙死锁检测插件')
            .fontSize(22)
            .fontWeight(FontWeight.Bold)
            .textAlign(TextAlign.Center)
            .width('80%')
          
          Image('https://img.icons8.com/color/48/000000/lock--v1.png')
            .width(30)
            .height(30)
        }
        .padding(15)
        .justifyContent(FlexAlign.SpaceBetween)
        
        // 状态信息
        Text(this.statusMessage)
          .fontSize(14)
          .padding(10)
          .backgroundColor(this.deadlockDetected ? '#fff1f0' : '#f6ffed')
          .borderRadius(6)
          .width('95%')
          .textAlign(TextAlign.Center)
          .margin({ bottom: 5 })
        
        // 代码输入区域
        TextArea({
          placeholder: '请输入Java代码进行死锁检测',
          text: this.userCode
        })
          .onChange((value) => {
            this.userCode = value;
          })
          .width('95%')
          .height(300)
          .padding(15)
          .backgroundColor('#ffffff')
          .borderRadius(10)
          .margin({ top: 8, bottom: 10 })
          .fontSize(16)
        
        // 底部操作按钮
        Row() {
          Button('检测死锁')
            .fontSize(18)
            .padding(15)
            .width('45%')
            .onClick(() => {
              this.startDeadlockDetection();
            })
          
          Button('重置')
            .fontSize(18)
            .padding(15)
            .width('45%')
            .onClick(() => {
              this.resetDetection();
            })
        }
        .padding(15)
        .width('100%')
        .justifyContent(FlexAlign.SpaceAround)
        
        // 死锁可视化区域
        if (this.processes.length > 0 || this.resources.length > 0) {
          Stack() {
            // 绘制连接线
            ForEach(this.edges, (edge: Edge) => {
              Path()
                .width('100%')
                .height('100%')
                .commands(this.calculateEdgePath(edge))
                .stroke(this.deadlockDetected ? '#ff4d4f' : '#1890ff')
                .strokeWidth(3)
            })
            
            // 绘制进程节点
            ForEach(this.processes, (process: Process) => {
              this.drawProcessNode(process)
            })
            
            // 绘制资源节点
            ForEach(this.resources, (resource: Resource) => {
              this.drawResourceNode(resource)
            })
          }
          .width('100%')
          .height(250)
          .padding(15)
        }
      }
      .height('auto')
      .width('100%')
      .backgroundColor('#f5f5f5')
      .justifyContent(FlexAlign.Center)
      .padding(10)
    }
    .height('100%')
    .backgroundColor('#f5f5f5')
  }
  
  // 绘制进程节点
  @Builder
  private drawProcessNode(process: Process) {
    Circle()
      .width(60)
      .height(60)
      .fill(this.deadlockDetected ? '#ffccc7' : '#e6f7ff')
      .stroke(this.deadlockDetected ? '#ff4d4f' : '#1890ff')
      .strokeWidth(2)
      .position(this.getNodePosition(process.id))
      .onClick(() => {
        this.showProcessInfo(process);
      })

    Text(process.name)
      .fontSize(14)
      .fontWeight(FontWeight.Bold)
      .position(this.getTextPosition(process.id, 'process'))
      .width(50)
      .textAlign(TextAlign.Center)
  }
  
  // 绘制资源节点
  @Builder
  private drawResourceNode(resource: Resource) {
    Row()
      .width(80)
      .height(40)
      .backgroundColor(this.deadlockDetected ? '#fff1f0' : '#f0f9ff')
      .border({width: 2, color: this.deadlockDetected ? '#ff4d4f' : '#1890ff'})
      .borderRadius(10)
      .position(this.getNodePosition(resource.id))
      .onClick(() => {
        this.showResourceInfo(resource);
      })
      .justifyContent(FlexAlign.Center)
      .alignItems(VerticalAlign.Center)
    {
      Text(resource.name)
        .fontSize(14)
        .fontWeight(FontWeight.Bold)
    }
  }
  
  // 获取节点位置
  private getNodePosition(id: string): Position {
    const pos = this.nodePositions[id];
    return pos || {x: 0, y: 0};
  }
  
  // 获取文本位置
  private getTextPosition(id: string, type: string): Position {
    const pos = this.getNodePosition(id);
    const x = pos?.x || 0;
    const y = pos?.y || 0;
    if (type === 'process') {
      return {x: x + 10, y: y + 24};
    } else {
      return {x: x + 5, y: y + 14};
    }
  }
  
  // 计算边的路径
  private calculateEdgePath(edge: Edge): string {
    const fromPos = this.getNodePosition(edge.from);
    const toPos = this.getNodePosition(edge.to);
    
    const fromX = fromPos?.x || 0;
    const fromY = fromPos?.y || 0;
    const toX = toPos?.x || 0;
    const toY = toPos?.y || 0;
    
    const startX = edge.from.startsWith('p') ? (fromX + 45) : (fromX + 65);
    const startY = edge.from.startsWith('p') ? (fromY + 25) : (fromY + 15);
    const endX = edge.to.startsWith('p') ? toX : toX;
    const endY = edge.to.startsWith('p') ? (toY + 25) : (toY + 15);
    
    // 添加箭头
    const arrowSize = 6;
    const angle = Math.atan2(endY - startY, endX - startX);
    const arrowX1 = endX - arrowSize * Math.cos(angle - Math.PI / 6);
    const arrowY1 = endY - arrowSize * Math.sin(angle - Math.PI / 6);
    const arrowX2 = endX - arrowSize * Math.cos(angle + Math.PI / 6);
    const arrowY2 = endY - arrowSize * Math.sin(angle + Math.PI / 6);
    
    return `M${startX} ${startY} L${endX} ${endY} M${endX} ${endY} L${arrowX1} ${arrowY1} M${endX} ${endY} L${arrowX2} ${arrowY2}`;
  }
  
  // 显示进程信息
  private showProcessInfo(process: Process) {
    this.statusMessage = `进程信息: ${process.name} - ${process.status}`;
  }
  
  // 显示资源信息
  private showResourceInfo(resource: Resource) {
    this.statusMessage = `资源信息: ${resource.name} - 被${resource.heldBy}持有`;
  }
  
  // 开始死锁检测
  private startDeadlockDetection() {
    this.statusMessage = '正在执行死锁检测...';
    
    // 使用死锁检测器分析代码
    const analyzer = new CodeAnalyzer();
    const detector = analyzer.analyzeCode(this.userCode);
    const result = detector.detectDeadlocks();
    
    // 更新UI
    this.deadlockDetected = result.hasDeadlock;
    this.statusMessage = result.hasDeadlock ? 
      `检测到死锁！${detector.formatDeadlockInfo(result.cycles)}` : 
      '未检测到死锁';
    
    // 生成可视化数据
    this.generateVisualizationData(detector, result.cycles);
  }
  
  // 生成可视化数据
  private generateVisualizationData(detector: DeadlockDetector, cycles: GraphNode[][]) {
    // 简单生成一些测试数据用于可视化
    this.processes = [
      {id: 'p1', name: '线程1', status: '运行中'},
      {id: 'p2', name: '线程2', status: '运行中'}
    ];
    
    this.resources = [
      {id: 'r1', name: '锁1', heldBy: 'p1'},
      {id: 'r2', name: '锁2', heldBy: 'p2'}
    ];
    
    this.edges = [
      {from: 'p1', to: 'r2', type: 'request'},
      {from: 'p2', to: 'r1', type: 'request'},
      {from: 'r1', to: 'p1', type: 'hold'},
      {from: 'r2', to: 'p2', type: 'hold'}
    ];
    
    // 设置节点位置
    this.nodePositions = {
      'p1': {x: 50, y: 50},
      'p2': {x: 250, y: 50},
      'r1': {x: 50, y: 150},
      'r2': {x: 250, y: 150}
    };
  }
  
  // 重置检测
  private resetDetection() {
    this.deadlockDetected = false;
    this.statusMessage = '请输入代码进行死锁检测';
    this.processes = [];
    this.resources = [];
    this.edges = [];
    this.nodePositions = {};
  }
}