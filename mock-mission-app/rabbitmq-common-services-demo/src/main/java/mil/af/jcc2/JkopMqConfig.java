package mil.af.jcc2;

import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@Configuration
public class JkopMqConfig {

	static String topicExchangeName;
	static String queueName;
    static String routingKey;

    public JkopMqConfig(@Value("${spring.rabbitmq.exchange}") String exchange,
                        @Value("${spring.rabbitmq.queue}") String queue,
                        @Value("${spring.rabbitmq.routingKey}") String key) {
        this.topicExchangeName = exchange;
        this.queueName = queue;
        this.routingKey = key;
    }

	@Bean
	Queue queue() {
		return new Queue(queueName, false);
	}

	@Bean
	TopicExchange exchange() {
		return new TopicExchange(topicExchangeName);
	}

	@Bean
	Binding binding(Queue queue, TopicExchange exchange) {
		return BindingBuilder.bind(queue).to(exchange).with(routingKey);
	}

}
