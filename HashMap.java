https://blog.csdn.net/qq_33842627/article/details/88690749


HashMap的常量
/默认的初始容量
static final int DEFAULT_INITIAL_CAPACITY = 1 << 4; // aka 16

//hashmap的最大容量值
static final int MAXIMUM_CAPACITY = 1 << 30;

//负载因子
//关于为什么默认的负载因子选值为0.75源码中的一段注释是这样解释的根据泊松分布，
Ideally, under random hashCodes, the frequency of
nodes in bins follows a Poisson distribution
(http://en.wikipedia.org/wiki/Poisson_distribution) with a
parameter of about 0.5 on average for the default resizing
threshold of 0.75, although with a large variance because of
resizing granularity. Ignoring variance, the expected
occurrences of list size k are (exp(-0.5) * pow(0.5, k) 
factorial(k)). The first values are:
0: 0.60653066
1: 0.30326533
2: 0.07581633
3: 0.01263606
4: 0.00157952
5: 0.00015795
6: 0.00001316
7: 0.00000094
8: 0.00000006
more: less than 1 in ten million
简单翻译一下就是在理想情况下,使用随机哈希码,节点出现的频率在hash桶中遵循泊松分布，同时给出了桶中元素个数和概率的对照表。
从上面的表中可以看到当桶中元素到达8个的时候，概率已经变得非常小，也就是说用0.75作为加载因子，每个碰撞位置的链表长度超过８个是几乎不可能的。


static final float DEFAULT_LOAD_FACTOR = 0.75f;

//JDK1.8 ，Entry链表最大长度，当table中节点数目大于该长度时，将链表转成红黑树存储；
static final int TREEIFY_THRESHOLD = 8;

//JDK1.8 ，当table中节点数小于该长度，将红黑树转为链表存储；
static final int UNTREEIFY_THRESHOLD = 6;

//table可能被转化为树形结构的最小容量。当哈希表的大小超过这个阈值，才会把链式结构转化成树型结构，否则仅采取扩容来尝试减少冲突。
static final int MIN_TREEIFY_CAPACITY = 64;
总结：

1.hashmap的负载因子的初值为0.75

2.当桶中节点的键值对数大于8时节点存储结构转为红黑树，当节点树小于6时存储结构转为链表






HashMap的构造方法
//HashMap的构造方法有4个，一个默认构造方法和3个重载方法

//此构造方法由使用者自己定义HashMap的初始容量和负载因子
public HashMap(int initialCapacity, float loadFactor) {
	//初始容量小于0，抛出非法参数异常
	if (initialCapacity < 0)
		throw new IllegalArgumentException("Illegal initial capacity: " +
										   initialCapacity);
	//初始容量大于最大容量，将以最大容量给其赋值
	if (initialCapacity > MAXIMUM_CAPACITY)
		initialCapacity = MAXIMUM_CAPACITY;
	//负载因子为负数，检测非法运算。
	if (loadFactor <= 0 || Float.isNaN(loadFactor))
		throw new IllegalArgumentException("Illegal load factor: " +
										   loadFactor);
	this.loadFactor = loadFactor;

	this.threshold = tableSizeFor(initialCapacity);
}


public HashMap(int initialCapacity) {
	   //默认负载因子为0.75
		this(initialCapacity, DEFAULT_LOAD_FACTOR);
	}

public HashMap() {
		this.loadFactor = DEFAULT_LOAD_FACTOR; // all other fields defaulted
	}

public HashMap(Map<? extends K, ? extends V> m) {
		this.loadFactor = DEFAULT_LOAD_FACTOR;
		//将m的键值对插入此map中，类似于复制map
		putMapEntries(m, false);
}






HashMap的put方法:
public V put(K key, V value) {
        return putVal(hash(key), key, value, false, true);
    }

//onlyIfAbsent 默认false，表示表示允许旧值替换。
//evict  构造方法中可以对其进行定义，默认为true，表示HashMap不处于创建模式。
final V putVal(int hash, K key, V value, boolean onlyIfAbsent,
                   boolean evict) {
	//散列
	Node<K,V>[] tab;
	//需要插入的键值对
	Node<K,V> p;
	//n：散列长度，i：插入位置的index
	int n, i;
	//如果hashtable为空使用resize（）方法创建一个长度为n的散列
	if ((tab = table) == null || (n = tab.length) == 0)
		n = (tab = resize()).length;
	//通过Entry<k,v>的hash码和散列长度做与运算，找到插入位置的index。
	//如果该位置没有其他键值对存在则插入入。
	if ((p = tab[i = (n - 1) & hash]) == null)  //i = (n - 1) & hash   这里进行按位与,前面提到无论是初始化Node类型的数组还是resize(),数组大小都必须是2的幂次方,这里可以很好地解释,(n-1)的二进制前面都是0,只有后面都是1的参与运算参与,这样无论hash前面位是什么,都不会导致溢出.
		tab[i] = newNode(hash, key, value, null);
	//存在冲突
	else {
		Node<K,V> e; K k;
	   //比较该下标第一个元素的hash值相等key相等，相等则用e记录下来
		if (p.hash == hash &&
			((k = p.key) == key || (key != null && key.equals(k))))
			e = p;
	   //如果该下标的元素是红黑树类型且没有该键值对则放到树中
		else if (p instanceof TreeNode)
			e = ((TreeNode<K,V>)p).putTreeVal(this, tab, hash, key, value);
		else {
			//链表类型的遍历
			for (int binCount = 0; ; ++binCount) {
				//到达尾部
				if ((e = p.next) == null) {
					p.next = newNode(hash, key, value, null);
					//判断是否需要转成红黑树
					if (binCount >= TREEIFY_THRESHOLD - 1) // -1 for 1st
						treeifyBin(tab, hash);
					break;
				}
				 // 链表节点的<key, value>与put操作<key, value>相同时，不做重复操作，跳出循环
				if (e.hash == hash &&
					((k = e.key) == key || (key != null && key.equals(k))))
					break;
				p = e;
			}
		}
   // 找到或新建一个key和hashCode与插入元素相等的键值对，进行put操作
		if (e != null) { // existing mapping for key
			V oldValue = e.value;
			if (!onlyIfAbsent || oldValue == null)
				e.value = value;
			afterNodeAccess(e);
			return oldValue;
		}
	}
	++modCount;
	//如果大小超过阈值则扩容，threshold=负载因子*容量
	if (++size > threshold)
		resize();
	afterNodeInsertion(evict);
	return null;
}






//HashMap 扩容方法
final Node<K,V>[] resize() {
	//用oldTab记录旧散列表
	Node<K,V>[] oldTab = table;
	//旧桶的容量
	int oldCap = (oldTab == null) ? 0 : oldTab.length;
	//旧桶的阈值
	int oldThr = threshold;
	//初始化新桶的容量，阈值
	int newCap, newThr = 0;
	//如果旧桶不为空
	if (oldCap > 0) {
		//旧桶的容量大于等于最大容量，将阈值等于int类型的最大值（0x7fffffff），完成扩容
		if (oldCap >= MAXIMUM_CAPACITY) {
			threshold = Integer.MAX_VALUE;
			return oldTab;
		}
		//旧桶容量的两倍小于最大容量，且旧桶容量大于等于默认容量（新桶容量在这扩大到原来的两倍）
		else if ((newCap = oldCap << 1) < MAXIMUM_CAPACITY &&
				 oldCap >= DEFAULT_INITIAL_CAPACITY)
			//新阈值变为旧阈值的两倍     
			newThr = oldThr << 1; // double threshold
	}
	//数组为空的情况
	else if (oldThr > 0) // initial capacity was placed in threshold
	 //数组的新容量设置为老数组扩容的临界值
		newCap = oldThr;
	//如果旧容量 <= 0，且旧临界值 <= 0，新容量扩充为默认初始化容量，新临界值为DEFAULT_LOAD_FACTOR * DEFAULT_INITIAL_CAPACITY
	else {               // zero initial threshold signifies using defaults
		newCap = DEFAULT_INITIAL_CAPACITY;
		newThr = (int)(DEFAULT_LOAD_FACTOR * DEFAULT_INITIAL_CAPACITY);
	}
	
	if (newThr == 0) {//旧桶为空时
		float ft = (float)newCap * loadFactor;
		//如果新桶容量小于最大容量且阈值也小于最大容量则新阈值等于newCap * loadFactor，否则新阈值等于int类型的最大值（0x7fffffff）
		newThr = (newCap < MAXIMUM_CAPACITY && ft < (float)MAXIMUM_CAPACITY ?
				  (int)ft : Integer.MAX_VALUE);
	}
	//更新阈值
	threshold = newThr;
	@SuppressWarnings({"rawtypes","unchecked"})
	//扩容
		Node<K,V>[] newTab = (Node<K,V>[])new Node[newCap];
	table = newTab;
	if (oldTab != null) {
		//遍历旧桶
		for (int j = 0; j < oldCap; ++j) {
			Node<K,V> e;
			//将旧桶中的数据放入新桶中
			if ((e = oldTab[j]) != null) {
				oldTab[j] = null;
				//如果该结点只有一个键值对
				if (e.next == null)
					//将该键值对插入节点中
					newTab[e.hash & (newCap - 1)] = e;
				//如果该节点时树的实例
				else if (e instanceof TreeNode)
					//将树中的node分离
					((TreeNode<K,V>)e).split(this, newTab, j, oldCap);
				else { // preserve order
					 //如果旧桶中的结构为链表,链表重排，jdk1.8做的一系列优化
					Node<K,V> loHead = null, loTail = null;
					Node<K,V> hiHead = null, hiTail = null;
					Node<K,V> next;
					//遍历整个链表中的节点

					do {
						next = e.next;
						  // 原索引
						if ((e.hash & oldCap) == 0) {
							if (loTail == null)
								loHead = e;
							else
								loTail.next = e;
							loTail = e;
						}
						else {
							// 原索引+oldCap
							if (hiTail == null)
								hiHead = e;
							else
								hiTail.next = e;
							hiTail = e;
						}
					} while ((e = next) != null);
					 // 原索引放到bucket里
					if (loTail != null) {
						loTail.next = null;
						newTab[j] = loHead;
					}
					// 原索引+oldCap放到bucket里
					if (hiTail != null) {
						hiTail.next = null;
						newTab[j + oldCap] = hiHead;
					}
				}
			}
		}
	}
	return newTab;
}
