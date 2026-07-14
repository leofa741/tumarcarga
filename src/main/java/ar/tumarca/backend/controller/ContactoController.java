package ar.tumarca.backend.controller;

import ar.tumarca.backend.tools.ContactoTool;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ContactoController {

    private final ContactoTool contactoTool;

    public ContactoController(ContactoTool contactoTool) {
        this.contactoTool = contactoTool;
    }

    @GetMapping("/contacto")
    public ResponseEntity<String> obtenerContacto() {
        return ResponseEntity.ok(contactoTool.obtenerContacto());
    }

    @GetMapping("/precios")
    public ResponseEntity<String> obtenerTodosLosPrecios() {
        return ResponseEntity.ok("""
            💰 PRECIOS DE TUMARCA.AR:
            
            1. 💻 Web Básica (Landing Page): desde USD $300
            2. 💻 Web Intermedia (Corporativa): desde USD $800
            3. 💻 Web Avanzada (A medida): desde USD $1,500
            4. 🛒 E-commerce: desde USD $2,000
            5. 📱 App Móvil: desde USD $3,000
            6. 🎨 Branding: desde USD $400
            7. 📈 SEO Mensual: desde USD $200/mes
            
            📱 WhatsApp: +54 9 11 4146-1312
            """);
    }

    @GetMapping("/precio/{servicio}")
    public ResponseEntity<String> obtenerPrecio(@PathVariable String servicio) {
        return ResponseEntity.ok(contactoTool.obtenerPrecio(servicio));
    }

    @GetMapping("/servicios")
    public ResponseEntity<String> listarServicios() {
        return ResponseEntity.ok(contactoTool.listarServicios());
    }
}