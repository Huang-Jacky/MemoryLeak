# Android 内存泄漏
### 一、数据存储与内存分配
1. **寄存器**（Registers）最快的存储区域，位于 CPU 内部 [^2]。然而，寄存器的数量十分有限，所以寄存器根据需求进行分配。我们对其没有直接的控制权，也无法在自己的程序里找到寄存器存在的踪迹（另一方面，C/C++ 允许开发者向编译器建议寄存器的分配）。

2. **栈内存**（Stack）存在于常规内存 RAM（随机访问存储器，Random Access Memory）区域中，可通过栈指针获得处理器的直接支持。栈指针下移分配内存，上移释放内存，这是一种快速有效的内存分配方法，速度仅次于寄存器。创建程序时，Java 系统必须准确地知道栈内保存的所有项的生命周期。这种约束限制了程序的灵活性。因此，虽然在栈内存上存在一些 Java 数据，特别是对象引用，但 Java 对象却是保存在堆内存的。

3. **堆内存**（Heap）这是一种通用的内存池（也在 RAM 区域），所有 Java 对象都存在于其中。与栈内存不同，编译器不需要知道对象必须在堆内存上停留多长时间。因此，用堆内存保存数据更具灵活性。创建一个对象时，只需用 `new` 命令实例化对象即可，当执行代码时，会自动在堆中进行内存分配。这种灵活性是有代价的：分配和清理堆内存要比栈内存需要更多的时间（如果可以用 Java 在栈内存上创建对象，就像在 C++ 中那样的话）。随着时间的推移，Java 的堆内存分配机制现在已经非常快，因此这不是一个值得关心的问题了。

4. **常量存储**（Constant storage）常量值通常直接放在程序代码中，因为它们永远不会改变。如需严格保护，可考虑将它们置于只读存储器 ROM （只读存储器，Read Only Memory）中 [^3]。

5. **非 RAM 存储**（Non-RAM storage）数据完全存在于程序之外，在程序未运行以及脱离程序控制后依然存在。两个主要的例子：（1）序列化对象：对象被转换为字节流，通常被发送到另一台机器；（2）持久化对象：对象被放置在磁盘上，即使程序终止，数据依然存在。这些存储的方式都是将对象转存于另一个介质中，并在需要时恢复成常规的、基于 RAM 的对象。Java 为轻量级持久化提供了支持。而诸如 JDBC 和 Hibernate 这些类库为使用数据库存储和检索对象信息提供了更复杂的支持。

&emsp;&emsp;Java中的基本数据类型不通过关键字 new 创建，而是使用一个“自动”变量直接存储"值"，并置于栈内存中，Java 确定了每种基本类型的内存占用大小；而通过 new 创建的引用类型的对象都是保存在堆内存中，并自动在堆中进行内存分配。

#### Java中的引用类型

##### 强引用(Strong reference)
实际编码中最常见的一种引用类型。常见形式如：A a = new A();等。强引用本身存储在栈内存中，其存储指向对内存中对象的地址。一般情况下，当对内存中的对象不再有任何强引用指向它时，垃圾回收机器开始考虑可能要对此内存进行的垃圾回收。如当进行编码：a = null，此时，刚刚在堆中分配地址并新建的a对象没有其他的任何引用，当系统进行垃圾回收时，堆内存将被垃圾回收。
>SoftReference、WeakReference、PhantomReference都是类java.lang.ref.Reference的子类。Reference作为抽象基类，定义了其子类对象的基本操作。Reference子类都具有如下特点：
<br/>1.Reference子类不能无参化直接创建，必须至少以强引用对象为构造参数，创建各自的子类对象；
<br/>2.因为1中以强引用对象为构造参数创建对象，因此，使得原本强引用所指向的堆内存中的对象将不再只与强引用本身直接关联，与Reference的子类对象的引用也有一定联系。且此种联系将可能影响到对象的垃圾回收。
##### 软引用(Soft Reference)
```
A a = new A();
SoftReference<A> srA = new SoftReference<A>(a);
```
通过对象的强引用为参数，创建了一个SoftReference对象，并使栈内存中的srA指向此对象。

