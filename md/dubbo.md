



1、init

- protocol.dubbo.initConsumerConfig 

主要是根据配置文件初始化消费者端的配置供config.Load()方法配置消费者部分功能

```
func init() {
	// load clientconfig from consumer_config
	// default use dubbo
	consumerConfig := config.GetConsumerConfig()
	...
	if protocolConf == nil {
		logger.Info("protocol_conf default use dubbo config")
	} else {
	...
		dubboConfByte, err := yaml.Marshal(dubboConf)
	...
		err = yaml.Unmarshal(dubboConfByte, &defaultClientConfig)
	...
	}
	...
}
```

- cluster_impl.*_cluster.init、loadbalance.*_loadbalance.init、filter_impl.*_filter.init

初始化各种cluster容错策略，负载均衡策略，过滤器

cluster

```
//遍历所有的Invokers判断invoker.isAvalible,只要一个有为true直接调用返回，否则就抛出异常
available_cluster.init    clusters["available"]=NewAvailableCluster

//广播调用所有提供者，逐个调用，任意一台报错则报错,通常用于通知所有提供者更新缓存或日志等本地资源信息
broad_cluster.init  clusters["braod"]=NewBroadCluster

//失败自动恢复,后台记录失败请求,定时重发通知,用于消息通知
failback_cluster.init clusters["failback"]=NewFailbackCluster

//快速失败,失败一次,立即报错,非幂等性的写操作，比如新增记录
failfast_cluster.init clusters["failfast"]=NewFailfastCluster

//失败自动切换，当出现失败，重试其它服务器,通常用于读操作，但重试会带来更长延迟
failover_cluster.init clusters["failover"]=NewFailoverCluster

//失败安全，出现异常时，直接忽略,通常用于写入审计日志等操作
failsafe_cluster.init clusters["failsafe"]=NewFailsafeCluster

//并行调用多个服务器，只要一个成功即返回,通常用于实时性要求较高的读操作，但需要浪费更多服务资源
forking_cluster.init clusters["forking"]=NewForkingCluster
```

loadbalance

```
//一致性哈希算法,相同参数的请求总是发到同一提供者
consistent_hash.init  loadbalances["consistenthash"] = NewConsistentHashLoadBalance

//最小活跃数负载均衡
least_active.init  loadbalances["leastactive"] = NewLeastActiveLoadBalance

//随机，按权重设置随机概率 
random.init  loadbalances["random"] = NewRandomLoadBalance

//根据权重进轮训，轮训的缺点就是无法顾及invoker的执行效率，有可能将请求积压在某一处理较慢的provider上
roundrobin.init  loadbalances["roundrobin"] = NewRoundRobinLoadBalance
```

filter

```
...
```

- registry.init （我是用的注册中心是zk，就要引入zk的包）

- registry.protocol.init



2、config.load()

这个方法主要有两个关键地方，加载consumer config和provider config

consumer config加载

```
{
	...
	checkApplicationName(consumerConfig.ApplicationConfig)//检查应用配置
	checkRegistries(consumerConfig.Registries, consumerConfig.Registry)//检查注册中心配置
	
    for key, ref := range consumerConfig.References {
        if ref.Generic {
        genericService := NewGenericService(key)
        SetConsumerService(genericService)
        }
        rpcService := GetConsumerService(key)
        if rpcService == nil {
        logger.Warnf("%s does not exist!", key)
        continue
        }
        ref.id = key
        ref.Refer(rpcService)
        ref.Implement(rpcService)
    }
    
    //wait for invoker is available, if wait over default 3s, then panic
		var count int

        for _, refconfig := range consumerConfig.References {
            ...
            if refconfig.invoker != nil &&
                !refconfig.invoker.IsAvailable() {
                checkok = false
                ...
            }
		...
        }
			

}
```

这一段的重点是服务引用的过程，主要体现在这两个方法上。

- reference.refer(rpcService)
- reference.impliment(rpcService)



