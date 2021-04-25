# 					k8s集群搭建过程

主要是遵循下面这两篇博客搞的

[Kubernetes:v1.13.4墙内部署文档(CentOS7)](https://www.jianshu.com/p/c54f9742e590)

[记录Kubernetes安装过程](https://github.com/misrn/Deploy-Kubernetes/tree/v1.13.4)

主机：36.155.10.122  kube-master

​			36.155.10.125  kube-node1



安装前准备

- 关闭selinux

```
# vim /etc/sysconfig/selinux 
  SELINUX=disabled
```

- 禁用交换区

  ```
  swapoff -a #否则kubelet无法启动
  ```
  
- 修改内核参数

  至于为什么要修改，我也不是很清楚，我看大家有这一步，有些没得的，我也不晓得他们是怎么搭好的

```
# cat  > /etc/sysctl.d/k8s.conf << EOF
net.bridge.bridge-nf-call-ip6tables = 1
net.bridge.bridge-nf-call-iptables = 1
net.ipv4.ip_forward = 1
EOF


#有些教程没这一步，就会报
#sysctl: cannot stat /proc/sys/net/bridge/bridge-nf-call-ip6tables: No such file or #directory
#sysctl: cannot stat /proc/sys/net/bridge/bridge-nf-call-iptables: No such file or #directory
#然后还得搜一下，这玩意要怎么解决，仿佛在说，年轻人，搭环境这种事情不存顺风顺水的
modprobe br_netfilter 
##如果报 br_netfilter模块找不到错误，走下面的步骤
vi /etc/rc.sysinit #设置开机启动模块
>#!/bin/bash
>for file in /etc/sysconfig/modules/*.modules ; do
>[ -x $file ] && $file
>done

vi /etc/sysconfig/modules/br_netfilter.modules
>modprobe br_netfilter

chmod 755 br_netfilter.modules
#重启
shutdown -r now 

#查看模块
lsmod | grep br_netfilter

sysctl -p /etc/sysctl.d/k8s.conf
```

- 关闭防火墙

```bash
systemctl disable firewalld
systemctl stop firewalld
```

- 更改主机名，不然会冲突

  
  
- 配置yum源

  ```
  wget -O CentOS-Base.repo http://mirrors.aliyun.com/repo/Centos-7.repo（阔以先备份再操作）
  #重新加载yum
  yum clean all
  yum makecache
  yum update
  ```



- 安装docker

配置docker-ce.repo

在/etc/yum.repos.d下操作

```csharp
# 添加可以下载老版本docker的yum源
cat>> /etc/yum.repos.d/docker-main.repo <<EOF
[docker-main]
name=docker-main
baseurl=http://mirrors.aliyun.com/docker-engine/yum/repo/main/centos/7/
gpgcheck=1
enabled=1
gpgkey=http://mirrors.aliyun.com/docker-engine/yum/gpg
EOF
```

阔以选择在线安装，上面有篇教程说的是1.13.0的docker版本跟，1.13.4的kubernetes完美切合的，所以阔以执行以下命令安装

```python
# 清除缓存
yum clean all
# 安装docker 1.13.0
yum -y install docker-engine-1.13.0
```



也阔以选在离线安装，下载docker的rpm包，下载地址如下

https://yum.dockerproject.org/repo/main/centos/7/Packages/

选择

docker-engine-selinux-17.05.0.ce-1.el7.centos.noarch.rpm

docker-engine-1.13.0-1.el7.centos.x86_64.rpm

这两个包，下载下来，copy到服务器上安装即可，但这里我出了点毛病，说我还差点东西，我回头再试试看。

我已经回头试试看了，这么搞没毛病，之前是在主节点上操作的，yum源没更新，差了一堆包，真是日了个狗。

这俩方法都阔以的。

```
yum -y install docker-engine-selinux-17.05.0.ce-1.el7.centos.noarch.rpm 
yum -y install docker-engine-1.13.0-1.el7.centos.x86_64.rpm

#查看docker版本
[root@kube-node1 ~]# docker -v
Docker version 1.13.0, build 49bf474

#设置docker开机启动，启动docker
systemctl enable docker
systemctl start docker
```



- 安装kubeadm

  首先配置kubernetes.repo

  ```
  #　cat > /etc/yum.repos.d/kube.repo << EOF
  [kubernetes]
  name=Kubernetes
  baseurl=https://mirrors.aliyun.com/kubernetes/yum/repos/kubernetes-el7-x86_64/
  enabled=1
  gpgcheck=1
  repo_gpgcheck=0  #等于1会开启gpg校验，可能会校验不通过
  gpgkey=https://mirrors.aliyun.com/kubernetes/yum/doc/yum-key.gpg https://mirrors.aliyun.com/kubernetes/yum/doc/rpm-package-key.gpg
  EOF
  ```

安装kubeadm，在线是不可能在线的，只能自己去下载包来，慢慢安装，还好有大佬。

```
##安装git，新建目录，拉取项目，这里边就有需要用的包，但不全，等会就知道了
yum install git -y && \ 
mkdir /data && \  
cd /data && \
git clone https://github.com/fandaye/Deploy-Kubernetes.git 

cd /data/Deploy-Kubernetes
git checkout v1.13.4  # 切换到v1.13.4分支
```

![1569750473065](C:\Users\Lenovo\AppData\Roaming\Typora\typora-user-images\1569750473065.png)

看嘛，在pkg这个文件夹里，就有需要用到的包，下面来装一装试试

```
 yum -y install kubeadm-1.13.4-0.x86_64.rpm kubectl-1.13.4-0.x86_64.rpm kubelet-1.13.4-0.x86_64.rpm 
 
 #然后就炸了，报错了
Error: Package: kubelet-1.13.4-0.x86_64 (/kubelet-1.13.4-0.x86_64)
           Requires: kubernetes-cni = 0.6.0
Error: Package: kubeadm-1.13.4-0.x86_64 (/kubeadm-1.13.4-0.x86_64)
           Requires: kubernetes-cni >= 0.6.0
Error: Package: kubeadm-1.13.4-0.x86_64 (/kubeadm-1.13.4-0.x86_64)
           Requires: cri-tools >= 1.11.0
说缺少这些个玩意儿，分开装，更能看清楚
```

分开装，就能安装kubectl这个包，不需要其他依赖的。另外还需要去找，我看看，下面这两个包。

```
kubernetes-cni-0.6.0-0.x86_64.rpm
cri-tools-1.12.0.rpm
```

不过还好我找到了kubernetes-cni-0.6.0-0.x86_64.rpm，有大佬分享出来了，回头我贴出来。

下面那个给个链接，https://cbs.centos.org/koji/rpminfo?rpmID=167526

有了这俩包，就能愉快的安装服务了。

```
#这里也是有顺序的，要按照这个顺序来
yum -y install cri-tools-1.12.0.rpm
yum -y install kubernetes-cni-0.6.0-0.x86_64.rpm kubelet-1.13.4-0.x86_64.rpm 
yum -y install kubeadm-1.13.4-0.x86_64.rpm

#安装成功后，阔以设置开机启动kubelet,这时候还不能start kubelet，会报错
systemctl enable kubelet
```



拉取kubernetes镜像

这里搞了两套，都是大佬自己在github上存的镜像，就不用翻墙了

1、shell执行一下下面的代码

```python
images=(kube-proxy:v1.13.4 kube-scheduler:v1.13.4 kube-controller-manager:v1.13.4
kube-apiserver:v1.13.4 etcd:3.2.24 coredns:1.2.6 pause:3.1 )
for imageName in ${images[@]} ; do
docker pull gcr.azk8s.cn/google-containers/$imageName
docker tag gcr.azk8s.cn/google-containers/$imageName k8s.gcr.io/$imageName
docker rmi gcr.azk8s.cn/google-containers/$imageName
done
```

2、保存以下代码

```
echo ""
echo "=========================================================="
echo "Pull Kubernetes v1.13.4 Images from aliyuncs.com ......"
echo "=========================================================="
echo ""
MY_REGISTRY=registry.cn-hangzhou.aliyuncs.com/openthings
*## 拉取镜像*
docker pull ${MY_REGISTRY}/k8s-gcr-io-kube-apiserver:v1.13.4
docker pull ${MY_REGISTRY}/k8s-gcr-io-kube-controller-manager:v1.13.4
docker pull ${MY_REGISTRY}/k8s-gcr-io-kube-scheduler:v1.13.4
docker pull ${MY_REGISTRY}/k8s-gcr-io-kube-proxy:v1.13.4
docker pull ${MY_REGISTRY}/k8s-gcr-io-etcd:3.2.24
docker pull ${MY_REGISTRY}/k8s-gcr-io-pause:3.1
docker pull ${MY_REGISTRY}/k8s-gcr-io-coredns:1.2.6
*## 添加Tag*
docker tag ${MY_REGISTRY}/k8s-gcr-io-kube-apiserver:v1.13.4 k8s.gcr.io/kube-apiserver:v1.13.4
docker tag ${MY_REGISTRY}/k8s-gcr-io-kube-scheduler:v1.13.4 k8s.gcr.io/kube-scheduler:v1.13.4
docker tag ${MY_REGISTRY}/k8s-gcr-io-kube-controller-manager:v1.13.4 k8s.gcr.io/kube-controller-manager:v1.13.4
docker tag ${MY_REGISTRY}/k8s-gcr-io-kube-proxy:v1.13.4 k8s.gcr.io/kube-proxy:v1.13.4
docker tag ${MY_REGISTRY}/k8s-gcr-io-etcd:3.2.24 k8s.gcr.io/etcd:3.2.24
docker tag ${MY_REGISTRY}/k8s-gcr-io-pause:3.1 k8s.gcr.io/pause:3.1
docker tag ${MY_REGISTRY}/k8s-gcr-io-coredns:1.2.6 k8s.gcr.io/coredns:1.2.6
*##删除镜像*
docker rmi ${MY_REGISTRY}/k8s-gcr-io-kube-apiserver:v1.13.4
docker rmi ${MY_REGISTRY}/k8s-gcr-io-kube-controller-manager:v1.13.4
docker rmi ${MY_REGISTRY}/k8s-gcr-io-kube-scheduler:v1.13.4
docker rmi ${MY_REGISTRY}/k8s-gcr-io-kube-proxy:v1.13.4
docker rmi ${MY_REGISTRY}/k8s-gcr-io-etcd:3.2.24
docker rmi ${MY_REGISTRY}/k8s-gcr-io-pause:3.1
docker rmi ${MY_REGISTRY}/k8s-gcr-io-coredns:1.2.6
echo ""
echo "=========================================================="
echo "Pull Kubernetes v1.13.4 Images FINISHED."
echo "into registry.cn-hangzhou.aliyuncs.com/openthings, "
echo "=========================================================="
echo ""
```



- 安装k8s集群

可以使用kudeadm init命令，后面加一堆参数，启动集群，如下：

kubeadm init --apiserver-advertise-address=10.13.0.180 --kubernetes-version=v1.13.4 --pod-network-cidr=10.244.0.0/16 --ignore-preflight-errors=NumCPU（不加这个，如果是单核cpu会报错）



具体参数什么意思，可以去[官网](https://kubernetes.io/docs/reference/setup-tools/kubeadm/kubeadm-init/)看看，讲的很清楚的。

也阔以配置yaml文件，等我找个能用的模板，再贴上来，或者再研究研究。

启动起来以后，是下面这个样子

![1569827608527](C:\Users\Lenovo\AppData\Roaming\Typora\typora-user-images\1569827608527.png)

得执行一下第一个框里的命令，才能正常使用，这个是配置当前用户的环境，不执行会报x509错误。

如果这里，报了509，提示token已过期或者无效，有可能是，主节点和从节点的时间不吻合，执行一下命令

```
#查看时间
date
#校对时间，各节点都进行
ntpdate cn.pool.ntp.org
```



kubectl命令默认从$HOME/.kube/config这个位置读取配置。配置文件中包含apiserver的地址，证书，用户名等你可以cat查看一下。需要做如下配置：

```
mkdir -p $HOME/.kube
cp -i /etc/kubernetes/admin.conf $HOME/.kube/config
chown $(id -u):$(id -g) $HOME/.kube/config
```

这个时候查看master节点的状态，是notready，需要安装flannel的网络插件。方法如下：

```
docker pull registry.cn-hangzhou.aliyuncs.com/gaven_k8s/flannel:v0.11.0-amd64

docker tag registry.cn-hangzhou.aliyuncs.com/gaven_k8s/flannel:v0.11.0-amd64 quay.io/coreos/flannel:v0.11.0-amd64

docker rmi registry.cn-hangzhou.aliyuncs.com/gaven_k8s/flannel:v0.11.0-amd64
##上面拉镜像这一步，从节点也得搞


kubectl apply -f https://raw.githubusercontent.com/coreos/flannel/master/Documentation/kube-flannel.yml
```

主节点从节点都得装，不然到时候想过pod起不起来的，去官网拉镜像是不行的，得翻墙，上面的就是把大佬的镜像拉下来，打tag打成官网的，然后才能顺利启动的。

执行命令，查看pod启动情况

```
kubectl get pod --all-namespaces

[root@xwq-master ~]# kubectl get pod --all-namespaces
NAMESPACE     NAME                                 READY   STATUS    RESTARTS   AGE
kube-system   coredns-86c58d9df4-mdltx             1/1     Running   0          2m20s
kube-system   coredns-86c58d9df4-z7qmt             1/1     Running   0          2m20s
kube-system   etcd-xwq-master                      1/1     Running   0          81s
kube-system   kube-apiserver-xwq-master            1/1     Running   0          87s
kube-system   kube-controller-manager-xwq-master   1/1     Running   0          106s
kube-system   kube-flannel-ds-amd64-7sfn6          1/1     Running   0          78s
kube-system   kube-flannel-ds-amd64-j7rhw          1/1     Running   0          78s
kube-system   kube-proxy-62tsp                     1/1     Running   0          119s
kube-system   kube-proxy-9vp7q                     1/1     Running   0          2m21s
kube-system   kube-scheduler-xwq-master            1/1     Running   0          104s
```

都是running状态，就说明是成功了的，在执行命令，查看节点情况

```
[root@xwq-master ~]# kubectl get nodes
NAME         STATUS   ROLES    AGE     VERSION
xwq-master   Ready    master   3m46s   v1.13.4
xwq-node1    Ready    <none>   3m5s    v1.13.4
```

全部都是Ready状态，说明是搭好了的。

安装dashboard，也得像flannel那也拉镜像，我这里是打包好的镜像，能够直接用，下面这一步从节点也得搞。

```
docker load --input dashboard.tar

#执行docker images | grep dashboard查看dashboard镜像
[root@xwq-master ~]# docker images | grep dashboard
k8s.gcr.io/kubernetes-dashboard-amd64   v1.10.0             0dab2435c100        13 months ago       122 MB

kubectl apply -f dashboard.yml  #这个文件阔以从网上找到，更改一下相关参数

执行一下命令，能够看到dashboard的pod是running状态，所以没毛病
[root@xwq-master ~]# kubectl get pod --all-namespaces | grep dashboard
kube-system   kubernetes-dashboard-79ff88449c-bvsvl   1/1     Running   0          47s
```

设置serviceaccount，用于登陆dashboard

```
kubectl create serviceaccount dashboard-admin -n kube-system
```

创建clusterrolebinding

```
kubectl create clusterrolebinding cluster-dashboard-admin --clusterrole=cluster-admin --serviceaccount=kube-system:dashboard-admin
```

查看生成的secret的token，用于登陆

```
kubectl  get  secret -n kube-system|grep dashboard-admin
[root@xwq-master ~]# kubectl  get  secret -n kube-system|grep dashboard-admin
dashboard-admin-token-vztch                      kubernetes.io/service-account-token   3      55s
#然后执行命令，查看token
kubectl describe secret dashboard-admin-token-vztch -n kube-system
```

登陆dashboard

https://192.168.200.69:30080



查看api-server swagger页面

修改这个文件/etc/kubernetes/manifests/kube-apiserver.yaml

加入以下三个参数，等待api-server重启完成后就能访问了

```
- --enable-swagger-ui=true
- --insecure-bind-address=0.0.0.0
- --insecure-port=8080
```

