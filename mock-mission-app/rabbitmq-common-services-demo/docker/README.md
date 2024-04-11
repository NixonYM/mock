# Local laptop dev and test
To confirm the demo apps work as expected before trying against live JKOP clusters, use a local set of services deployed via Docker and Maven.

## RabbitMQ Service
Start a local rabbitmq instance using docker compose. The image deploys and exposes the management UI (http://localhost:15672) and the AMQP endpoint (port 5762). The default admin username/password are in the docker-compose.yaml file.
```
cd docker/rabbitmq
docker compose up
```

## 1. Simple CLI demo app
The CLI demo app is an off-the-shelf Spring Boot guide project that posts a single message to a topic exchange and then reads the message off the queue based on the routing key used by the exchange->queue binding. The exchanges, queues and default admin user are all contained in the default RabbitMQ virtual-host "/".

### Create the demo app docker image
```
git clone https://github.com/spring-guides/gs-messaging-rabbitmq.git
cd gs-messaging-rabbitmq/complete
mvn spring-boot:build-image -DskipTests
```

### Run the demo app Docker container
The terminal output will show `Sending message...` followed by `Received <Hello from RabbitMQ!>`
```
export SPRING_RABBITMQ_USERNAME=jkopadmin SPRING_RABBITMQ_PASSWORD=jkoppassword; mvn spring-boot:run
```

## 2. Advanced Multicluster demo webapp
The cluster demo webapp is a more complete, yet self-contained, Spring Boot webapp with two REST endpoints that 1) send a message to an exchange and 2) lists all messages received from the queue. The sending method will post a single message to a topic exchange and the consumer method will reads the message off the queue based on the routing key used by the exchange->queue binding. For this demo, we create a new RabbitMQ virtual-host "jcc2-readiness" as a logical grouping for the exchanges, queues and an app-specific user.

### Create the demo app docker image
```
from this cloned repo...
cd rabbitmq-common-services-demo
mvn spring-boot:build-image -DskipTests
```

### Create the new vhost and user
```
curl -u jkopadmin:jkoppassword -X PUT http://localhost:15672/api/vhosts/jcc2-readiness
curl -u jkopadmin:jkoppassword -d '{"password":"readinesspassword","tags":"administrator"}' -H "Content-Type: application/json" -X PUT http://localhost:15672/api/users/readinessuser
curl -u jkopadmin:jkoppassword -d '{"configure":".*","write":".*","read":".*"}' -H "Content-Type: application/json" -X PUT http://localhost:15672/api/permissions/jcc2-readiness/readinessuser
```

### Run the demo app Docker container
Running the application will start the webapp tomcat server that listens on port 8080.
```
export SPRING_RABBITMQ_USERNAME=readinessuser SPRING_RABBITMQ_PASSWORD=readinesspassword; mvn spring-boot:run
```

### Generate messages and confirm receipt
Generate data from the app, which is then sent to a RabbitMQ exchange->queue. Refreshing the send REST endpoint URL in a browser will generate and send one new message per HTTP GET. Update the `cyberPayload` query parameter to use a different message payload string. The confirm payload REST endpoint URL will show all received messages in order.
```
http://localhost:8080/sendCyberPayload?cyberPayload=ASI-65456
http://localhost:8080/confirmCyberPayload
```

## Tear down
- Ctrl-C the RabbitmQ container to stop it
- Ctrl-C the webapp to stop it
