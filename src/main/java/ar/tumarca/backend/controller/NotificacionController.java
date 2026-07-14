package ar.tumarca.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    private final JdbcTemplate jdbcTemplate;

    public NotificacionController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> obtenerNotificaciones(
            @RequestParam(defaultValue = "pendiente") String estado,
            @RequestParam(defaultValue = "50") int limite) {

        String sql = "SELECT * FROM notificaciones WHERE estado = ? ORDER BY fecha_creacion DESC LIMIT ?";
        return ResponseEntity.ok(jdbcTemplate.queryForList(sql, estado, limite));
    }

    @PostMapping("/{id}/marcar-leida")
    public ResponseEntity<Map<String, String>> marcarLeida(@PathVariable Long id) {
        String sql = "UPDATE notificaciones SET estado = 'leida', fecha_lectura = NOW() WHERE id = ?";
        jdbcTemplate.update(sql, id);
        return ResponseEntity.ok(Map.of("status", "marcada como leída"));
    }

    @GetMapping("/estadisticas")
    public ResponseEntity<Map<String, Object>> obtenerEstadisticas() {
        String sql = """
            SELECT 
                COUNT(*) as total,
                COUNT(CASE WHEN estado = 'pendiente' THEN 1 END) as pendientes,
                COUNT(CASE WHEN estado = 'leida' THEN 1 END) as leidas,
                COUNT(CASE WHEN prioridad = 'alta' THEN 1 END) as alta_prioridad
            FROM notificaciones
            """;
        return ResponseEntity.ok(jdbcTemplate.queryForMap(sql));
    }
}