package com.deadlock.detector.test

import java.util.concurrent.locks.ReentrantLock

/**
 * Kotlin死锁测试示例
 * 包含多种Kotlin特有的线程创建和锁使用方式
 */
class KotlinDeadlockDemo {
    // 测试用锁对象
    private val lock1 = Any()
    private val lock2 = Any()
    private val reentrantLock1 = ReentrantLock()
    private val reentrantLock2 = ReentrantLock()

    /**
     * 场景1：使用Lambda表达式创建线程的死锁
     * synchronized函数调用方式
     */
    fun testLambdaThreadDeadlock() {
        // 线程1：先获取lock1，再尝试获取lock2
        val thread1 = Thread {
            synchronized(lock1) {
                println("线程1：获取了lock1")
                Thread.sleep(100)
                synchronized(lock2) {
                    println("线程1：获取了lock2")
                }
            }
        }

        // 线程2：先获取lock2，再尝试获取lock1
        val thread2 = Thread {
            synchronized(lock2) {
                println("线程2：获取了lock2")
                Thread.sleep(100)
                synchronized(lock1) {
                    println("线程2：获取了lock1")
                }
            }
        }

        thread1.start()
        thread2.start()
    }

    /**
     * 场景2：使用对象表达式创建Runnable的死锁
     */
    fun testObjectExpressionDeadlock() {
        // 线程1：使用对象表达式实现Runnable
        val thread1 = Thread(object : Runnable {
            override fun run() {
                synchronized(lock1) {
                    println("对象表达式线程1：获取了lock1")
                    Thread.sleep(100)
                    synchronized(lock2) {
                        println("对象表达式线程1：获取了lock2")
                    }
                }
            }
        })

        // 线程2：使用对象表达式实现Runnable
        val thread2 = Thread(object : Runnable {
            override fun run() {
                synchronized(lock2) {
                    println("对象表达式线程2：获取了lock2")
                    Thread.sleep(100)
                    synchronized(lock1) {
                        println("对象表达式线程2：获取了lock1")
                    }
                }
            }
        })

        thread1.start()
        thread2.start()
    }

    /**
     * 场景3：使用普通类实现Runnable的死锁
     */
    fun testRunnableClassDeadlock() {
        val runnable1 = DeadlockRunnable1()
        val runnable2 = DeadlockRunnable2()

        val thread1 = Thread(runnable1)
        val thread2 = Thread(runnable2)

        thread1.start()
        thread2.start()
    }

    /**
     * 场景4：使用ReentrantLock的死锁
     */
    fun testReentrantLockDeadlock() {
        // 线程1：先获取reentrantLock1，再尝试获取reentrantLock2
        val thread1 = Thread {
            reentrantLock1.lock()
            try {
                println("ReentrantLock线程1：获取了reentrantLock1")
                Thread.sleep(100)
                reentrantLock2.lock()
                try {
                    println("ReentrantLock线程1：获取了reentrantLock2")
                } finally {
                    reentrantLock2.unlock()
                }
            } finally {
                reentrantLock1.unlock()
            }
        }

        // 线程2：先获取reentrantLock2，再尝试获取reentrantLock1
        val thread2 = Thread {
            reentrantLock2.lock()
            try {
                println("ReentrantLock线程2：获取了reentrantLock2")
                Thread.sleep(100)
                reentrantLock1.lock()
                try {
                    println("ReentrantLock线程2：获取了reentrantLock1")
                } finally {
                    reentrantLock1.unlock()
                }
            } finally {
                reentrantLock2.unlock()
            }
        }

        thread1.start()
        thread2.start()
    }

    // 普通类实现Runnable接口
    private inner class DeadlockRunnable1 : Runnable {
        override fun run() {
            synchronized(lock1) {
                println("Runnable类线程1：获取了lock1")
                Thread.sleep(100)
                synchronized(lock2) {
                    println("Runnable类线程1：获取了lock2")
                }
            }
        }
    }

    // 普通类实现Runnable接口
    private inner class DeadlockRunnable2 : Runnable {
        override fun run() {
            synchronized(lock2) {
                println("Runnable类线程2：获取了lock2")
                Thread.sleep(100)
                synchronized(lock1) {
                    println("Runnable类线程2：获取了lock1")
                }
            }
        }
    }
}

// 运行测试
fun main() {
    val demo = KotlinDeadlockDemo()
    println("测试Lambda线程死锁...")
    demo.testLambdaThreadDeadlock()
    
    // 等待测试完成
    Thread.sleep(1000)
    
    println("测试对象表达式死锁...")
    demo.testObjectExpressionDeadlock()
    
    Thread.sleep(1000)
    
    println("测试Runnable类死锁...")
    demo.testRunnableClassDeadlock()
    
    Thread.sleep(1000)
    
    println("测试ReentrantLock死锁...")
    demo.testReentrantLockDeadlock()
}