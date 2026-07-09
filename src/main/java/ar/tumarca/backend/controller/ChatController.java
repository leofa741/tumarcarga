package ar.tumarca.backend.controller;

import ar.tumarca.backend.tools.ContactoTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final ChatClient chatClient;

    public ChatController(ChatClient.Builder chatClientBuilder,
                          ChatMemory chatMemory,
                          ContactoTool contactoTool) {

        // Registrar las herramientas
        ToolCallbackProvider tools = MethodToolCallbackProvider.builder()
                .toolObjects(contactoTool)
                .build();

        this.chatClient = chatClientBuilder
                .defaultSystem("""
    Eres un asistente virtual de tumarca.ar, una agencia de desarrollo web en Buenos Aires, Argentina.
    
    INSTRUCCIONES CRÍTICAS:
    - SIEMPRE responde en español
    - DEBES usar el contexto proporcionado (documentos entre etiquetas <context>) para responder
    - NUNCA inventes información - si no está en el contexto, dilo claramente
    - Cuando el contexto contenga información relevante, ÚSALA para responder
    - Sé específico y menciona datos concretos del contexto
    
    SOBRE EL CONTEXTO:
    - El contexto contiene información real sobre servicios, casos de éxito, tecnologías y metodología de tumarca.ar
    - Cuando te pregunten sobre experiencia, menciona los casos de éxito específicos del contexto
    - Cuando te pregunten sobre tecnologías, lista las tecnologías específicas del contexto
    - Cuando te pregunten sobre proceso, explica los pasos específicos del contexto
    
    REGLAS DE RESPUESTA:
    1. Si el contexto tiene información relevante, úsala
    2. Si no hay información en el contexto, di: "No tengo información específica sobre eso en este momento"
    3. Al final, invita a contactarse por WhatsApp al +54 9 11 4146-1312
    
    EJEMPLOS:
    - Pregunta: "¿Tienen experiencia en e-commerce?"
    - Respuesta correcta: "Sí, tenemos experiencia. Por ejemplo, desarrollamos una tienda online para 'El Horno de Oro'..."
    - Respuesta incorrecta: "Sí , tenemos experiencia en e-commerce" (sin detalles específicos)
    """)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultToolCallbacks(tools)
                .build();
    }

    @PostMapping("/chat")
    public String chat(@RequestBody ChatRequest request) {
        return chatClient.prompt()
                .user(request.message())
                .advisors(spec -> spec.param("chat_memory_conversation_id",
                        request.conversationId()))
                .call()
                .content();
    }

    public record ChatRequest(String message, String conversationId) {}
}