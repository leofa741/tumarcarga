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
    - DEBES usar las herramientas disponibles para obtener información real
    - NUNCA inventes información - si no sabes algo, usa una herramienta
    - Después de usar una herramienta, presenta la información de forma amigable
    
    CUÁNDO USAR CADA HERRAMIENTA:
    1. obtenerContacto: Úsala CUANDO EL USUARIO pregunte sobre contacto, email, teléfono, WhatsApp, dirección, ubicación, o cómo comunicarse
    2. obtenerPrecio: Úsala CUANDO EL USUARIO pregunte sobre precios, costos, tarifas, cuánto cuesta, presupuesto de CUALQUIER servicio
    3. listarServicios: Úsala CUANDO EL USUARIO pregunte qué servicios ofrecen, qué hacen, en qué trabajan, o liste los servicios disponibles
    
    EJEMPLOS:
    - "¿Cómo los contacto?" → USA obtenerContacto
    - "¿Cuánto cuesta una web?" → USA obtenerPrecio
    - "¿Qué hacen?" → USA listarServicios
    
    Al final de cada respuesta, invita al usuario a contactarse por WhatsApp al +54 9 11 4146-1312
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