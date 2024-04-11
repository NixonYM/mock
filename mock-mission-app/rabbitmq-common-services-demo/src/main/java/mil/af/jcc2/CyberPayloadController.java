package mil.af.jcc2;

import java.util.concurrent.atomic.AtomicLong;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CyberPayloadController {

	private final AtomicLong counter = new AtomicLong();
    private final RabbitTemplate rabbitTemplate;
    private final String topicExchangeName;
    private final String routingKey;

	public CyberPayloadController(RabbitTemplate rabbitTemplate, 
	                              @Value("${spring.rabbitmq.exchange}") String exchange,
                             	  @Value("${spring.rabbitmq.routingKey}") String key) {
		this.rabbitTemplate = rabbitTemplate;
		this.topicExchangeName = exchange;
		this.routingKey = key;
	}

	@GetMapping("/sendCyberPayload")
	public CyberPayload sendCyberPayload(@RequestParam(value = "cyberPayload", defaultValue = "ASI-12345") String cyberPayload) {

		String msgRoutingKey = routingKey.replace("#", "bdp");
	    rabbitTemplate.convertAndSend(topicExchangeName, msgRoutingKey, cyberPayload);

		return new CyberPayload(counter.incrementAndGet(), cyberPayload);		
	}
}
