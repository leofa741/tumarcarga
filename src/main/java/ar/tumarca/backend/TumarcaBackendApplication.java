package ar.tumarca.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TumarcaBackendApplication {

    public static void main(String[] args) {
        // System.out.println("GEMINI_API_KEY=" + System.getenv("GEMINI_API_KEY"));
      //  System.out.println("GROQ_API_KEY=" + System.getenv("GROQ_API_KEY"));


        SpringApplication.run(TumarcaBackendApplication.class, args);
    }

}
