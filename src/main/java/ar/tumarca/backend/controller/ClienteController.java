package ar.tumarca.backend.controller;

import ar.tumarca.backend.tools.ClienteTool;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ClienteController {

    private final ClienteTool clienteTool;

    public ClienteController(ClienteTool clienteTool) {
        this.clienteTool = clienteTool;
    }


    @PostMapping("/clientes")
    public ResponseEntity<?> registrarCliente(@Valid @RequestBody ClienteRequest request) {
        try {
            String resultado = clienteTool.registrarCliente(
                    request.nombre(),
                    request.email(),
                    request.telefono(),
                    request.empresa(),
                    request.proyecto(),
                    request.servicios_interes()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(resultado);
        } catch (Exception e) {
            System.err.println("❌ Error al registrar cliente: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al registrar cliente", "details", "Ocurrió un error interno"));
        }
    }

    // Manejador de errores de validación (¡Excelente práctica!)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return ResponseEntity.badRequest().body(errors);
    }

    // Record con validaciones estrictas (¡Perfecto!)
    public record ClienteRequest(
            @NotBlank(message = "El nombre es obligatorio")
            @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
            String nombre,

            @NotBlank(message = "El email es obligatorio")
            @Email(message = "El formato del email es inválido")
            String email,

            @Pattern(regexp = "^[0-9+\\-\\s()]*$", message = "El teléfono solo puede contener números, espacios, +, - y paréntesis")
            @Size(max = 20, message = "El teléfono no puede tener más de 20 caracteres")
            String telefono,

            @Size(max = 100, message = "La empresa no puede tener más de 100 caracteres")
            String empresa,

            @Size(max = 1000, message = "El proyecto no puede tener más de 1000 caracteres") // Aumentado un poco para proyectos detallados
            String proyecto,

            @Size(max = 200, message = "Los servicios de interés no pueden tener más de 200 caracteres")
            String servicios_interes
    ) {}
}