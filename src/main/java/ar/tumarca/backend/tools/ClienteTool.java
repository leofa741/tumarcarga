package ar.tumarca.backend.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

@Service
public class ClienteTool {

    private final JdbcTemplate jdbcTemplate;
    private final JavaMailSender mailSender;

    public ClienteTool(JdbcTemplate jdbcTemplate, JavaMailSender mailSender) {
        this.jdbcTemplate = jdbcTemplate;
        this.mailSender = mailSender;
    }

    @Tool(description = "REGISTRA un nuevo cliente potencial en el sistema de tumarca.ar. DEBES usar esta herramienta cuando el usuario proporcione sus datos de contacto (nombre, email, teléfono) o exprese interés en contratar servicios. Captura: nombre, email, telefono, empresa, proyecto, servicios_interes. Devuelve confirmación del registro.")
    public String registrarCliente(
            @ToolParam(description = "Nombre completo del cliente") String nombre,
            @ToolParam(description = "Email del cliente") String email,
            @ToolParam(description = "Teléfono del cliente (opcional)", required = false) String telefono,
            @ToolParam(description = "Nombre de la empresa (opcional)", required = false) String empresa,
            @ToolParam(description = "Descripción del proyecto o necesidad (opcional)", required = false) String proyecto,
            @ToolParam(description = "Servicios de interés separados por coma: web_basica, web_intermedia, web_avanzada, ecommerce, app_movil, branding, seo, marketing_digital, ia (opcional)", required = false) String servicios_interes
    ) {
        try {
            // Convertir servicios a array
            String[] serviciosArray = null;
            if (servicios_interes != null && !servicios_interes.isEmpty()) {
                serviciosArray = servicios_interes.split(",");
                for (int i = 0; i < serviciosArray.length; i++) {
                    serviciosArray[i] = serviciosArray[i].trim();
                }
            }

            // Insertar en base de datos
            String sql = "INSERT INTO clientes (nombre, email, telefono, empresa, proyecto, servicios_interes) VALUES (?, ?, ?, ?, ?, ?)";
            jdbcTemplate.update(sql, nombre, email, telefono, empresa, proyecto, serviciosArray);

            // Obtener ID del cliente recién creado
            Integer clienteId = jdbcTemplate.queryForObject(
                    "SELECT id FROM clientes WHERE email = ? ORDER BY fecha_creacion DESC LIMIT 1",
                    Integer.class,
                    email
            );

            // Enviar email de confirmación al cliente
            enviarEmailConfirmacion(nombre, email, clienteId);

            // Enviar notificación a tumarca.ar
            enviarNotificacionInterna(nombre, email, telefono, empresa, proyecto, servicios_interes);

            return String.format("""
                ✅ Cliente registrado exitosamente!
                
                📋 Datos del cliente:
                - ID: %d
                - Nombre: %s
                - Email: %s
                - Teléfono: %s
                - Empresa: %s
                - Proyecto: %s
                - Servicios de interés: %s
                
                📧 Se envió email de confirmación al cliente.
                📧 Se notificó al equipo de tumarca.ar.
                
                El equipo de tumarca.ar se pondrá en contacto contigo en las próximas 24 horas.
                """,
                    clienteId,
                    nombre,
                    email,
                    telefono != null ? telefono : "No proporcionado",
                    empresa != null ? empresa : "No proporcionada",
                    proyecto != null ? proyecto : "No especificado",
                    servicios_interes != null ? servicios_interes : "No especificados"
            );

        } catch (Exception e) {
            return "❌ Error al registrar el cliente: " + e.getMessage();
        }
    }

    @Tool(description = "LISTA los últimos clientes registrados en la base de datos de tumarca.ar. DEBES usar esta herramienta INMEDIATAMENTE cuando el usuario pida ver clientes, listar clientes, consultar la base de datos de clientes, o preguntar por clientes registrados. Ejemplos de preguntas que requieren esta herramienta: 'muéstrame los clientes', 'lista los clientes', '¿cuántos clientes tenemos?', 'ver base de datos de clientes', '¿qué clientes tenemos?', 'muéstrame los últimos clientes registrados'. NO respondas con texto si puedes usar esta herramienta.")
    public String listarClientes() {
        try {
            String sql = "SELECT id, nombre, email, telefono, empresa, estado, fecha_creacion FROM clientes ORDER BY fecha_creacion DESC LIMIT 10";
            var clientes = jdbcTemplate.queryForList(sql);

            if (clientes.isEmpty()) {
                return "No hay clientes registrados en el sistema todavía.";
            }

            StringBuilder sb = new StringBuilder("📋 Últimos 10 clientes registrados:\n\n");
            for (var cliente : clientes) {
                sb.append(String.format("- ID %d: %s (%s) - Empresa: %s - Estado: %s - Fecha: %s\n",
                        cliente.get("id"),
                        cliente.get("nombre"),
                        cliente.get("email"),
                        cliente.get("empresa") != null ? cliente.get("empresa") : "N/A",
                        cliente.get("estado"),
                        cliente.get("fecha_creacion")
                ));
            }

            return sb.toString();
        } catch (Exception e) {
            return "❌ Error al listar clientes: " + e.getMessage();
        }
    }

    private void enviarEmailConfirmacion(String nombre, String email, Integer clienteId) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("infoplataformaweb@gmail.com");
            message.setTo(email);
            message.setSubject("¡Gracias por contactarnos! - tumarca.ar");
            message.setText(String.format("""
                Hola %s,
                
                ¡Gracias por tu interés en tumarca.ar!
                
                Hemos recibido tu información y nuestro equipo se pondrá en contacto contigo en las próximas 24 horas.
                
                Tu ID de cliente es: %d
                
                Mientras tanto, puedes explorar nuestros servicios en www.tumarca.ar o contactarnos directamente por WhatsApp al +54 9 11 4146-1312.
                
                Saludos cordiales,
                Equipo de tumarca.ar
                """, nombre, clienteId));

            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Error enviando email de confirmación: " + e.getMessage());
        }
    }

    private void enviarNotificacionInterna(String nombre, String email, String telefono,
                                           String empresa, String proyecto, String servicios) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("infoplataformaweb@gmail.com");
            message.setTo("infoplataformaweb@gmail.com");
            message.setSubject("🆕 Nuevo cliente potencial: " + nombre);
            message.setText(String.format("""
                Nuevo cliente registrado en el sistema:
                
                Nombre: %s
                Email: %s
                Teléfono: %s
                Empresa: %s
                Proyecto: %s
                Servicios de interés: %s
                Fecha: %s
                
                Por favor, contactar al cliente en las próximas 24 horas.
                """,
                    nombre, email,
                    telefono != null ? telefono : "No proporcionado",
                    empresa != null ? empresa : "No proporcionada",
                    proyecto != null ? proyecto : "No especificado",
                    servicios != null ? servicios : "No especificados",
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
            ));

            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Error enviando notificación interna: " + e.getMessage());
        }
    }
}