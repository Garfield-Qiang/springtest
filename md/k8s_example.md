

根据kuberntes权威指南上的简单示例，搭了一个单机的k8s环境。



1、环境准备，我是在自己的阿里云服务器上操作的，系统是CentOS7，出口ip是47.92.49.105。

​	（1）首先关闭防火墙，不然服务启动后访问不了后面要用的30001端口。

```
	systemctl disable firewalld
	systemctl stop firewalld
```

​		(2) 切换yum源

​				1）备份。

```
	mv /etc/yum.repos.d/CentOS-Base.repo /etc/yum.repos.d/CentOS-Base.repo.backup	
```

​				2）下载新的CentOS-Base.repo 到/etc/yum.repos.d/					

```
	wget -O /etc/yum.repos.d/CentOS-Base.repo [http://mirrors.aliyun.com/repo/Centos-7.repo]
```

​				3) yum makecache

​				参照教程https://yq.aliyun.com/articles/691851?spm=a2c4e.11155472.0.0.3b885de9peb5Vb

​		(3) 安装etcd和kubernetes

```
		yum install -y etcd kubernetes
```

​				然后修改两个配置文件

​				1）Docker配置文件为/etc/sysconfig/docker，将其中的OPTION的内容备份后修改为：

			#OPTIONS='--selinux-enabled --log-driver=journald --signature-verification=false'
			OPTIONS='--selinux-enabled=false --insecure-registry gcr.io'

​				2）修改kubernetes apiserver的配置文件，/etc/kubernetes/apiserver，修改KUBE_ADMISSION_CONTROL如下，去掉ServiceAccount

```
KUBE_ADMISSION_CONTROL="--admission-control=NamespaceLifecycle,NamespaceExists,LimitRanger,SecurityContextDeny,ResourceQuota"
```

​				3）修改docker的镜像源

![1562830645842](C:\Users\Lenovo\AppData\Roaming\Typora\typora-user-images\1562830645842.png)

​		（4）按照顺序启动服务

```
 systemctl start etcd
 systemctl start docker
 systemctl start kube-apiserver
 systemctl start kube-controller-manager
 systemctl start kube-scheduler
 systemctl start kubelet
 systemctl start kube-proxy
```

启动kube-apiserver时，遇到如下问题，

```
[root@Jarvis ~]# systemctl start kube-apiserver
Job for kube-apiserver.service failed because the control process exited with error code. See "systemctl status kube-apiserver.service" and "journalctl -xe" for details.
```

执行systemctl status kube-apiserver.service和journalctl -xe命令后，发现信息中有如下一段。

```
Jul 12 09:29:19 Jarvis kube-apiserver[27570]: F0712 09:29:19.363204   27570 genericapiserver.go:195] failed to listen on 127.0.0.1:8080: listen tcp 127.0.0.1:8080: bind: address already in use
```

再执行netstat -ntlp，发现我已经使用了8080端口，端口被占用了，只能去修改/etc/kubernetes/apiserver，将8080端口改为其他端口即可。

2、启动mysql服务

​	（1）首先定义为mysql定义一个rc文件：mysql-rc.yaml，文件内容如下

```
apiVersion: v1
kind: ReplicationController                            #副本控制器RC
metadata:
  name: mysql                                          #RC的名称，全局唯一
spec:
  replicas: 1                                          #Pod副本的期待数量
  selector:
    app: mysql                                         #符合目标的Pod拥有此标签
  template:                                            #根据此模板创建Pod的副本（实例）
    metadata:
      labels:
        app: mysql                                     #Pod副本拥有的标签，对应RC的Selector
    spec:
      containers:                                      #Pod内容器的定义部分
      - name: mysql                                    #容器的名称
        image: hub.c.163.com/library/mysql              #容器对应的Docker image
        ports: 
        - containerPort: 3306                          #容器应用监听的端口号
        env:                                           #注入容器内的环境变量
        - name: MYSQL_ROOT_PASSWORD 
          value: "123456"
```

kind，表明此资源对象类型，比如这里的值为“ReplicationController”，表示这是一个RC;

spec，定义了一些RC的相关属性，比如spec.selector是RC的Pod标签（Label）选择器，即监控和管理这些标签的Pod的实例，确保当前集群上仅有replicas个Pod实例运行。

创建好rc文件后，通过执行以下命令，将它发布到kubernetes集群中

