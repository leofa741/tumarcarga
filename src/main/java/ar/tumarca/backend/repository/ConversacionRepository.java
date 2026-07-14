package ar.tumarca.backend.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class ConversacionRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public ConversacionRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Guarda un mensaje de la conversación
     */
    public void guardarMensaje(String conversationId, String role, String message, Map<String, Object> metadata) {
        try {
            String sql = "INSERT INTO conversaciones (conversation_id, role, message, metadata) VALUES (?, ?, ?, ?::jsonb)";

            // Convertir metadata a JSON usando Jackson
            String metadataJson = "{}";
            if (metadata != null && !metadata.isEmpty()) {
                metadataJson = objectMapper.writeValueAsString(metadata);
            }

            System.out.println("💾 Guardando mensaje: conversationId=" + conversationId +
                    ", role=" + role +
                    ", message=" + message.substring(0, Math.min(50, message.length())) + "...");

            jdbcTemplate.update(sql, conversationId, role, message, metadataJson);

            System.out.println("✅ Mensaje guardado correctamente");

        } catch (JsonProcessingException e) {
            System.err.println("❌ Error procesando JSON: " + e.getMessage());
            // Guardar sin metadata si falla el JSON
            try {
                String sql = "INSERT INTO conversaciones (conversation_id, role, message) VALUES (?, ?, ?)";
                jdbcTemplate.update(sql, conversationId, role, message);
            } catch (Exception ex) {
                System.err.println("❌ Error guardando sin metadata: " + ex.getMessage());
            }
        } catch (Exception e) {
            System.err.println("❌ Error guardando mensaje: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Obtiene todos los mensajes de una conversación
     */
    public List<Map<String, Object>> obtenerConversacion(String conversationId) {
        String sql = "SELECT id, conversation_id, role, message, metadata, created_at " +
                "FROM conversaciones WHERE conversation_id = ? ORDER BY created_at ASC";
        return jdbcTemplate.queryForList(sql, conversationId);
    }

    /**
     * Obtiene las últimas N conversaciones únicas
     */
    public List<Map<String, Object>> obtenerUltimasConversaciones(int limite) {
        String sql = "SELECT DISTINCT conversation_id, " +
                "MIN(created_at) as inicio, " +
                "MAX(created_at) as fin, " +
                "COUNT(*) as total_mensajes " +
                "FROM conversaciones " +
                "GROUP BY conversation_id " +
                "ORDER BY fin DESC " +
                "LIMIT ?";
        return jdbcTemplate.queryForList(sql, limite);
    }

    /**
     * Obtiene estadísticas generales
     */
    public Map<String, Object> obtenerEstadisticas() {
        String sql = "SELECT " +
                "COUNT(DISTINCT conversation_id) as total_conversaciones, " +
                "COUNT(*) as total_mensajes, " +
                "COUNT(CASE WHEN role = 'user' THEN 1 END) as mensajes_usuario, " +
                "COUNT(CASE WHEN role = 'bot' THEN 1 END) as mensajes_bot, " +
                "MIN(created_at) as primera_conversacion, " +
                "MAX(created_at) as ultima_conversacion " +
                "FROM conversaciones";
        return jdbcTemplate.queryForMap(sql);
    }

    /**
     * Obtiene las preguntas más frecuentes
     */
    public List<Map<String, Object>> obtenerPreguntasFrecuentes(int limite) {
        String sql = "SELECT message, COUNT(*) as frecuencia " +
                "FROM conversaciones " +
                "WHERE role = 'user' " +
                "GROUP BY message " +
                "ORDER BY frecuencia DESC " +
                "LIMIT ?";
        return jdbcTemplate.queryForList(sql, limite);
    }
}