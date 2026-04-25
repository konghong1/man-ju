package org.kh.manju.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class AzureOpenAiLlmClient extends SimulatedLlmClient {

    public AzureOpenAiLlmClient(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
    public String provider() {
        return "azure-openai";
    }
}
