package mil.af.jcc2;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
public class CyberPayloadControllerIntTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	public void noParamSendCyberPayloadShouldReturnDefaultMessage() throws Exception {

		this.mockMvc.perform(get("/sendCyberPayload")).andDo(print()).andExpect(status().isOk())
				.andExpect(jsonPath("$.content").value("ASI-12345"));
	}

	@Test
	public void paramSendCyberPayloadShouldReturnTailoredMessage() throws Exception {

		this.mockMvc.perform(get("/sendCyberPayload").param("cyberPayload", "USI-98765"))
				.andDo(print()).andExpect(status().isOk())
				.andExpect(jsonPath("$.content").value("USI-98765"));
	}

}