```
[root@Jarvis ~]# kubectl create -f mysql-rc.yaml
replicationcontroller "mysql" created
```

然后查看刚刚创建的RC

```
[root@Jarvis ~]# kubectl get rc
NAME      DESIRED   CURRENT   READY     AGE
mysql     1         1         1         54s
```

查看Pods的创建情况可以用如下命令

```
[root@Jarvis ~]# kubectl get pods
NAME          READY     STATUS    RESTARTS   AGE
mysql-53jgb   1/1       Running   0          1m
kubectl get pods
```

这里中途也遇到过一个问题，就是pod的状态一直是ContainerCreateing，然后通过kubectl describe pod PodName命令查看错误信息。

```
1m        47s        3    {kubelet 127.0.0.1}            Warning        FailedSync    Error syncing pod, skipping: failed to "StartContainer" for "POD" with ErrImagePull: "image pull failed for registry.access.redhat.com/rhel7/pod-infrastructure:latest, this may be because there are no credentials on this request.  details: (open /etc/docker/certs.d/registry.access.redhat.com/redhat-ca.crt: no such file or directory)"

  1m    10s    4    {kubelet 127.0.0.1}        Warning    FailedSync    Error syncing pod, skipping: failed to "StartContainer" for "POD" with ImagePullBackOff: "Back-off pulling image \"registry.access.redhat.com/rhel7/pod-infrastructure:latest\""
```

发现是需要证书什么的，于是又查资料，说执行yum install *rhsm*就行了，但是我执行了还是没用。然后执行了下面的命令就可以了。

```
yum install -y wget 
wget http://mirror.centos.org/centos/7/os/x86_64/Packages/python-rhsm-certificates-1.19.10-1.el7_4.x86_64.rpm
rpm2cpio python-rhsm-certificates-1.19.10-1.el7_4.x86_64.rpm | cpio -iv --to-stdout ./etc/rhsm/ca/redhat-uep.pem | tee /etc/rhsm/ca/redhat-uep.pem
```

（2）接下来创建与kubernetes Service ---MySQL关联的文件，mysql-svc.yaml

```
apiVersion: v1                      
kind: Service                              #表明是K8s Service
metadata: 
  name: mysql                              #Service的全局唯一名称
spec:
  ports:
    - port: 3306                           #Service提供服务的端口号
  selector:                                #Service对应的Pod拥有这里定义的标签
    app: mysql
```

metadata.name是Service的服务名；

port属性则是定义了Service的虚端口；

spec.selector确定了哪些Pod副本对应到本服务。

通过kubectl create 命令创建Service对象，通过kubectl get svc 查询service

```
[root@Jarvis ~]# kubectl create -f mysql-svc.yaml 
service "mysql" created
[root@Jarvis ~]# kubectl get svc
NAME         CLUSTER-IP       EXTERNAL-IP   PORT(S)          AGE
kubernetes   10.254.0.1       <none>        443/TCP          1d
mysql        10.254.187.229   <none>        3306/TCP         8s
```



3、启动tomcat应用

先拉取一个测试镜像到本地

```
docker pull kubeguide/tomcat-app:v1
```

定义rc文件，myweb-rc.yaml

```
apiVersion: v1
kind: ReplicationController
metadata:
  name: myweb
spec:
  replicas: 5                                       #Pod副本期待数量为5
  selector:
    app: myweb
  template:
    metadata:
      labels:
        app: myweb
    spec:
      containers:
      - name: myweb
        image: docker.io/kubeguide/tomcat-app:v1
        ports: 
        - containerPort: 8080
        env:
        - name: MYSQL_SERVICE_HOST
          value: "mysql"
        - name: MYSQL_SERVICE_PORT
          value: "3306"
```

定义关联的service文件，myweb-svc.yaml

```
apiVersion: v1
kind: Service
metadata: 
  name: myweb
spec:
  type: NodePort
  ports:
    - port: 8080
      nodePort: 30001
  selector:
    app: myweb
```

同上，用相同的命令可以创建，查看pod.



然后访问47.92.49.105.30001/demo，可以发现服务启动起来了

![1562900839131](C:\Users\Lenovo\AppData\Roaming\Typora\typora-user-images\1562900839131.png)

这里也遇到过一个问题，关掉防火墙之后，还是不能访问，后来使用如下命令解决

```
iptables -P FORWARD ACCEPT  
```