软引用所指示的对象被垃圾回收需要满足两个条件：

1.当其指示的对象没有任何强引用对象指向它；

2.当虚拟机内存不足时；

SoftReference变相的延长了其指示对象占据堆内存的时间，直到虚拟机内存不足时垃圾回收器才回收此堆内存空间。
##### 弱引用(Weak Reference)
```
A a = new A();
WeakReference<A> wrA = new WeakReference<A>(a);
```
WeakReference不改变原有强引用对象的垃圾回收时机，一旦其指示对象没有任何强引用对象时，此对象即进入正常的垃圾回收流程。

当前已有强引用指向强引用对象，此时由于业务需要，需要增加对此对象的引用，同时又不希望改变此引用的垃圾回收时机，此时WeakReference正好符合需求，常见于一些与生命周期的场景中。
##### 虚引用(Phantom Reference)
与SoftReference或WeakReference相比，PhantomReference主要差别体现在如下几点：

1.PhantomReference只有一个构造函数PhantomReference(T referent, ReferenceQueue<? super T> q)，因此，PhantomReference使用必须结合ReferenceQueue；

2.不管有无强引用指向PhantomReference的指示对象，PhantomReference的get()方法返回结果都是null。

与WeakReference相同，PhantomReference并不会改变其指示对象的垃圾回收时机。且可以总结出：ReferenceQueue的作用主要是用于监听SoftReference/WeakReference/PhantomReference的指示对象是否已经被垃圾回收。
### 二、数据的清理
1. **作用域**，Java 中，作用域是由大括号 {} 的位置决定的,决定了在该范围内定义的变量名的可见性和生存周期。Java 的变量只有在其作用域内才可用。
```
{
    int x = 12;
    // 仅 x 变量可用
    {
        int q = 96;
        // x 和 q 变量皆可用
    }
    // 仅 x 变量可用
    // 变量 q 不在作用域内
}
```
1. **对象作用域**，Java 对象与基本类型具有不同的生命周期。当我们使用 new 关键字来创建 Java 对象时，它的生命周期将会超出作用域。
```
{
    String s = new String("a string");
} 
// 作用域终点
```
&emsp;&emsp;上例中，引用 s 在作用域终点就结束了。但是，引用 s 指向的字符串对象依然还在占用内存。

&emsp;&emsp;我们在 Java 中并没有主动清理这些对象，那么它是如何避免 C++ 中出现的内存被填满从而阻塞程序的问题呢？答案是：Java 的垃圾收集器(garbage collection，简称GC)会检查所有 new 出来的对象并判断哪些不再可达(如果再没有引用指向该对象，那么该对象就无从处理或调用该对象，这样的对象称为不可到达unreachable)，继而释放那些被占用的内存，供其他新的对象使用。也就是说，我们不必担心内存回收的问题了。你只需简单创建对象即可。当其不再被需要时，能自行被垃圾收集器释放。垃圾回收机制有效防止了因程序员忘记释放内存而造成的“内存泄漏”问题。
# 三、内存泄漏
### 3.1内存泄漏原因分析
&emsp;&emsp;上述我们已经知道数据存储的位置以及内存的分配，并且知道了Java的垃圾回收器会自动的清理不再可达的对象,但是如果对象一直被引用，那么该对象将无法被回收，造成内存不能被释放和使用。因此**对象无法被GC回收是造成内存泄露的主要原因**。

&emsp;&emsp;**一般内存泄漏**(traditional memory leak)是：由忘记释放分配的内存导致的。

&emsp;&emsp;**逻辑内存泄漏**(logical memory leak)是：当应用不再需要这个对象，当仍未释放该对象的所有引用。

