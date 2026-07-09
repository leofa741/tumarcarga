package ar.tumarca.backend.config;

import ar.tumarca.backend.service.DocumentIngestionService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final DocumentIngestionService documentIngestionService;

    public DataInitializer(DocumentIngestionService documentIngestionService) {
        this.documentIngestionService = documentIngestionService;
    }

    @Override
    public void run(String... args) {
        System.out.println("🚀 Iniciando carga de documentos...");
        documentIngestionService.loadDocuments();
    }
}