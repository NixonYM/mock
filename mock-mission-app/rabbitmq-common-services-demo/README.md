# Demo Intent
Demo of cluster/multicluster usage of a common services message broker, showcasing how two mission applications can exchange message payloads using standard EIP and AMQP protocols.

"Topic exchanges route messages to queues based on wildcard matches between the routing key and the routing pattern, which is specified by the queue binding. Messages are routed to one or many queues based on a matching between a message routing key and this pattern." Ref: https://www.cloudamqp.com/blog/part4-rabbitmq-for-beginners-exchanges-routing-keys-bindings.html#topic-exchange 

# Demo Objectives
1. Live demo of async message exchange across multiple JKOP clusters
2. Quick overview of business driver and value of EIP patterns
    - Standards and protocols: AMQP, STOMP, MQTT
3. Address access controls supporting multi-tenant security
    - authN identity, authZ permissions, vhosts
4. Data protection
    - SSL/TLS enabled and all data to/from broker services are encrypted
    - On disk (TBD)
    - Additional plugins, e.g. broker federation, authN integrations, etc.
5. Additional features of message brokering implementation, relevant to JCC2

# Local demo app dev and test
Reference [this README.md](docker/README.md)

# Run the live JKOP cluster demo(s)

## 1a. Simple demo from the cloud management server
This simple demo confirms that RabbitMQ is deploy to the cluster and operating as expected.

Prerequisites:

1. Cloud management server instance type is "t3.medium", as java needs the memory
2. All jkop-apps are zarf deployed to a JKOP cluster
3. RabbitMQ services are deployed to a JKOP cluster
4. RabbitMQ default username and password are known
    - Username: Contained in Secret "rabbitmq-jkop-config" > rabbitmq.conf > "default_user = user"
    - Password: Contained in Secret "rabbitmq-jkop" > rabbitmq-password > base64 encoded
5. Demo app container has been loaded in the cloud mgmt server Docker engine.
    - `messaging-rabbitmq-complete:0.0.1-SNAPSHOT`

### Perform the Demo
These steps assume the rabbitmq service types are ClusterIP, exposing ports 5672 and 15672. The final goal is to have these services exposed on a public ingress gateway using DNS domain "rabbitmq.jkop-int.jcc2.org".

SSH session #1
```
ssh -F ssh_config mgmt
sudo docker network create cloud-mgmt-network
edit ./run_docker.sh to add "--name "jkop-manager" --network cloud-mgmt-network -p 28000:28000" to the docker command
sudo ./run_docker.sh

(assume cie-ato-admin role)
k port-forward --address 0.0.0.0 service/rabbitmq-jkop 28000:5672 -n rabbitmq
```

SSH session #2
```
ssh -F ssh_config mgmt
sudo docker run -t --network=cloud-mgmt-network -e SPRING_RABBITMQ_HOST=jkop-manager -e SPRING_RABBITMQ_PORT=28000 -e SPRING_RABBITMQ_USERNAME=<user> -e SPRING_RABBITMQ_PASSWORD=<password> messaging-rabbitmq-complete:0.0.1-SNAPSHOT
```

**Demo Callouts**
- The logging output from SSH session #2 will show successful message producer/consumer actions, `Sending message...` followed by `Received <Hello from RabbitMQ!>`

## 1b. Advanced webapp demo from the cloud management server
This demo is an intermedia, and temporary, step to show more advanced broker capabilities. It uses a webapp that is deployed to the cloud mgmt server and invoked via curl CLI calls. The end goal is to deploy this app to the cluster(s) and demonstate it using a browser, simulating an actual mission application. This end goal demo is outline in section "#2. Advanced Multicluster demo webapp" below.

Prerequisites:

6. Demo app container has been loaded in the cloud mgmt server Docker engine.
    - `jkop-message-queue-demo-app:0.0.1-SNAPSHOT`

### Perform the Demo
These steps assume the rabbitmq service types are ClusterIP, exposing ports 5672 and 15672. The final goal is to have these services exposed on a public ingress gateway using DNS domain "rabbitmq.jkop-int.jcc2.org".

SSH session #1
```
ssh -F ssh_config mgmt
sudo docker network create cloud-mgmt-network
edit ./run_docker.sh to add "--name "jkop-manager" --network cloud-mgmt-network -p 28000:28000" to the docker command
sudo ./run_docker.sh

(assume cie-ato-admin role)
k port-forward --address 0.0.0.0 service/rabbitmq-jkop 28000:15672 -n rabbitmq
```

SSH session #2
```
ssh -F ssh_config mgmt
curl -u <user>:<password> -X PUT http://localhost:28000/api/vhosts/jcc2-readiness
curl -u <user>:<password> -d '{"password":"readinesspassword","tags":"administrator"}' -H "Content-Type: application/json" -X PUT http://localhost:28000/api/users/readinessuser
curl -u <user>:<password> -d '{"configure":".*","write":".*","read":".*"}' -H "Content-Type: application/json" -X PUT http://localhost:28000/api/permissions/jcc2-readiness/readinessuser
```

SSH session #1 (cont.)
```
Ctrl-C the port-forward command to port 15672
k port-forward --address 0.0.0.0 service/rabbitmq-jkop 28000:5672 -n rabbitmq
```

SSH session #2 (cont.)
```
sudo docker run -t --network=cloud-mgmt-network -p 9090:8080 -e SPRING_RABBITMQ_HOST=jkop-mgmr -e SPRING_RABBITMQ_PORT=28000 -e SPRING_RABBITMQ_VIRTUAL-HOST=jcc2-readiness -e SPRING_RABBITMQ_USERNAME=readinessuser -e SPRING_RABBITMQ_PASSWORD=readinesspassword jkop-message-queue-demo-app:0.0.1-SNAPSHOT
```

SSH session #3
```
ssh -F ssh_config mgmt
curl -X GET  http://localhost:9090/sendCyberPayload?cyberPayload=ASI-00555
curl -X GET  http://localhost:9090/sendCyberPayload?cyberPayload=ASI-00556
curl -X GET  http://localhost:9090/sendCyberPayload?cyberPayload=ASI-00557
curl -X GET  http://localhost:9090/confirmCyberPayload
```

**Demo Callouts**
- The REST call responses from SSH session #3 will show successful message producer/consumer actions. Each `sendCyberPayload` call will send one message to the broker. The `confirmCyberPayload` call will show a list of all messages received from the broker. 

## 2. Advanced Multicluster demo webapp

Prerequsites


1. All jkop-apps are zarf deployed to a JKOP cluster
2. RabbitMQ services are deployed to a JKOP cluster and addressable with DNS domain entries.
3. RabbitMQ default username and password are known
    - Username: Contained in Secret "rabbitmq-jkop-config" > rabbitmq.conf > "default_user = user"
    - Password: Contained in Secret "rabbitmq-jkop" > rabbitmq-password > base64 encoded
4. ArgoCD is deployed to a JKOP cluster

### Perform the Demo

```
Steps TBD...
```

**Demo Callouts**
- The REST call responses from SSH session #3 will show successful message producer/consumer actions. Each `sendCyberPayload` call will send one message to the broker. The `confirmCyberPayload` call will show a list of all messages received from the broker. 