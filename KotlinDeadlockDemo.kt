// Kotlin死锁测试文件 - 用于验证DeadlockDetectorPlugin的Kotlin支持功能

class KotlinDeadlockDemo {
    // 创建两个锁对象
    private val lock1 = Any()
    private val lock2 = Any()
    
    // 使用Thread构造函数 + Lambda创建线程
    fun testLambdaThreadDeadlock() {
        println("测试Lambda线程死锁")
        
        // 线程1：先获取lock1，再尝试获取lock2
        val thread1 = Thread {
            synchronized(lock1) {
                println("线程1：获取了lock1")
                Thread.sleep(100) // 增加死锁概率
                synchronized(lock2) {
                    println("线程1：获取了lock2")
                }
            }
        }
        
        // 线程2：先获取lock2，再尝试获取lock1
        val thread2 = Thread {
            synchronized(lock2) {
                println("线程2：获取了lock2")
                Thread.sleep(100) // 增加死锁概率
                synchronized(lock1) {
                    println("线程2：获取了lock1")
                }
            }
        }
        
        thread1.start()
        thread2.start()
    }
    
    // 使用对象表达式实现Runnable
    fun testObjectExpressionDeadlock() {
        println("测试对象表达式死锁")
        
        val thread1 = Thread(object : Runnable {
            override fun run() {
                synchronized(lock1) {
                    println("线程1：获取了lock1")
                    Thread.sleep(100)
                    synchronized(lock2) {
                        println("线程1：获取了lock2")
                    }
                }
            }
        })
        
        val thread2 = Thread(object : Runnable {
            override fun run() {
                synchronized(lock2) {
                    println("线程2：获取了lock2")
                    Thread.sleep(100)
                    synchronized(lock1) {
                        println("线程2：获取了lock1")
                    }
                }
            }
        })
        
        thread1.start()
        thread2.start()
    }
    
    // 普通类实现Runnable接口
    inner class DeadlockRunnable1 : Runnable {
        override fun run() {
            synchronized(lock1) {
                println("Runnable1：获取了lock1")
                Thread.sleep(100)
                synchronized(lock2) {
                    println("Runnable1：获取了lock2")
                }
            }
        }
    }
    
    inner class DeadlockRunnable2 : Runnable {
        override fun run() {
            synchronized(lock2) {
                println("Runnable2：获取了lock2")
                Thread.sleep(100)
                synchronized(lock1) {
                    println("Runnable2：获取了lock1")
                }
            }
        }
    }
    
    fun testRunnableClassDeadlock() {
        println("测试Runnable类死锁")
        
        val thread1 = Thread(DeadlockRunnable1())
        val thread2 = Thread(DeadlockRunnable2())
        
        thread1.start()
        thread2.start()
    }
}

// 主函数，用于运行测试
fun main() {
    val demo = KotlinDeadlockDemo()
    
    // 选择要运行的死锁测试
    println("选择死锁测试类型：")
    println("1. Lambda线程死锁")
    println("2. 对象表达式死锁")
    println("3. Runnable类死锁")
    
    val choice = readLine()?.toIntOrNull() ?: 1
    
    when (choice) {
        1 -> demo.testLambdaThreadDeadlock()
        2 -> demo.testObjectExpressionDeadlock()
        3 -> demo.testRunnableClassDeadlock()
        else -> demo.testLambdaThreadDeadlock()
    }
    
    // 等待一段时间后退出
    Thread.sleep(2000)
    println("测试完成")
}