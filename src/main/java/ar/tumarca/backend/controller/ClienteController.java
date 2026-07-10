package ar.tumarca.backend.controller;

import ar.tumarca.backend.tools.ClienteTool;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ClienteController {

    private final ClienteTool clienteTool;

    public ClienteController(ClienteTool clienteTool) {
        this.clienteTool = clienteTool;
    }

    @GetMapping("/clientes")
    public String listarClientes() {
        return clienteTool.listarClientes();
    }

    @PostMapping("/clientes")
    public String registrarCliente(@RequestBody ClienteRequest request) {
        return clienteTool.registrarCliente(
                request.nombre(),
                request.email(),
                request.telefono(),
                request.empresa(),
                request.proyecto(),
                request.servicios_interes()
        );
    }

    public record ClienteRequest(
            String nombre,
            String email,
            String telefono,
            String empresa,
            String proyecto,
            String servicios_interes
    ) {}
}