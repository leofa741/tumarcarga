package ar.tumarca.backend.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

@Service
public class DocumentIngestionService {

    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;

    @Value("classpath:documents/servicios.txt")
    private Resource serviciosDoc;

    @Value("classpath:documents/casos_exito.txt")
    private Resource casosExitoDoc;

    @Value("classpath:documents/sobre_nosotros.txt")
    private Resource sobreNosotrosDoc;

    @Value("classpath:documents/faq.txt")
    private Resource faqDoc;

    @Value("classpath:documents/testimonios.txt")
    private Resource testimoniosDoc;

    @Value("classpath:documents/tecnologias.txt")
    private Resource tecnologiasDoc;

    @Value("classpath:documents/metodologia.txt")
    private Resource metodologiaDoc;

    @Value("classpath:documents/precios.txt")
    private Resource preciosDoc;

    public DocumentIngestionService(VectorStore vectorStore, JdbcTemplate jdbcTemplate) {
        this.vectorStore = vectorStore;
        this.jdbcTemplate = jdbcTemplate;
    }

    public void loadDocuments() {
        System.out.println("🔄 Iniciando carga de documentos...");

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM vector_store",
                Integer.class
        );

        if (count != null && count > 0) {
            System.out.println("⚠️ Ya hay " + count + " documentos. Saltando carga.");
            System.out.println("💡 Para recargar: DELETE FROM vector_store;");
            return;
        }

        // Cargar cada documento con su estrategia de división
        loadDocument(serviciosDoc, "servicios", "catalogo", 5, "section");
        loadDocument(casosExitoDoc, "casos_exito", "experiencia", 5, "section");
        loadDocument(sobreNosotrosDoc, "sobre_nosotros", "empresa", 4, "section");
        loadDocument(faqDoc, "faq", "preguntas_frecuentes", 5, "qa");
        loadDocument(testimoniosDoc, "testimonios", "prueba_social", 3, "testimonial");
        loadDocument(tecnologiasDoc, "tecnologias", "stack_tecnico", 4, "category");
        loadDocument(metodologiaDoc, "metodologia", "proceso", 4, "step");
        loadDocument(preciosDoc, "precios", "comercial", 5, "price_section");

