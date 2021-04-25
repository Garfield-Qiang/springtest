1、下载安装包

```
cd /data/
curl -O https://fastdl.mongodb.org/linux/mongodb-linux-x86_64-4.0.4.tgz
tar zxvf mongodb-linux-x86_64-4.0.4.tgz
```

2、创建相关目录

```
mv mongodb-linux-x86_64-4.0.4 mongodb
mkdir -p mongodb/{data/db,log}
mkdir -p /etc/mongodb

#生成keyFile
openssl rand -base64 741 > mongodb.key
chmod 600 mongodb.key
#权限未修改为 600 的报错如下输出：
#ACCESS   permissions on xxx are too open
```

3、创建配置文件

```
vim /etc/mongodb/mgdb.conf

dbpath=/data/mongodb/data/db  #数据文件存放目录
logpath=/data/mongodb/log/mongodb.log  #日志文件存放目录
port=27017  #端口，默认27017，可以自定义
logappend=true  #开启日志追加添加日志
fork=true  #以守护程序的方式启用，即在后台运行
bind_ip=0.0.0.0  #本地监听IP，0.0.0.0表示本地所有IP
auth=true  #是否需要验证权限登录(用户名和密码)
replSet=rsmongo #制定副本集名称
keyFile=/data/mongodb.key  # 集群的私钥的完整路径，只对于Replica Set 架构有效

```

具体配置文件信息，可以参考

https://docs.mongodb.com/manual/reference/configuration-options/

4、配置服务

​	4.1 添加环境变量

```
vim /etc/profile

	export MONGODB_HOME=/data/mongodb
	export PATH=$PATH:$MONGODB_HOME/bin
	
source /etc/profile
```

​	4.2 创建启动配置文件

```
vim /usr/lib/systemd/system/mongodb.service

    [Unit]
    Description=mongodb
    After=network.target remote-fs.target nss-lookup.target

    [Service]
    Type=forking
    RuntimeDirectory=mongodb
    PIDFile=/data/mongodb/data/db/mongod.lock
    ExecStart=/data/mongodb/bin/mongod --config /etc/mongodb/mgdb.conf
    ExecStop=/data/mongodb/bin/mongod --shutdown --config /etc/mongodb/mgdb.conf
    PrivateTmp=true

    [Install]  
    WantedBy=multi-user.target
```

​	4.3 配置服务自启

```
systemctl daemon-reload
systemctl start mongodb
systemctl enable mongodb
```

5、启动服务

```
mongo --port ${port}  #config文件里配置的port，如果是默认的则不需要--port参数
```

6、配置副本集

```
#完成副本集初始化配置
> config = {_id:"rsmongo",members:[]}
{ "_id" : "rsmongo", "members" : [ ] }
> config.members.push({_id:0,host:"10.13.0.182:27017",priority:3})
1
> config.members.push({_id:1,host:"10.13.0.184:27017",priority:2})
2
> config.members.push({_id:2,host:"10.13.0.185:27017",priority:1})
3
> rs.initiate(config)
{ "ok" : 1 }


rsmongo:SECONDARY>  #一直回车
rsmongo:PRIMARY>   #切换到主节点
rsmongo:PRIMARY> use admin
```

member详细说明

**priority**：表示一个成员被选举为Primary节点的优先级，默认值是1，取值范围是从0到100，将priority设置为0有特殊含义：Priority为0的成员永远不能成为Primary 节点。Replica Set中，Priority最高的成员，会优先被选举为Primary 节点，只要其满足条件。

**hidden**：将成员配置为隐藏成员，要求Priority 为0。Client不会向隐藏成员发送请求，因此隐藏成员不会收到Client的Request。

**slaveDelay**：单位是秒，将Secondary 成员配置为延迟备份节点，要求Priority 为0，表示该成员比Primary 成员滞后指定的时间，才能将Primary上进行的写操作同步到本地。为了数据读取的一致性，应将延迟备份节点的hidden设置为true，避免用户读取到明显滞后的数据。Delayed members maintain a copy of the data that reflects the state of the data at some time in the past.