>注意：与内存泄漏容易混淆的是内存溢出，内存溢出是指程序向系统申请的内存空间超出了系统所能使用的最大内存空间，大量的内存泄露会导致内存溢出(OOM)。
### 3.2内存泄漏的影响
&emsp;&emsp;内存泄漏，可能会导致应用卡顿以及出现 OOM。
# 四、内存泄漏的检测工具
### 4.1 Android Studio Profiler
点击 View > Tool Windows > Android Profiler打开
### 4.2 LeakCanary 
配置依赖：
```
    /*配置 leakcanary-android*/
    debugImplementation 'com.squareup.leakcanary:leakcanary-android:1.6.3'
    releaseImplementation 'com.squareup.leakcanary:leakcanary-android-no-op:1.6.3'
    // Optional, if you use support library fragments:
    debugImplementation 'com.squareup.leakcanary:leakcanary-support-fragment:1.6.3'
```
代码配置：
```
        //配置内存泄漏检测
        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return;
        }
        LeakCanary.install(this);
```
![leak1.png](screenshot/leak1.png)
# 五、常见的内存泄漏场景
### 5.1非静态内部类
非静态内部类会持有外部类实例的引用，如果非静态内部类的实例是静态的，就会间接的长期维持着外部类的引用，阻止被系统回收。
### 5.2匿名内部类
比如AsyncTask,当AsyncTask的异步任务在后台执行耗时任务期间，AsyncTask所在Activity被销毁了，被AsyncTask持有的Activity实例不会被垃圾收集器回收，直到异步任务结束；
解决办法就是自定义一个静态的AsyncTask。
### 5.3Handler内存泄漏
Handler的Message被存储在MessageQueue中，有些Message并不能马上被处理，它们在MessageQueue中存在的时间会很长，这就会导致Handler无法被回收。如果Handler 是非静态的，则Handler也会导致引用它的Activity或者Service不能被回收。
### 5.4未正确使用Context
对于不是必须使用Activity Context的情况（Dialog的Context就必须是Activity Context），我们可以考虑使用Application Context来代替Activity的Context
### 5.5静态View
使用静态View可以避免每次启动Activity都去读取并渲染View，但是静态View会持有Activity的引用，导致Activity无法被回收，解决的办法就是在onDestory方法中将静态View置为null。
### 5.6资源对象未关闭
资源对象比如Cursor、File等，往往都用了缓冲，不使用的时候应该关闭它们。把他们的引用置为null，而不关闭它们，往往会造成内存泄漏。因此，在资源对象不使用时，一定要确保它已经关闭，通常在finally语句中关闭，防止出现异常时，资源未被释放的问题。
### 5.7集合中对象没清理
通常把一些对象的引用加入到了集合中，当不需要该对象时，如果没有把它的引用从集合中清理掉，这样这个集合就会越来越大。如果这个集合是static的话，那情况就会更加严重。
### 5.8Bitmap对象
临时创建的某个相对比较大的bitmap对象，在经过变换得到新的bitmap对象之后，应该尽快回收原始的bitmap，这样能够更快释放原始bitmap所占用的空间。
避免静态变量持有比较大的bitmap对象或者其他大的数据对象，如果已经持有，要尽快置空该静态变量。
### 5.9监听器未关闭
很多系统服务（比如TelephonyManager、SensorManager）需要register和unregister监听器，我们需要确保在合适的时候及时unregister那些监听器。自己手动add的Listener，要记得在合适的时候及时remove这个Listener。
### 5.10WebView
不同的Android版本的WebView会有差异，加上不同厂商的定制ROM的WebView的差异，这就导致WebView存在着很大的兼容性问题。WebView都会存在内存泄漏的问题，在应用中只要使用一次WebView，内存就不会被释放掉。通常的解决办法就是为WebView单开一个进程，使用AIDL与应用的主进程进行通信。WebView进程可以根据业务需求，在合适的时机进行销毁。
>静态内部类和非静态内部类的区别很微妙，非静态内部类(包括匿名内部类)会持有外部类实例的引用；但每一个Android开发者都应该注意到这个标准，避免在Activity中使用非静态内部类，如果该类的实例会存在在Activity的生命周期之外。必须使用静态内部类持有一个外部类的弱引用替代。
### 六、参考文章
Java编程思想(Java8)

[避免可控的内存泄漏 ](http://liuwangshu.cn/application/performance/ram-3-memory-leak.html)





