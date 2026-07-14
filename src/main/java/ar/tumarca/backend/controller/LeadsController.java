package ar.tumarca.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/leads")
public class LeadsController {

    private final JdbcTemplate jdbcTemplate;

    public LeadsController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> obtenerLeads() {
        String sql = "SELECT * FROM clientes ORDER BY score DESC, fecha_registro DESC";
        List<Map<String, Object>> leads = jdbcTemplate.queryForList(sql);

        int hot = 0, warm = 0, cold = 0;
        for (Map<String, Object> lead : leads) {
            String cal = (String) lead.get("calificacion");
            if (cal.contains("HOT")) hot++;
            else if (cal.contains("WARM")) warm++;
            else cold++;
        }

        return ResponseEntity.ok(Map.of(
                "total", leads.size(),
                "hot", hot,
                "warm", warm,
                "cold", cold,
                "leads", leads
        ));
    }

    @GetMapping("/hot")
    public ResponseEntity<List<Map<String, Object>>> obtenerLeadsHot() {
        String sql = "SELECT * FROM clientes WHERE calificacion LIKE '%HOT%' ORDER BY fecha_registro DESC";
        return ResponseEntity.ok(jdbcTemplate.queryForList(sql));
    }

    @GetMapping("/warm")
    public ResponseEntity<List<Map<String, Object>>> obtenerLeadsWarm() {
        String sql = "SELECT * FROM clientes WHERE calificacion LIKE '%WARM%' ORDER BY fecha_registro DESC";
        return ResponseEntity.ok(jdbcTemplate.queryForList(sql));
    }

    @GetMapping("/estadisticas")
    public ResponseEntity<Map<String, Object>> obtenerEstadisticasLeads() {
        String sql = """
            SELECT 
                COUNT(*) as total_leads,
                AVG(score) as score_promedio,
                COUNT(CASE WHEN calificacion LIKE '%HOT%' THEN 1 END) as hot_leads,
                COUNT(CASE WHEN calificacion LIKE '%WARM%' THEN 1 END) as warm_leads,
                COUNT(CASE WHEN calificacion LIKE '%COLD%' THEN 1 END) as cold_leads,
                MAX(fecha_registro) as ultimo_lead
            FROM clientes
            """;
        return ResponseEntity.ok(jdbcTemplate.queryForMap(sql));
    }
}