**votes**：有效值是0或1，默认值是1，如果votes是1，表示该成员（voting member）有权限选举Primary 成员。在一个Replica Set中，最多有7个成员，其votes 属性的值是1。

**arbiterOnly**：表示该成员是仲裁者，arbiter的唯一作用是就是参与选举，其votes属性是1，arbiter不保存数据，也不会为client提供服务。

**buildIndexes**：表示实在在成员上创建Index，该属性不能修改，只能在增加成员时设置该属性。如果一个成员仅仅作为备份，不接收Client的请求，将该成员设置为不创建index，能够提高数据同步的效率。



查看集群状态

```
rsmongo:PRIMARY> rs.status()
{
	"set" : "rsmongo",
	"date" : ISODate("2020-06-04T02:00:28.816Z"),
	"myState" : 1,
	"term" : NumberLong(1),
	"syncingTo" : "",
	"syncSourceHost" : "",
	"syncSourceId" : -1,
	"heartbeatIntervalMillis" : NumberLong(2000),
	"optimes" : {
		"lastCommittedOpTime" : {
			"ts" : Timestamp(1591236021, 1),
			"t" : NumberLong(1)
		},
		"readConcernMajorityOpTime" : {
			"ts" : Timestamp(1591236021, 1),
			"t" : NumberLong(1)
		},
		"appliedOpTime" : {
			"ts" : Timestamp(1591236021, 1),
			"t" : NumberLong(1)
		},
		"durableOpTime" : {
			"ts" : Timestamp(1591236021, 1),
			"t" : NumberLong(1)
		}
	},
	"lastStableCheckpointTimestamp" : Timestamp(1591236001, 3),
	"members" : [
		{
			"_id" : 0,
			"name" : "10.13.0.182:27017",
			"health" : 1,
			"state" : 1,
			"stateStr" : "PRIMARY",
			"uptime" : 629,
			"optime" : {
				"ts" : Timestamp(1591236021, 1),
				"t" : NumberLong(1)
			},
			"optimeDate" : ISODate("2020-06-04T02:00:21Z"),
			"syncingTo" : "",
			"syncSourceHost" : "",
			"syncSourceId" : -1,
			"infoMessage" : "could not find member to sync from",
			"electionTime" : Timestamp(1591235940, 1),
			"electionDate" : ISODate("2020-06-04T01:59:00Z"),
			"configVersion" : 1,
			"self" : true,
			"lastHeartbeatMessage" : ""
		},
		{
			"_id" : 1,
			"name" : "10.13.0.184:27017",
			"health" : 1,
			"state" : 2,
			"stateStr" : "SECONDARY",
			"uptime" : 99,
			"optime" : {
				"ts" : Timestamp(1591236021, 1),
				"t" : NumberLong(1)
			},
			"optimeDurable" : {
				"ts" : Timestamp(1591236021, 1),
				"t" : NumberLong(1)
			},
			"optimeDate" : ISODate("2020-06-04T02:00:21Z"),
			"optimeDurableDate" : ISODate("2020-06-04T02:00:21Z"),
			"lastHeartbeat" : ISODate("2020-06-04T02:00:28.742Z"),
			"lastHeartbeatRecv" : ISODate("2020-06-04T02:00:26.911Z"),
			"pingMs" : NumberLong(0),
			"lastHeartbeatMessage" : "",
			"syncingTo" : "10.13.0.182:27017",
			"syncSourceHost" : "10.13.0.182:27017",
			"syncSourceId" : 0,
			"infoMessage" : "",
			"configVersion" : 1
		},
		{
			"_id" : 2,
			"name" : "10.13.0.185:27017",
			"health" : 1,
			"state" : 2,
			"stateStr" : "SECONDARY",
			"uptime" : 99,
			"optime" : {
				"ts" : Timestamp(1591236021, 1),
				"t" : NumberLong(1)
			},
			"optimeDurable" : {
				"ts" : Timestamp(1591236021, 1),
				"t" : NumberLong(1)
			},
			"optimeDate" : ISODate("2020-06-04T02:00:21Z"),
			"optimeDurableDate" : ISODate("2020-06-04T02:00:21Z"),
			"lastHeartbeat" : ISODate("2020-06-04T02:00:28.742Z"),
			"lastHeartbeatRecv" : ISODate("2020-06-04T02:00:26.911Z"),
			"pingMs" : NumberLong(0),
			"lastHeartbeatMessage" : "",
			"syncingTo" : "10.13.0.182:27017",
			"syncSourceHost" : "10.13.0.182:27017",
			"syncSourceId" : 0,
			"infoMessage" : "",
			"configVersion" : 1
		}
	],
	"ok" : 1,
	"operationTime" : Timestamp(1591236021, 1),
	"$clusterTime" : {
		"clusterTime" : Timestamp(1591236021, 1),
		"signature" : {
			"hash" : BinData(0,"n8we9wt5XmTS7HVv7Euwlrhk4no="),
			"keyId" : NumberLong("6834306326814785538")
		}
	}
}

```

