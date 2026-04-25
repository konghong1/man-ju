package org.kh.manju.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class OpenAiCompatibleLlmClient extends SimulatedLlmClient {

    public OpenAiCompatibleLlmClient(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
    public String provider() {
        return "openai-compatible";
    }
}
