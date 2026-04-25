package org.kh.manju.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class GeminiLlmClient extends SimulatedLlmClient {

    public GeminiLlmClient(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
    public String provider() {
        return "gemini";
    }
}