添加用户

```
switched to db admin
#添加用户
rsmongo:PRIMARY> db.createUser({user:"root",pwd:"root",roles:[{role:"root",db:"admin"}]})
Successfully added user: {
	"user" : "root",
	"roles" : [
		{
			"role" : "root",
			"db" : "admin"
		}
	]
}
rsmongo:PRIMARY> db.auth("root","root")
1
```

测试操作主节点数据

```
rsmongo:PRIMARY> show dbs
admin   0.000GB
config  0.000GB
local   0.000GB
rsmongo:PRIMARY> use testdb
switched to db testdb
rsmongo:PRIMARY> db.testCollection.insertOne({"x":"1"})
{
	"acknowledged" : true,
	"insertedId" : ObjectId("5ed85648b24d2fb976797c8a")
}
```

在副本节点上查看数据

```
rsmongo:SECONDARY> rs.slaveOk()
rsmongo:SECONDARY> use admin
switched to db admin
rsmongo:SECONDARY> db.auth("root","root")
1
rsmongo:SECONDARY> show dbs
admin   0.000GB
config  0.000GB
local   0.000GB
testdb  0.000GB #主节点新增的db
rsmongo:SECONDARY> use testdb
switched to db testdb
rsmongo:SECONDARY> show collections
testCollection #主节点新增的collection
rsmongo:SECONDARY> db.testCollection.find()
{ "_id" : ObjectId("5ed85648b24d2fb976797c8a"), "x" : "1" } #主节点新增的col

```

7、添加Secondary节点

