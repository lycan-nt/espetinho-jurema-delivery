package br.com.delivere.acai.gestaoapp;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * API para o app consultar resumos de gestão no MongoDB (com histórico por data e loja).
 */
@RestController
@RequestMapping("/api/gestao-app")
@CrossOrigin(origins = "http://localhost:4200")
@ConditionalOnBean(MongoTemplate.class)
public class GestaoAppController {

    private static final String COLLECTION = "resumo_gestao";

    private final MongoTemplate mongoTemplate;

    public GestaoAppController(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Retorna o resumo de uma loja em uma data.
     * idLoja obrigatório. data opcional (padrão: hoje).
     */
    @GetMapping("/resumo")
    public ResponseEntity<ResumoGestaoDocument> resumo(
            @RequestParam String idLoja,
            @RequestParam(required = false) String data) {
        LocalDate d = (data != null && !data.isBlank())
                ? LocalDate.parse(data)
                : LocalDate.now();
        String docId = ResumoGestaoDocument.buildId(d, idLoja.trim());
        ResumoGestaoDocument doc = mongoTemplate.findById(docId, ResumoGestaoDocument.class, COLLECTION);
        if (doc == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(doc);
    }

    /**
     * Lista resumos com filtros opcionais: idLoja, data (dia), dataInicio e dataFim (período).
     */
    @GetMapping("/resumos")
    public List<ResumoGestaoDocument> resumos(
            @RequestParam(required = false) String idLoja,
            @RequestParam(required = false) String data,
            @RequestParam(required = false) String dataInicio,
            @RequestParam(required = false) String dataFim) {
        Query q = new Query();
        if (idLoja != null && !idLoja.isBlank()) {
            q.addCriteria(Criteria.where("idLoja").is(idLoja.trim()));
        }
        if (data != null && !data.isBlank()) {
            q.addCriteria(Criteria.where("data").is(LocalDate.parse(data)));
        } else {
            if (dataInicio != null && !dataInicio.isBlank()) {
                q.addCriteria(Criteria.where("data").gte(LocalDate.parse(dataInicio)));
            }
            if (dataFim != null && !dataFim.isBlank()) {
                q.addCriteria(Criteria.where("data").lte(LocalDate.parse(dataFim)));
            }
        }
        q.with(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "data"));
        return mongoTemplate.find(q, ResumoGestaoDocument.class, COLLECTION);
    }
}
