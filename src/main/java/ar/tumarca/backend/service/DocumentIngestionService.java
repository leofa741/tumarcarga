package ar.tumarca.backend.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DocumentIngestionService {

    private final VectorStore vectorStore;

    @Value("classpath:documents/servicios.txt")
    private Resource serviciosDoc;

    @Value("classpath:documents/casos_exito.txt")
    private Resource casosExitoDoc;

    @Value("classpath:documents/sobre_nosotros.txt")
    private Resource sobreNosotrosDoc;

    public DocumentIngestionService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public void loadDocuments() {
        System.out.println("🔄 Cargando documentos en la base de datos vectorial...");

        loadDocument(serviciosDoc, "servicios");
        loadDocument(casosExitoDoc, "casos_exito");
        loadDocument(sobreNosotrosDoc, "sobre_nosotros");

        System.out.println("✅ Documentos cargados exitosamente!");
    }

    private void loadDocument(Resource resource, String source) {
        try {
            TextReader textReader = new TextReader(resource);
            textReader.getCustomMetadata().put("source", source);
            List<Document> documents = textReader.get();

            // ✅ CORRECTO (usa valores por defecto):
            TokenTextSplitter splitter = new TokenTextSplitter();
            List<Document> chunks = splitter.apply(documents);

            vectorStore.add(chunks);

            System.out.println("✅ Cargado: " + source + " (" + chunks.size() + " fragmentos)");
        } catch (Exception e) {
            System.err.println("❌ Error cargando " + source + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}