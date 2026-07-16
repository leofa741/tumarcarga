package ar.tumarca.backend.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class ClienteTool {

    private final JdbcTemplate jdbcTemplate;
    private final JavaMailSender mailSender;

    // Cliente HTTP nativo de Java (no requiere dependencias extra en pom.xml)
    private final HttpClient httpClient = HttpClient.newHttpClient();

    // ⚙️ ========================================================================
    // ⚙️ CREDENCIALES DE TELEGRAM
    // ⚙️ ========================================================================
    private static final String TELEGRAM_BOT_TOKEN = "8228507019:AAGxUlM5MDpv1vux5JMAO4X5nT3uwXDGC94";
    private static final String TELEGRAM_CHAT_ID = "8874664977";

    // ⚙️ ========================================================================
    // ⚙️ EMAIL DEL EQUIPO (Para recibir las notificaciones internas por correo)
    // ⚙️ ========================================================================
    private static final String EMAIL_EQUIPO = "hola@tumarca.ar"; // 👈 ¡CAMBIA ESTO por tu email real!

    public ClienteTool(JdbcTemplate jdbcTemplate, JavaMailSender mailSender) {
        this.jdbcTemplate = jdbcTemplate;
        this.mailSender = mailSender;
    }

    @Tool(description = "REGISTRA un nuevo cliente potencial en la base de datos de tumarca.ar. DEBES usar esta herramienta cuando el usuario proporcione su nombre y email, o diga 'quiero contratar', 'necesito un presupuesto', 'me interesa', o cualquier intención de contacto. Calcula automáticamente la calificación del lead según el proyecto y servicios.")
    public String registrarCliente(
            @ToolParam(description = "Nombre completo del cliente") String nombre,
            @ToolParam(description = "Email del cliente") String email,
            @ToolParam(description = "Teléfono del cliente (opcional)") String telefono,
            @ToolParam(description = "Nombre de la empresa (opcional)") String empresa,
            @ToolParam(description = "Descripción del proyecto o necesidad") String proyecto,
            @ToolParam(description = "Servicios de interés separados por coma: web_basica, web_intermedia, web_avanzada, ecommerce, app_movil, branding, seo, marketing") String servicios_interes
    ) {
        try {
            // 1. Calcular score automáticamente
            int score = calcularScore(proyecto, servicios_interes, empresa);
            String calificacion = obtenerCalificacion(score);
            String prioridad = obtenerPrioridad(score);

            // 2. Guardar en base de datos
            String sql = """
                INSERT INTO clientes (nombre, email, telefono, empresa, proyecto, servicios_interes, score, calificacion, prioridad, fecha_registro)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

            jdbcTemplate.update(sql, nombre, email, telefono, empresa, proyecto,
                    servicios_interes, score, calificacion, prioridad, LocalDateTime.now());

            // 3. Enviar email de confirmación al cliente
            enviarEmailConfirmacionCliente(nombre, email, calificacion);

            // 4. Enviar email de notificación detallada al equipo interno
            enviarEmailNotificacionEquipo(nombre, email, empresa, proyecto, calificacion, score);

            // 5. 🔥 NOTIFICACIÓN INSTANTÁNEA A TELEGRAM (Solo si es HOT o WARM, score >= 4)
            if (score >= 4) {
                enviarNotificacionTelegram(nombre, empresa, proyecto, calificacion, score);
            }

            // 6. Crear notificación para el equipo en la base de datos
            crearNotificacion(nombre, email, calificacion, score);

            return String.format("""
                ✅ ¡Registro exitoso!
                
                📋 Datos del cliente:
                • Nombre: %s
                • Email: %s
                • Empresa: %s
                • Proyecto: %s
                • Servicios: %s
                
                🎯 Calificación automática: %s (Score: %d/10)
                📌 Prioridad: %s
                
                Hemos enviado un email de confirmación y nuestro equipo te contactará pronto.
                
                📱 Para atención inmediata: WhatsApp +54 9 11 4146-1312
                """, nombre, email, empresa != null ? empresa : "No especificada",
                    proyecto != null ? proyecto : "No especificado",
                    servicios_interes != null ? servicios_interes : "No especificados",
                    calificacion, score, prioridad);

        } catch (Exception e) {
            System.err.println("❌ Error registrando cliente: " + e.getMessage());
            e.printStackTrace();
            return "❌ Error al registrar el cliente. Por favor, intenta nuevamente o contáctanos por WhatsApp al +54 9 11 4146-1312";
        }
    }

    /**
     * Calcula el score del lead basado en múltiples factores
     */
    private int calcularScore(String proyecto, String servicios, String empresa) {
        int score = 0;

        if (servicios != null) {
            String serv = servicios.toLowerCase();
            if (serv.contains("app_movil") || serv.contains("app")) score += 4;
            else if (serv.contains("ecommerce") || serv.contains("e-commerce")) score += 3;
            else if (serv.contains("web_avanzada") || serv.contains("avanzada")) score += 3;
            else if (serv.contains("web_intermedia") || serv.contains("intermedia")) score += 2;
            else if (serv.contains("web_basica") || serv.contains("basica")) score += 1;

            if (serv.contains("seo")) score += 1;
            if (serv.contains("marketing")) score += 1;
            if (serv.contains("branding")) score += 1;
        }

        if (proyecto != null && proyecto.length() > 20) {
            String proj = proyecto.toLowerCase();
            if (proj.contains("tienda") || proj.contains("ecommerce") || proj.contains("ventas")) score += 2;
            if (proj.contains("sistema") || proj.contains("plataforma") || proj.contains("app")) score += 2;
            if (proj.length() > 100) score += 1;
        }

        if (empresa != null && empresa.length() > 3) {
            score += 1;
            if (empresa.toLowerCase().contains("s.a.") ||
                    empresa.toLowerCase().contains("srl") ||
                    empresa.toLowerCase().contains("s.r.l")) {
                score += 1;
            }
        }

        return Math.min(score, 10);
    }

    private String obtenerCalificacion(int score) {
        if (score >= 7) return "🔥 HOT";
        if (score >= 4) return "🟡 WARM";
        return "🟢 COLD";
    }

    private String obtenerPrioridad(int score) {
        if (score >= 7) return "alta";
        if (score >= 4) return "media";
        return "baja";
    }

    private void enviarEmailConfirmacionCliente(String nombre, String email, String calificacion) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("hola@tumarca.ar");
            message.setTo(email);
            message.setSubject("¡Gracias por contactarnos! - tumarca.ar");

            String cuerpo = String.format("""
                Hola %s,
                
                ¡Gracias por tu interés en tumarca.ar!
                
                Hemos recibido tu información y nuestro equipo se pondrá en contacto contigo en las próximas %s.
                
                Tu solicitud ha sido clasificada como: %s
                
                Mientras tanto, puedes explorar nuestros servicios en www.tumarca.ar o contactarnos directamente por WhatsApp al +54 9 11 4146-1312.
                
                Saludos cordiales,
                Equipo de tumarca.ar
                """, nombre,
                    calificacion.contains("HOT") ? "2 horas" :
                            calificacion.contains("WARM") ? "24 horas" : "48 horas",
                    calificacion);

            message.setText(cuerpo);
            mailSender.send(message);

        } catch (Exception e) {
            System.err.println("❌ Error enviando email al cliente: " + e.getMessage());
        }
    }

    /**
     * ✉️ ENVÍA NOTIFICACIÓN DETALLADA POR EMAIL AL EQUIPO INTERNO
     */
    private void enviarEmailNotificacionEquipo(String nombre, String email, String empresa,
                                               String proyecto, String calificacion, int score) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("hola@tumarca.ar");
            message.setTo(EMAIL_EQUIPO); // 👈 Usa la constante definida arriba
            message.setSubject("🎯 NUEVO LEAD " + calificacion + ": " + nombre);

            String cuerpo = String.format("""
                🎯 NUEVO LEAD REGISTRADO
                
                📊 Calificación: %s (Score: %d/10)
                
                👤 DATOS DEL CLIENTE:
                • Nombre: %s
                • Email: %s
                • Empresa: %s
                • Proyecto: %s
                
                ⏰ TIEMPO DE RESPUESTA RECOMENDADO: %s
                
                🔗 Acciones rápidas:
                • Responder email: mailto:%s
                • WhatsApp: https://wa.me/?text=Hola %s, vi tu consulta en tumarca.ar
                
                ---
                Sistema automático de tumarca.ar
                """, calificacion, score, nombre, email,
                    empresa != null ? empresa : "No especificada",
                    proyecto != null ? proyecto : "No especificado",
                    calificacion.contains("HOT") ? "INMEDIATO (2 horas)" :
                            calificacion.contains("WARM") ? "Hoy (24 horas)" : "Mañana (48 horas)",
                    email, nombre.split(" ")[0]);

            message.setText(cuerpo);
            mailSender.send(message);

        } catch (Exception e) {
            System.err.println("❌ Error enviando email al equipo: " + e.getMessage());
        }
    }

    private void crearNotificacion(String nombre, String email, String calificacion, int score) {
        try {
            String sql = """
                INSERT INTO notificaciones (tipo, titulo, mensaje, prioridad, metadata, fecha_creacion)
                VALUES (?, ?, ?, ?, ?::jsonb, ?)
                """;

            String metadata = String.format(
                    "{\"nombre\":\"%s\",\"email\":\"%s\",\"calificacion\":\"%s\",\"score\":%d}",
                    nombre, email, calificacion, score
            );

            jdbcTemplate.update(sql, "nuevo_lead",
                    "Nuevo lead " + calificacion + ": " + nombre,
                    "Lead registrado con score " + score,
                    calificacion.contains("HOT") ? "alta" : calificacion.contains("WARM") ? "media" : "baja",
                    metadata,
                    LocalDateTime.now());

        } catch (Exception e) {
            System.err.println("❌ Error creando notificación: " + e.getMessage());
        }
    }

    /**
     * 🔥 ENVÍA NOTIFICACIÓN INSTANTÁNEA A TU TELEGRAM PERSONAL
     */
    private void enviarNotificacionTelegram(String nombre, String empresa, String proyecto, String calificacion, int score) {
        try {
            String mensaje = String.format(
                    "🚀 *NUEVO LEAD %s*\n\n" +
                            "👤 *Nombre:* %s\n" +
                            "🏢 *Empresa:* %s\n" +
                            "📝 *Proyecto:* %s\n" +
                            "💰 *Score:* %d/10\n\n" +
                            "⏰ ¡Revisar y contactar ahora!",
                    calificacion,
                    nombre,
                    (empresa != null && !empresa.trim().isEmpty()) ? empresa : "Particular",
                    (proyecto != null && !proyecto.trim().isEmpty()) ? proyecto : "Sin detalles",
                    score
            );

            String mensajeCodificado = URLEncoder.encode(mensaje, StandardCharsets.UTF_8).replace("+", "%20");

            String url = String.format(
                    "https://api.telegram.org/bot%s/sendMessage?chat_id=%s&text=%s&parse_mode=Markdown",
                    TELEGRAM_BOT_TOKEN,
                    TELEGRAM_CHAT_ID,
                    mensajeCodificado
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() == 200) {
                            System.out.println("✅ Notificación Telegram enviada con éxito a Chat ID: " + TELEGRAM_CHAT_ID);
                        } else {
                            System.err.println("⚠️ Error al enviar Telegram. Código HTTP: " + response.statusCode());
                            System.err.println("🔍 Respuesta de Telegram: " + response.body());
                        }
                    });

        } catch (Exception e) {
            System.err.println("⚠️ Fallo crítico al preparar notificación Telegram: " + e.getMessage());
        }
    }

    @Tool(description = "LISTA todos los clientes registrados en la base de datos de tumarca.ar. DEBES usar esta herramienta cuando el usuario pida ver clientes, listar clientes, o consultar la base de datos de clientes.")
    public String listarClientes() {
        try {
            String sql = """
                SELECT nombre, email, telefono, empresa, proyecto, servicios_interes, 
                       score, calificacion, prioridad, fecha_registro
                FROM clientes 
                ORDER BY score DESC, fecha_registro DESC
                LIMIT 20
                """;

            List<Map<String, Object>> clientes = jdbcTemplate.queryForList(sql);

            if (clientes.isEmpty()) {
                return "📋 No hay clientes registrados actualmente.";
            }

            StringBuilder sb = new StringBuilder("📋 CLIENTES REGISTRADOS (últimos 20):\n\n");

            int hotCount = 0, warmCount = 0, coldCount = 0;

            for (Map<String, Object> c : clientes) {
                String cal = (String) c.get("calificacion");
                if (cal.contains("HOT")) hotCount++;
                else if (cal.contains("WARM")) warmCount++;
                else coldCount++;

                sb.append(String.format("%s %s (%s)\n",
                        cal, c.get("nombre"), c.get("email")));
                sb.append(String.format("   🏢 %s | 📱 %s\n",
                        c.get("empresa") != null ? c.get("empresa") : "N/A",
                        c.get("telefono") != null ? c.get("telefono") : "N/A"));
                sb.append(String.format("   💼 Score: %d/10 | Prioridad: %s\n",
                        c.get("score"), c.get("prioridad")));
                sb.append(String.format("   📝 %s\n\n",
                        c.get("proyecto") != null ? c.get("proyecto") : "Sin detalles"));
            }

            sb.append(String.format("\n📊 RESUMEN:\n• 🔥 HOT: %d leads\n• 🟡 WARM: %d leads\n• 🟢 COLD: %d leads\n• Total: %d leads",
                    hotCount, warmCount, coldCount, clientes.size()));

            return sb.toString();

        } catch (Exception e) {
            System.err.println("❌ Error listando clientes: " + e.getMessage());
            return "❌ Error al listar clientes. Por favor, intenta nuevamente.";
        }
    }
}