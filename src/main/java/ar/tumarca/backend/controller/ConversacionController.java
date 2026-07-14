package ar.tumarca.backend.controller;

import ar.tumarca.backend.repository.ConversacionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/conversaciones")
public class ConversacionController {

    private final ConversacionRepository conversacionRepository;

    public ConversacionController(ConversacionRepository conversacionRepository) {
        this.conversacionRepository = conversacionRepository;
    }

    // ✅ NUEVO: Endpoint para registrar un mensaje manualmente
    @PostMapping
    public ResponseEntity<Map<String, String>> registrarMensaje(@RequestBody Map<String, Object> request) {
        String conversationId = (String) request.get("conversationId");
        String role = (String) request.get("role");
        String message = (String) request.get("message");

        conversacionRepository.guardarMensaje(conversationId, role, message, null);

        return ResponseEntity.ok(Map.of("status", "guardado"));
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listarConversaciones(
            @RequestParam(defaultValue = "50") int limite) {
        return ResponseEntity.ok(conversacionRepository.obtenerUltimasConversaciones(limite));
    }

    @GetMapping("/{conversationId}")
    public ResponseEntity<List<Map<String, Object>>> obtenerConversacion(
            @PathVariable String conversationId) {
        return ResponseEntity.ok(conversacionRepository.obtenerConversacion(conversationId));
    }

    @GetMapping("/estadisticas")
    public ResponseEntity<Map<String, Object>> obtenerEstadisticas() {
        return ResponseEntity.ok(conversacionRepository.obtenerEstadisticas());
    }

    @GetMapping("/preguntas-frecuentes")
    public ResponseEntity<List<Map<String, Object>>> preguntasFrecuentes(
            @RequestParam(defaultValue = "10") int limite) {
        return ResponseEntity.ok(conversacionRepository.obtenerPreguntasFrecuentes(limite));
    }
}