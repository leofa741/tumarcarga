package ar.tumarca.backend.controller;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Controller {

    @GetMapping("/hello")
    public String hello() {
        return "¡Hola desde tumarca.ar backend! 🚀";
    }


}
