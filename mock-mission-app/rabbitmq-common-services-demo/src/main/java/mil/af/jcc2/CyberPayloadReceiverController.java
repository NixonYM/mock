package mil.af.jcc2;

import java.util.ArrayList;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CyberPayloadReceiverController {

    @GetMapping("/confirmCyberPayload")
	public ArrayList<CyberPayload> confirmCyberPayload() {		
		return CyberPayloadReceiver.cyberPayloadMessages;
	}
}