```
rsmongo:PRIMARY> rs.add("10.13.0.187:27017")
{
	"ok" : 1,
	"operationTime" : Timestamp(1591239242, 1),
	"$clusterTime" : {
		"clusterTime" : Timestamp(1591239242, 1),
		"signature" : {
			"hash" : BinData(0,"XTrLQEpoalSHDWa744pUpD2ONgw="),
			"keyId" : NumberLong("6834306326814785538")
		}
	}
}

rsmongo:PRIMARY> rs.status()

{
			"_id" : 4,
			"name" : "10.13.0.187:27017",
			"health" : 0,
			"state" : 8,
			"stateStr" : "(not reachable/healthy)",
			"uptime" : 0,
			"optime" : {
				"ts" : Timestamp(0, 0),
				"t" : NumberLong(-1)
			},
			"optimeDurable" : {
				"ts" : Timestamp(0, 0),
				"t" : NumberLong(-1)
			},
			"optimeDate" : ISODate("1970-01-01T00:00:00Z"),
			"optimeDurableDate" : ISODate("1970-01-01T00:00:00Z"),
			"lastHeartbeat" : ISODate("2020-06-04T02:54:12.837Z"),
			"lastHeartbeatRecv" : ISODate("1970-01-01T00:00:00Z"),
			"pingMs" : NumberLong(0),
			"lastHeartbeatMessage" : "no response within election timeout period",
			"syncingTo" : "",
			"syncSourceHost" : "",
			"syncSourceId" : -1,
			"infoMessage" : "",
			"configVersion" : -1
		}
###上面是新加入的节点未启动mongo服务的状态，如下，启动服务以后，节点状态正常
		{
			"_id" : 4,
			"name" : "10.13.0.187:27017",
			"health" : 1,
			"state" : 2,
			"stateStr" : "SECONDARY",
			"uptime" : 2,
			"optime" : {
				"ts" : Timestamp(1591239651, 1),
				"t" : NumberLong(1)
			},
			"optimeDurable" : {
				"ts" : Timestamp(1591239651, 1),
				"t" : NumberLong(1)
			},
			"optimeDate" : ISODate("2020-06-04T03:00:51Z"),
			"optimeDurableDate" : ISODate("2020-06-04T03:00:51Z"),
			"lastHeartbeat" : ISODate("2020-06-04T03:01:00.620Z"),
			"lastHeartbeatRecv" : ISODate("2020-06-04T03:01:00.462Z"),
			"pingMs" : NumberLong(1),
			"lastHeartbeatMessage" : "",
			"syncingTo" : "mongo-slave-3:27017",
			"syncSourceHost" : "mongo-slave-3:27017",
			"syncSourceId" : 3,
			"infoMessage" : "",
			"configVersion" : 3
		}

```

health 为1表示server正常，0表示server宕.

state 为1表明Primary，2表明secondary，3表示Recovering，7表示Arbiter，8表示Down.

此时新加入的节点，并未启动服务，所以health为0，state为8

当 10.13.0.187:27017 节点启动了mongodb服务后，重新执行rs.status()，可以发现，节点状态已经更新



8、移除 Secondary节点

​	

```
rsmongo:PRIMARY> rs.isMaster()
{
	"hosts" : [
		"10.13.0.182:27017",
		"10.13.0.184:27017",
		"10.13.0.185:27017",
		"mongo-slave-3:27017",
		"10.13.0.187:27017"
	],
	"setName" : "rsmongo",
	"setVersion" : 3,
	"ismaster" : true,
	"secondary" : false,
	"primary" : "10.13.0.182:27017",
	"me" : "10.13.0.182:27017",
	"electionId" : ObjectId("7fffffff0000000000000001"),
	"lastWrite" : {
		"opTime" : {
			"ts" : Timestamp(1591239861, 1),
			"t" : NumberLong(1)
		},
		"lastWriteDate" : ISODate("2020-06-04T03:04:21Z"),
		"majorityOpTime" : {
			"ts" : Timestamp(1591239861, 1),
			"t" : NumberLong(1)
		},
		"majorityWriteDate" : ISODate("2020-06-04T03:04:21Z")
	},
	"maxBsonObjectSize" : 16777216,
	"maxMessageSizeBytes" : 48000000,
	"maxWriteBatchSize" : 100000,
	"localTime" : ISODate("2020-06-04T03:04:26.414Z"),
	"logicalSessionTimeoutMinutes" : 30,
	"minWireVersion" : 0,
	"maxWireVersion" : 7,
	"readOnly" : false,
	"ok" : 1,
	"operationTime" : Timestamp(1591239861, 1),
	"$clusterTime" : {
		"clusterTime" : Timestamp(1591239861, 1),
		"signature" : {
			"hash" : BinData(0,"GDdadA1Hif1V7fAWVLTbMFm89Hk="),
			"keyId" : NumberLong("6834306326814785538")
		}
	}
}
#host里可以看到，当前集群有5个节点，下面移除一个节点
rsmongo:PRIMARY> rs.remove("mongo-slave-3:27017")
{
	"ok" : 1,
	"operationTime" : Timestamp(1591240161, 1),
	"$clusterTime" : {
		"clusterTime" : Timestamp(1591240161, 1),
		"signature" : {
			"hash" : BinData(0,"jf4upG7RjV34WIjJ3jU6xF5tCNA="),
			"keyId" : NumberLong("6834306326814785538")
		}
	}
}

```

