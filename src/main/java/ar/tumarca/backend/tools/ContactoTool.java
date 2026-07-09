package ar.tumarca.backend.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

@Service
public class ContactoTool {

    @Tool(description = "OBTIENE información de contacto COMPLETA de tumarca.ar. DEBES usar esta herramienta cuando el usuario pregunte: cómo contactar, email, teléfono, WhatsApp, dirección, ubicación, horarios, o cualquier pregunta sobre comunicación. Devuelve todos los datos de contacto reales.")
    public String obtenerContacto() {
        return """
        📧 Email: hola@tumarca.ar
        📱 WhatsApp: +54 9 11 4146-1312
        📍 Ubicación: Buenos Aires, Argentina
        🌐 Web: www.tumarca.ar
        🕐 Horario de atención: Lunes a Viernes de 9:00 a 18:00 hs
        """;
    }

    @Tool(description = "OBTIENE el precio REAL de un servicio específico. DEBES usar esta herramienta cuando el usuario pregunte: precios, costos, tarifas, cuánto cuesta, presupuesto, o mencione cualquier servicio. Parámetros: servicio (web_basica, web_intermedia, web_avanzada, ecommerce, app_movil, branding, seo). Devuelve el precio exacto en USD.")
    public String obtenerPrecio(
            @ToolParam(description = "Tipo de servicio: web_basica, web_intermedia, web_avanzada, ecommerce, app_movil, branding, seo") String servicio
    ) {
        return switch (servicio.toLowerCase()) {
            case "web_basica" -> "💰 Web Básica (Landing Page): desde USD $300. Incluye diseño responsive, hasta 5 secciones, formulario de contacto.";
            case "web_intermedia" -> "💰 Web Intermedia (Corporativa): desde USD $800. Incluye hasta 10 páginas, blog, SEO básico, integración redes sociales.";
            case "web_avanzada" -> "💰 Web Avanzada (A medida): desde USD $1,500. Incluye funcionalidades personalizadas, panel de administración, integraciones API.";
            case "ecommerce" -> "💰 E-commerce: desde USD $2,000. Incluye catálogo de productos, carrito de compras, pasarela de pago, gestión de stock.";
            case "app_movil" -> "💰 App Móvil: desde USD $3,000. Incluye desarrollo para Android/iOS, diseño UI/UX, backend API.";
            case "branding" -> "💰 Branding: desde USD $400. Incluye logo, paleta de colores, tipografía, manual de marca.";
            case "seo" -> "💰 SEO Mensual: desde USD $200/mes. Incluye optimización on-page, keywords, reportes mensuales.";
            default -> "❓ Servicio no reconocido. Los servicios disponibles son: web_basica, web_intermedia, web_avanzada, ecommerce, app_movil, branding, seo";
        };
    }

    @Tool(description = "LISTA todos los servicios que ofrece tumarca.ar con descripciones detalladas. DEBES usar esta herramienta cuando el usuario pregunte: qué servicios ofrecen, qué hacen, en qué trabajan, o solicite una lista de servicios. Devuelve la lista completa de servicios disponibles.")
    public String listarServicios() {
        return """
        🎯 Servicios de tumarca.ar:
        
        1. 💻 Desarrollo Web
           - Landing Pages
           - Sitios Corporativos
           - Tiendas Online (E-commerce)
           - Sistemas Web a Medida
        
        2. 📱 Desarrollo de Apps Móviles
           - Android e iOS
           - Apps híbridas
           - Integración con APIs
        
        3. 🎨 Branding y Diseño
           - Logos e Identidad Visual
           - Diseño UI/UX
           - Material de Marketing
        
        4. 📈 Marketing Digital
           - SEO (Posicionamiento)
           - Gestión de Redes Sociales
           - Campañas Publicitarias
        
        5. 🤖 Soluciones con IA
           - Chatbots Inteligentes
           - Automatización de Procesos
           - Análisis de Datos
        
        6. 🔧 Soporte y Mantenimiento
           - Mantenimiento Web
           - Actualizaciones
           - Hosting y Dominio
        """;
    }



}