        Integer newCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM vector_store",
                Integer.class
        );
        System.out.println("✅ Carga completa! Total fragmentos: " + newCount);
    }

    /**
     * Carga un documento dividiéndolo inteligentemente por secciones
     *
     * @param resource Archivo a cargar
     * @param source Nombre del documento
     * @param category Categoría para filtrado
     * @param priority Prioridad (1-5)
     * @param splitType Tipo de división: "section", "qa", "testimonial", "category", "step", "price_section"
     */
    private void loadDocument(Resource resource, String source, String category,
                              int priority, String splitType) {
        try {
            String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            // Dividir según el tipo de documento
            List<String> sections = splitContent(content, splitType);

            // Crear Documents con metadata
            List<Document> chunks = new ArrayList<>();
            for (int i = 0; i < sections.size(); i++) {
                String section = sections.get(i).trim();
                if (section.isEmpty()) continue;

                Map<String, Object> metadata = new HashMap<>();
                metadata.put("source", source);
                metadata.put("category", category);
                metadata.put("priority", priority);
                metadata.put("chunk_index", i);
                metadata.put("total_chunks", sections.size());
                metadata.put("split_type", splitType);

                chunks.add(new Document(section, metadata));
            }

            // Guardar en vector store
            if (!chunks.isEmpty()) {
                vectorStore.add(chunks);
                System.out.println("✅ Cargado: " + source +
                        " (" + chunks.size() + " fragmentos, categoría: " + category +
                        ", prioridad: " + priority + ", tipo: " + splitType + ")");
            }

        } catch (IOException e) {
            System.err.println("❌ Error cargando " + source + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Divide el contenido según el tipo de documento
     */
    private List<String> splitContent(String content, String splitType) {
        return switch (splitType) {
            case "qa" -> splitFAQ(content);
            case "testimonial" -> splitTestimonials(content);
            case "category" -> splitByCategory(content);
            case "step" -> splitByStep(content);
            case "price_section" -> splitPriceSections(content);
            default -> splitBySection(content);
        };
    }

    /**
     * FAQ: Divide por pares pregunta/respuesta
     * Patrón: texto seguido de "?" hasta la siguiente pregunta o final
     */
    private List<String> splitFAQ(String content) {
        List<String> sections = new ArrayList<>();
        // Divide cuando encuentra una línea que termina con "?" seguida de texto
        Pattern pattern = Pattern.compile("([\\s\\S]*?\\?)\\s*\\n(?=[^\\n]+\\?)", Pattern.MULTILINE);
        String[] parts = pattern.split(content);

        for (String part : parts) {
            if (part.trim().length() > 30) {  // Solo si tiene contenido real
                sections.add(part.trim());
            }
        }

        // Si no funcionó, dividir por doble salto de línea
        if (sections.size() <= 1) {
            sections = splitByDoubleNewline(content);
        }

        return sections;
    }

    /**
     * Testimonios: Divide por cliente (cada testimonio empieza con un nombre)
     */
    private List<String> splitTestimonials(String content) {
        List<String> sections = new ArrayList<>();
        // Divide por líneas que contienen "-" seguidas de texto largo (formato de testimonio)
        Pattern pattern = Pattern.compile("(?m)^[A-ZÁÉÍÓÚ][a-záéíóúñ]+ [A-ZÁÉÍÓÚ][a-záéíóúñ]+ - .*?:");
        String[] parts = pattern.split(content);
        String[] headers = pattern.split("");

        // Combinar headers con contenido
        Matcher matcher = pattern.matcher(content);
        List<String> allHeaders = new ArrayList<>();
        while (matcher.find()) {
            allHeaders.add(matcher.group());
        }

        for (int i = 0; i < allHeaders.size(); i++) {
            String section = allHeaders.get(i);
            if (i + 1 < parts.length) {
                section += parts[i + 1];
            }
            if (section.trim().length() > 50) {
                sections.add(section.trim());
            }
        }

        if (sections.isEmpty()) {
            sections = splitByDoubleNewline(content);
        }

        return sections;
    }

    /**
     * Tecnologías: Divide por categorías (FRONTEND:, BACKEND:, etc.)
     */
    private List<String> splitByCategory(String content) {
        List<String> sections = new ArrayList<>();
        Pattern pattern = Pattern.compile("(?m)^[A-ZÁÉÍÓÚ ]+:\\s*$");

        String[] parts = pattern.split(content);
        Matcher matcher = pattern.matcher(content);
        List<String> allHeaders = new ArrayList<>();
        while (matcher.find()) {
            allHeaders.add(matcher.group());
        }

        for (int i = 0; i < allHeaders.size(); i++) {
            String section = allHeaders.get(i);
            if (i + 1 < parts.length) {
                section += parts[i + 1];
            }
            if (section.trim().length() > 50) {
                sections.add(section.trim());
            }
        }

        if (sections.isEmpty()) {
            sections = splitByDoubleNewline(content);
        }

        return sections;
    }

    /**
     * Metodología: Divide por pasos (PASO 1:, PASO 2:, etc.)
     */
    private List<String> splitByStep(String content) {
        List<String> sections = new ArrayList<>();
        Pattern pattern = Pattern.compile("(?m)^PASO \\d+:");

        String[] parts = pattern.split(content);
        Matcher matcher = pattern.matcher(content);
        List<String> allHeaders = new ArrayList<>();
        while (matcher.find()) {
            allHeaders.add(matcher.group());
        }

        // Primera parte (antes de PASO 1)
        if (parts.length > 0 && parts[0].trim().length() > 50) {
            sections.add(parts[0].trim());
        }

        // Pasos
        for (int i = 0; i < allHeaders.size(); i++) {
            String section = allHeaders.get(i);
            if (i + 1 < parts.length) {
                section += parts[i + 1];
            }
            if (section.trim().length() > 50) {
                sections.add(section.trim());
            }
        }

        if (sections.size() <= 1) {
            sections = splitByDoubleNewline(content);
        }

        return sections;
    }

    /**
     * Precios: Divide por secciones separadas por ═══
     */
    private List<String> splitPriceSections(String content) {
        List<String> sections = new ArrayList<>();
        Pattern pattern = Pattern.compile("═{10,}");
        String[] parts = pattern.split(content);

        for (String part : parts) {
            if (part.trim().length() > 50) {
                sections.add(part.trim());
            }
        }

        if (sections.size() <= 1) {
            sections = splitByDoubleNewline(content);
        }

        return sections;
    }

    /**
     * Genérico: Divide por dobles saltos de línea
     */
    private List<String> splitBySection(String content) {
        return splitByDoubleNewline(content);
    }

    /**
     * Divide por doble salto de línea (estrategia por defecto)
     */
    private List<String> splitByDoubleNewline(String content) {
        List<String> sections = new ArrayList<>();
        String[] parts = content.split("\\n\\n+");

        for (String part : parts) {
            if (part.trim().length() > 50) {  // Solo secciones con contenido real
                sections.add(part.trim());
            }
        }

        // Si solo hay una sección grande, dividir por salto de línea simple
        if (sections.size() == 1 && sections.get(0).length() > 1500) {
            sections.clear();
            parts = content.split("\\n+");
            StringBuilder current = new StringBuilder();

            for (String part : parts) {
                if (current.length() + part.length() > 800) {
                    if (current.length() > 50) {
                        sections.add(current.toString().trim());
                    }
                    current = new StringBuilder();
                }
                current.append(part).append("\n");
            }
            if (current.length() > 50) {
                sections.add(current.toString().trim());
            }
        }

        return sections;
    }
}