移除mongo-slave-3节点以后，再回到mongo-slave-3节点，可以看到节点已经是如下状态

​	

```
rsmongo:OTHER> rs.status()
{
	"operationTime" : Timestamp(1591241001, 1),
	"ok" : 0,
	"errmsg" : "Our replica set config is invalid or we are not a member of it",
	"code" : 93,
	"codeName" : "InvalidReplicaSetConfig",
	"$clusterTime" : {
		"clusterTime" : Timestamp(1591241001, 1),
		"signature" : {
			"hash" : BinData(0,"HHzOTs2eNzOWmFOMmO1kVvk9cvQ="),
			"keyId" : NumberLong("6834306326814785538")
		}
	}
}

rsmongo:OTHER> db.testCollection.find()
Error: error: {
	"operationTime" : Timestamp(1591240541, 1),
	"ok" : 0,
	"errmsg" : "node is not in primary or recovering state",
	"code" : 13436,
	"codeName" : "NotMasterOrSecondary",
	"$clusterTime" : {
		"clusterTime" : Timestamp(1591240541, 1),
		"signature" : {
			"hash" : BinData(0,"lGef4HTovxWhwQ9oLSukxnrII+k="),
			"keyId" : NumberLong("6834306326814785538")
		}
	}
}
```

由之前的Secondary变为OTHER，这时候的节点是无法读写，做副本集

这是因为local.system.replset下面还保存有副本集的信息，当前节点没有查看权限

可以进primary查看

```
#OTHER节点是无法查看的
rsmongo:OTHER> db.config.system.replset.find()
Error: error: {
	"operationTime" : Timestamp(1591243251, 1),
	"ok" : 0,
	"errmsg" : "node is not in primary or recovering state",
	"code" : 13436,
	"codeName" : "NotMasterOrSecondary",
	"$clusterTime" : {
		"clusterTime" : Timestamp(1591243251, 1),
		"signature" : {
			"hash" : BinData(0,"pLsbcLBuBS7FKj8jNMShJKhaq9c="),
			"keyId" : NumberLong("6834306326814785538")
		}
	}
}
#PRIMARY可以
rsmongo:PRIMARY> db.system.replset.find()
{ "_id" : "rsmongo", "version" : 4, "protocolVersion" : NumberLong(1), "writeConcernMajorityJournalDefault" : true, "members" : [ { "_id" : 0, "host" : "10.13.0.182:27017", "arbiterOnly" : false, "buildIndexes" : true, "hidden" : false, "priority" : 3, "tags" : {  }, "slaveDelay" : NumberLong(0), "votes" : 1 }, { "_id" : 1, "host" : "10.13.0.184:27017", "arbiterOnly" : false, "buildIndexes" : true, "hidden" : false, "priority" : 2, "tags" : {  }, "slaveDelay" : NumberLong(0), "votes" : 1 }, { "_id" : 2, "host" : "10.13.0.185:27017", "arbiterOnly" : false, "buildIndexes" : true, "hidden" : false, "priority" : 1, "tags" : {  }, "slaveDelay" : NumberLong(0), "votes" : 1 }, { "_id" : 4, "host" : "10.13.0.187:27017", "arbiterOnly" : false, "buildIndexes" : true, "hidden" : false, "priority" : 1, "tags" : {  }, "slaveDelay" : NumberLong(0), "votes" : 1 } ], "settings" : { "chainingAllowed" : true, "heartbeatIntervalMillis" : 2000, "heartbeatTimeoutSecs" : 10, "electionTimeoutMillis" : 10000, "catchUpTimeoutMillis" : -1, "catchUpTakeoverDelayMillis" : 30000, "getLastErrorModes" : {  }, "getLastErrorDefaults" : { "w" : 1, "wtimeout" : 0 }, "replicaSetId" : ObjectId("5ed85559a89a27f7cce7194e") } }
```