```
func (c *ReferenceConfig) Refer(_ interface{}) {
	cfgURL := common.NewURLWithOptions(
		common.WithPath(c.id),
		common.WithProtocol(c.Protocol),
		common.WithParams(c.getUrlMap()),
		common.WithParamsValue(constant.BEAN_NAME_KEY, c.id),
	)

	if c.Url != "" {
		// 1. user specified URL, could be peer-to-peer address, or register center's address.
		urlStrings := gxstrings.RegSplit(c.Url, "\\s*[;]+\\s*")
		for _, urlStr := range urlStrings {
			serviceUrl, err := common.NewURL(urlStr)
			if err != nil {
				panic(fmt.Sprintf("user specified URL %v refer error, error message is %v ", urlStr, err.Error()))
			}
			if serviceUrl.Protocol == constant.REGISTRY_PROTOCOL {
				serviceUrl.SubURL = cfgURL
				c.urls = append(c.urls, &serviceUrl)
			} else {
				if serviceUrl.Path == "" {
					serviceUrl.Path = "/" + c.id
				}
				// merge url need to do
				newUrl := common.MergeUrl(&serviceUrl, cfgURL)
				c.urls = append(c.urls, newUrl)
			}
		}
	} else {
		// 2. assemble SubURL from register center's configuration mode
		c.urls = loadRegistries(c.Registry, consumerConfig.Registries, common.CONSUMER)

		// set url to regUrls
		for _, regUrl := range c.urls {
			regUrl.SubURL = cfgURL
		}
	}
	//上面这部分都在初始化referenceConfig.urls，
	
	
	//这里才是关键
	if len(c.urls) == 1 {
		c.invoker = extension.GetProtocol(c.urls[0].Protocol).Refer(*c.urls[0])
	} else {
		invokers := make([]protocol.Invoker, 0, len(c.urls))
		var regUrl *common.URL
		for _, u := range c.urls {
			invokers = append(invokers, extension.GetProtocol(u.Protocol).Refer(*u))
			if u.Protocol == constant.REGISTRY_PROTOCOL {
				regUrl = u
			}
		}
		if regUrl != nil {
			cluster := extension.GetCluster("registryAware")
			c.invoker = cluster.Join(directory.NewStaticDirectory(invokers))
		} else {
			cluster := extension.GetCluster(c.Cluster)
			c.invoker = cluster.Join(directory.NewStaticDirectory(invokers))
		}
	}

	// create proxy
	if c.Async {
		callback := GetCallback(c.id)
		c.pxy = extension.GetProxyFactory(consumerConfig.ProxyFactory).GetAsyncProxy(c.invoker, callback, cfgURL)
	} else {
		c.pxy = extension.GetProxyFactory(consumerConfig.ProxyFactory).GetProxy(c.invoker, cfgURL)
	}
}
```

referenceConfig.Refer方法种，重点方法是extension.GetProtocol(c.urls[0].Protocol).Refer(*c.urls[0])

查看registryProtocl.Refer方法

```
func (proto *registryProtocol) Refer(url common.URL) protocol.Invoker {
	var registryUrl = url
	var serviceUrl = registryUrl.SubURL
	if registryUrl.Protocol == constant.REGISTRY_PROTOCOL {
		protocol := registryUrl.GetParam(constant.REGISTRY_KEY, "")
		registryUrl.Protocol = protocol
	}

	var reg registry.Registry

	if regI, loaded := proto.registries.Load(registryUrl.Key()); !loaded {
		reg = getRegistry(&registryUrl)
		proto.registries.Store(registryUrl.Key(), reg)
	} else {
		reg = regI.(registry.Registry)
	}

	//new registry directory for store service url from registry
	// 创建directory
	directory, err := directory2.NewRegistryDirectory(&registryUrl, reg)
	if err != nil {
		logger.Errorf("consumer service %v  create registry directory  error, error message is %s, and will return nil invoker!",
			serviceUrl.String(), err.Error())
		return nil
	}
	// 注册到注册中心
	err = reg.Register(*serviceUrl)
	if err != nil {
		logger.Errorf("consumer service %v register registry %v error, error message is %s",
			serviceUrl.String(), registryUrl.String(), err.Error())
	}
	//订阅注册中心服务
	go directory.Subscribe(serviceUrl)

	//new cluster invoker
	
	cluster := extension.GetCluster(serviceUrl.GetParam(constant.CLUSTER_KEY, constant.DEFAULT_CLUSTER))
	//将订阅到的url在RegistryDirectory内部转换成Invoker
	invoker := cluster.Join(directory)
	proto.invokers = append(proto.invokers, invoker)
	return invoker
}
```

回到referenceConfig.Refer，

```
	// create proxy
	if c.Async {
		callback := GetCallback(c.id)
		c.pxy = extension.GetProxyFactory(consumerConfig.ProxyFactory).GetAsyncProxy(c.invoker, callback, cfgURL)
	} else {
		c.pxy = extension.GetProxyFactory(consumerConfig.ProxyFactory).GetProxy(c.invoker, cfgURL)
	}
这里的GetProxy是为invoker创建一个代理，调用的就是如下函数，返回一个新的proxy

func (factory *DefaultProxyFactory) GetAsyncProxy(invoker protocol.Invoker, callBack interface{}, url *common.URL) *proxy.Proxy {
	//create proxy
	attachments := map[string]string{}
	attachments[constant.ASYNC_KEY] = url.GetParam(constant.ASYNC_KEY, "false")
	return proxy.NewProxy(invoker, callBack, attachments)
}
```



referenceConfig.Implement

