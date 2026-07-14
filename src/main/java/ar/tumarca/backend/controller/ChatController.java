package ar.tumarca.backend.controller;

import ar.tumarca.backend.repository.ConversacionRepository;
import ar.tumarca.backend.tools.ClienteTool;
import ar.tumarca.backend.tools.ContactoTool;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final ChatClient chatClient;
    private final ConversacionRepository conversacionRepository;

    public ChatController(ChatClient.Builder chatClientBuilder,
                          ChatMemory chatMemory,
                          ContactoTool contactoTool,
                          ClienteTool clienteTool,
                          VectorStore vectorStore,
                          ConversacionRepository conversacionRepository) {
        this.conversacionRepository = conversacionRepository;

        ToolCallbackProvider tools = MethodToolCallbackProvider.builder()
                .toolObjects(contactoTool, clienteTool)
                .build();

        this.chatClient = chatClientBuilder
                // 🔥 CAMBIO 1: Prompt Estricto Anti-Alucinación
                .defaultSystem("""
                    Eres el asistente virtual de tumarca.ar, agencia de desarrollo web en Buenos Aires.
                    
                    REGLA ABSOLUTA Y PRIORITARIA:
                    Tu ÚNICA fuente de verdad es la información proporcionada entre las etiquetas <context>. 
                    ESTÁ TERMINANTEMENTE PROHIBIDO usar tu conocimiento general para inventar precios, servicios, 
                    tecnologías, procesos o testimonios que no estén explícitamente en el <context>.
                    
                    INSTRUCCIONES DE RESPUESTA:
                    1. Responde SIEMPRE en español.
                    2. Sé específico: menciona nombres, números y tecnologías exactas del <context>.
                    3. Si la respuesta a la pregunta del usuario NO está en el <context>, debes responder 
                       EXACTAMENTE: "No tengo esa información específica en mis documentos. Te invito a 
                       contactarnos por WhatsApp al +54 9 11 4146-1312 para que un humano te asesore".
                    4. Finaliza TODAS las respuestas útiles invitando a WhatsApp: +54 9 11 4146-1312.
                    
                    GUÍA DE TEMAS (Usa SOLO el <context> para estos):
                    - Experiencia/casos → Cita "El Horno de Oro" o "Style Boutique" si están en el contexto.
                    - Precios → Da los valores exactos en USD del contexto.
                    - Tecnologías → Lista solo las del stack del contexto.
                    - Proceso → Explica los 7 pasos del contexto.
                    - Testimonios → Cita nombres reales del contexto.
                    
                    HERRAMIENTAS DISPONIBLES (Úsalas cuando aplique):
                    - obtenerContacto, obtenerPrecio, listarServicios, registrarCliente, listarClientes.
                    """)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        RetrievalAugmentationAdvisor.builder()
                                .documentRetriever(
                                        VectorStoreDocumentRetriever.builder()
                                                .vectorStore(vectorStore)
                                                // 🔥 CAMBIO 2: Umbral de similitud más estricto (0.45)
                                                // Esto filtra el "ruido". Si un documento no se parece al menos un 45%
                                                // a la pregunta, NO se envía a la IA, evitando que invente.
                                                .similarityThreshold(0.45)
                                                .topK(7)                    // Trae los 7 mejores fragmentos que pasen el umbral
                                                .build()
                                )
                                .build()
                )
                .defaultToolCallbacks(tools)
                .build();
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        try {
            conversacionRepository.guardarMensaje(
                    request.conversationId(),
                    "user",
                    request.message(),
                    Map.of("timestamp", System.currentTimeMillis())
            );

            String response = chatClient.prompt()
                    .user(request.message())
                    .advisors(spec -> spec.param("chat_memory_conversation_id",
                            request.conversationId()))
                    .call()
                    .content();

            conversacionRepository.guardarMensaje(
                    request.conversationId(),
                    "bot",
                    response,
                    Map.of("timestamp", System.currentTimeMillis(), "source", "ai")
            );

            return ResponseEntity.ok(new ChatResponse(response, request.conversationId()));

        } catch (Exception e) {
            System.err.println("❌ Error en chat: " + e.getMessage());

            String fallbackResponse = """
            Lo siento, estoy experimentando dificultades técnicas.
            
            Mientras tanto, puedes contactarnos directamente:
            📱 WhatsApp: +54 9 11 4146-1312
            📧 Email: hola@tumarca.ar
            🌐 Web: www.tumarca.ar
            
            Nuestro equipo te atenderá personalmente.
            """;

            conversacionRepository.guardarMensaje(
                    request.conversationId(),
                    "bot",
                    fallbackResponse,
                    Map.of("timestamp", System.currentTimeMillis(), "source", "fallback", "error", e.getMessage())
            );

            return ResponseEntity.ok(new ChatResponse(fallbackResponse, request.conversationId()));
        }
    }

    @GetMapping("/tools")
    public ResponseEntity<ToolsResponse> getTools() {
        return ResponseEntity.ok(new ToolsResponse(
                "Herramientas configuradas en la aplicación",
                new String[]{
                        "listarClientes", "registrarCliente", "obtenerContacto",
                        "obtenerPrecio", "listarServicios"
                }
        ));
    }

    public record ChatRequest(
            @NotBlank @Size(min = 1, max = 2000) String message,
            @NotBlank @Size(max = 100) String conversationId
    ) {}

    public record ChatResponse(String reply, String conversationId) {}
    public record ToolsResponse(String description, String[] tools) {}
}