但是在local系统库下面的  system.replset  就连root权限的用户都无法删除，此时需要一个特殊权限的用户

```
rsmongo:PRIMARY> db.createUser({user: "systemAdmin",pwd: "root",roles: [ { role: "__system", db: "admin" } ]})
Successfully added user: {
	"user" : "systemAdmin",
	"roles" : [
		{
			"role" : "__system",
			"db" : "admin"
		}
	]
}
```

这时候，在去OTHER节点上查看

```
rsmongo:OTHER> use admin
switched to db admin
rsmongo:OTHER> db.auth("systemAdmin","root")
1
rsmongo:OTHER> use local
switched to db local
rsmongo:OTHER> db.system.replset.find()
{ "_id" : "rsmongo", "version" : 4, "protocolVersion" : NumberLong(1), "writeConcernMajorityJournalDefault" : true, "members" : [ { "_id" : 0, "host" : "10.13.0.182:27017", "arbiterOnly" : false, "buildIndexes" : true, "hidden" : false, "priority" : 3, "tags" : {  }, "slaveDelay" : NumberLong(0), "votes" : 1 }, { "_id" : 1, "host" : "10.13.0.184:27017", "arbiterOnly" : false, "buildIndexes" : true, "hidden" : false, "priority" : 2, "tags" : {  }, "slaveDelay" : NumberLong(0), "votes" : 1 }, { "_id" : 2, "host" : "10.13.0.185:27017", "arbiterOnly" : false, "buildIndexes" : true, "hidden" : false, "priority" : 1, "tags" : {  }, "slaveDelay" : NumberLong(0), "votes" : 1 }, { "_id" : 4, "host" : "10.13.0.187:27017", "arbiterOnly" : false, "buildIndexes" : true, "hidden" : false, "priority" : 1, "tags" : {  }, "slaveDelay" : NumberLong(0), "votes" : 1 } ], "settings" : { "chainingAllowed" : true, "heartbeatIntervalMillis" : 2000, "heartbeatTimeoutSecs" : 10, "electionTimeoutMillis" : 10000, "catchUpTimeoutMillis" : -1, "catchUpTakeoverDelayMillis" : 30000, "getLastErrorModes" : {  }, "getLastErrorDefaults" : { "w" : 1, "wtimeout" : 0 }, "replicaSetId" : ObjectId("5ed85559a89a27f7cce7194e") } }
```

现在可以查看这个信息了，然后尝试删除，好像还是不行

最后只能修改conf文件，去掉replset参数，重新启动mongodb服务，这样被移除的节点才变为单节点

9、测试选举功能

主节点执行如下命令，即可降级为副本节点

```
rsmongo:PRIMARY> rs.stepDown(30)
2020-06-04T15:10:34.568+0800 E QUERY    [js] Error: error doing query: failed: network error while attempting to run command 'replSetStepDown' on host '127.0.0.1:27017'  :
DB.prototype.runCommand@src/mongo/shell/db.js:168:1
DB.prototype.adminCommand@src/mongo/shell/db.js:185:1
rs.stepDown@src/mongo/shell/utils.js:1433:12
@(shell):1:1
2020-06-04T15:10:34.570+0800 I NETWORK  [js] trying reconnect to 127.0.0.1:27017 failed
2020-06-04T15:10:34.571+0800 I NETWORK  [js] reconnect 127.0.0.1:27017 ok
rsmongo:SECONDARY> 

#rs.stepDown(30) 这个命令会让primary降级为Secondary节点，并维持30s
```

详细节点操作可以参考[MongoDB副本集的常用操作及原理](https://www.cnblogs.com/ivictor/p/6804408.html)