```
//这里还是调用的，proxy.Implement方法
func (c *ReferenceConfig) Implement(v common.RPCService) {
	c.pxy.Implement(v)
}

//查看具体的Implment方法
//消费端的provider，都是实现了Reference方法的，表示实现了common.RPCService，所以provider都可以看成
//是一个RPCService
//这个方法的主要作用，是通过golang的反射机制，通过装饰器模式，实现provider里定义的方法
func (p *Proxy) Implement(v common.RPCService) {

	// check parameters, incoming interface must be a elem's pointer.
	valueOf := reflect.ValueOf(v)
	logger.Debugf("[Implement] reflect.TypeOf: %s", valueOf.String())

	valueOfElem := valueOf.Elem()
	typeOf := valueOfElem.Type()

	// check incoming interface, incoming interface's elem must be a struct.
	if typeOf.Kind() != reflect.Struct {
		logger.Errorf("%s must be a struct ptr", valueOf.String())
		return
	}

//装饰器模式，具体调用通过invoker实现，调用前后是处理入参和出参的逻辑
	makeDubboCallProxy := func(methodName string, outs []reflect.Type) func(in []reflect.Value) []reflect.Value {
		return func(in []reflect.Value) []reflect.Value {
			var (
				err    error
				inv    *invocation_impl.RPCInvocation
				inIArr []interface{}
				inVArr []reflect.Value
				reply  reflect.Value
			)
			if methodName == "Echo" {
				methodName = "$echo"
			}

			if len(outs) == 2 {
				if outs[0].Kind() == reflect.Ptr {
					reply = reflect.New(outs[0].Elem())
				} else {
					reply = reflect.New(outs[0])
				}
			} else {
				reply = valueOf
			}

			start := 0
			end := len(in)
			invCtx := context.Background()
			if end > 0 {
				if in[0].Type().String() == "context.Context" {
					if !in[0].IsNil() {
						// the user declared context as method's parameter
						invCtx = in[0].Interface().(context.Context)
					}
					start += 1
				}
				if len(outs) == 1 && in[end-1].Type().Kind() == reflect.Ptr {
					end -= 1
					reply = in[len(in)-1]
				}
			}

			if end-start <= 0 {
				inIArr = []interface{}{}
				inVArr = []reflect.Value{}
			} else if v, ok := in[start].Interface().([]interface{}); ok && end-start == 1 {
				inIArr = v
				inVArr = []reflect.Value{in[start]}
			} else {
				inIArr = make([]interface{}, end-start)
				inVArr = make([]reflect.Value, end-start)
				index := 0
				for i := start; i < end; i++ {
					inIArr[index] = in[i].Interface()
					inVArr[index] = in[i]
					index++
				}
			}

			inv = invocation_impl.NewRPCInvocationWithOptions(invocation_impl.WithMethodName(methodName),
				invocation_impl.WithArguments(inIArr), invocation_impl.WithReply(reply.Interface()),
				invocation_impl.WithCallBack(p.callBack), invocation_impl.WithParameterValues(inVArr))

			for k, value := range p.attachments {
				inv.SetAttachments(k, value)
			}

			// add user setAttachment
			atm := invCtx.Value("attachment")
			if m, ok := atm.(map[string]string); ok {
				for k, value := range m {
					inv.SetAttachments(k, value)
				}
			}

			result := p.invoke.Invoke(invCtx, inv)

			err = result.Error()
			logger.Debugf("[makeDubboCallProxy] result: %v, err: %v", result.Result(), err)
			if len(outs) == 1 {
				return []reflect.Value{reflect.ValueOf(&err).Elem()}
			}
			if len(outs) == 2 && outs[0].Kind() != reflect.Ptr {
				return []reflect.Value{reply.Elem(), reflect.ValueOf(&err).Elem()}
			}
			return []reflect.Value{reply, reflect.ValueOf(&err).Elem()}
		}
	}

	numField := valueOfElem.NumField()
	//循环遍历provider里定义的func，为每个func通过上面的makeDubboCallProxy来指定一个实现
	for i := 0; i < numField; i++ {
		t := typeOf.Field(i)
		methodName := t.Tag.Get("dubbo")
		if methodName == "" {
			methodName = t.Name
		}
		f := valueOfElem.Field(i)
		if f.Kind() == reflect.Func && f.IsValid() && f.CanSet() {
			outNum := t.Type.NumOut()

			if outNum != 1 && outNum != 2 {
				logger.Warnf("method %s of mtype %v has wrong number of in out parameters %d; needs exactly 1/2",
					t.Name, t.Type.String(), outNum)
				continue
			}

			// The latest return type of the method must be error.
			if returnType := t.Type.Out(outNum - 1); returnType != typError {
				logger.Warnf("the latest return type %s of method %q is not error", returnType, t.Name)
				continue
			}

			var funcOuts = make([]reflect.Type, outNum)
			for i := 0; i < outNum; i++ {
				funcOuts[i] = t.Type.Out(i)
			}

			// do method proxy here:
			f.Set(reflect.MakeFunc(f.Type(), makeDubboCallProxy(methodName, funcOuts)))
			logger.Debugf("set method [%s]", methodName)
		}
	}

	p.once.Do(func() {
		p.rpc = v
	})

}
```

