package cl.duoc.dsy1107.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import cl.duoc.dsy1107.demo.config.KafkaConfig;
import cl.duoc.dsy1107.demo.model.SignoVital;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class SignosVitalesService {

    private static final Logger log = LoggerFactory.getLogger(SignosVitalesService.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public SignosVitalesService(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = new ObjectMapper();
    }

    public SignoVital publicar(SignoVital signoVital) {
        String pacienteId = signoVital.pacienteId() == null ? "desconocido"
                : signoVital.pacienteId().toString();
        try {
            String valor = objectMapper.writeValueAsString(signoVital);
            log.info("[PUBLICA] topic={} key={} valor={}", KafkaConfig.TOPIC_SIGNOS_VITALES,
                    pacienteId, valor);
            kafkaTemplate.send(KafkaConfig.TOPIC_SIGNOS_VITALES, pacienteId, valor);
        } catch (Exception ex) {
            log.error("[ERROR] No se pudo publicar el signo vital: {}", ex.getMessage());
        }
        return signoVital;
    }
}
