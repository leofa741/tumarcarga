package ar.tumarca.backend.controller;

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

@RestController
@RequestMapping("/api")
public class ChatController {

    private final ChatClient chatClient;

    public ChatController(ChatClient.Builder chatClientBuilder,
                          ChatMemory chatMemory,
                          ContactoTool contactoTool,
                          ClienteTool clienteTool,
                          VectorStore vectorStore) {

        // Registrar las herramientas
        ToolCallbackProvider tools = MethodToolCallbackProvider.builder()
                .toolObjects(contactoTool, clienteTool)
                .build();

        this.chatClient = chatClientBuilder
                .defaultSystem("""
                    Eres un asistente virtual de tumarca.ar, una agencia de desarrollo web en Buenos Aires, Argentina.
                    
                    INSTRUCCIONES CRÍTICAS:
                    - SIEMPRE responde en español
                    - Usa el contexto proporcionado entre etiquetas <context> para responder
                    - Si el contexto tiene información relevante, úsala para responder
                    - Sé específico y menciona datos concretos del contexto
                    - Si no hay información en el contexto, di: "No tengo información específica sobre eso"
                    
                    SOBRE EL CONTEXTO:
                    - El contexto contiene información real sobre servicios, casos de éxito, tecnologías y metodología de tumarca.ar
                    - Cuando te pregunten sobre experiencia, menciona los casos de éxito específicos
                    - Cuando te pregunten sobre tecnologías, lista las tecnologías específicas
                    - Cuando te pregunten sobre proceso, explica los pasos específicos
                    
                    HERRAMIENTAS DISPONIBLES:
                    - listarClientes: Lista clientes de la base de datos
                    - registrarCliente: Registra un nuevo cliente
                    - obtenerContacto: Muestra información de contacto
                    - obtenerPrecio: Muestra precios de servicios
                    - listarServicios: Lista todos los servicios
                    
                    Al final, invita a contactarse por WhatsApp al +54 9 11 4146-1312
                    """)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        RetrievalAugmentationAdvisor.builder()
                                .documentRetriever(
                                        VectorStoreDocumentRetriever.builder()
                                                .vectorStore(vectorStore)
                                                .similarityThreshold(0.0)
                                                .topK(5)
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
            String response = chatClient.prompt()
                    .user(request.message())
                    .advisors(spec -> spec.param("chat_memory_conversation_id",
                            request.conversationId()))
                    .call()
                    .content();

            return ResponseEntity.ok(new ChatResponse(response, request.conversationId()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new ChatResponse("Error al procesar tu mensaje. Por favor, intenta nuevamente.",
                            request.conversationId()));
        }
    }

    @GetMapping("/tools")
    public ResponseEntity<ToolsResponse> getTools() {
        return ResponseEntity.ok(new ToolsResponse(
                "Herramientas configuradas en la aplicación",
                new String[]{
                        "listarClientes - Lista clientes de la base de datos",
                        "registrarCliente - Registra un nuevo cliente",
                        "obtenerContacto - Muestra información de contacto",
                        "obtenerPrecio - Muestra precios de servicios",
                        "listarServicios - Lista todos los servicios"
                }
        ));
    }

    // Request con validación
    public record ChatRequest(
            @NotBlank(message = "El mensaje es obligatorio")
            @Size(min = 1, max = 2000, message = "El mensaje debe tener entre 1 y 2000 caracteres")
            String message,

            @NotBlank(message = "El conversationId es obligatorio")
            @Size(max = 100, message = "El conversationId no puede tener más de 100 caracteres")
            String conversationId
    ) {}

    // Response estructurado
    public record ChatResponse(String reply, String conversationId) {}

    // Response para tools
    public record ToolsResponse(String description, String[] tools) {}
}