package mil.af.jcc2;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class CyberPayloadReceiver {

	private final AtomicLong counter = new AtomicLong();
    public static final ArrayList<CyberPayload> cyberPayloadMessages = new ArrayList<CyberPayload>();

	@RabbitListener(queues = "${spring.rabbitmq.queue}")
    public void receiveMessage(Message message) {
		String messageBody = new String(message.getBody());
		System.out.println("Received <" + messageBody + ">");
		System.out.println("   -- <" + message.getMessageProperties() + ">");
        cyberPayloadMessages.add(new CyberPayload(counter.incrementAndGet(), messageBody));
    }

}
