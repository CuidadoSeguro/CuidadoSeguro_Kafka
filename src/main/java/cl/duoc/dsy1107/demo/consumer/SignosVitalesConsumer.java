package cl.duoc.dsy1107.demo.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import cl.duoc.dsy1107.demo.config.KafkaConfig;
import cl.duoc.dsy1107.demo.model.SignoVital;
import cl.duoc.dsy1107.demo.service.EvaluadorSignosVitales;
import cl.duoc.dsy1107.demo.service.EvaluacionSignos;
import cl.duoc.dsy1107.demo.service.NotificacionCliente;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SignosVitalesConsumer {

    private static final Logger log = LoggerFactory.getLogger(SignosVitalesConsumer.class);

    private final ObjectMapper objectMapper;
    private final EvaluadorSignosVitales evaluador;
    private final NotificacionCliente notificacionCliente;

    private final Map<Long, List<SignoVital>> historialPorPaciente = new ConcurrentHashMap<>();

    public SignosVitalesConsumer(ObjectMapper objectMapper,
                                 EvaluadorSignosVitales evaluador,
                                 NotificacionCliente notificacionCliente) {
        this.objectMapper = objectMapper;
        this.evaluador = evaluador;
        this.notificacionCliente = notificacionCliente;
    }

    @KafkaListener(topics = KafkaConfig.TOPIC_SIGNOS_VITALES, groupId = "signos-vitales")
    public void procesar(ConsumerRecord<String, String> record) {
        try {
            SignoVital signo = objectMapper.readValue(record.value(), SignoVital.class);
            log.info("[LEE] grupo=signos-vitales key={} valor={}", record.key(), record.value());

            Long pacienteId = signo.pacienteId();
            if (pacienteId == null) {
                log.warn("[IGNORA] signo vital sin pacienteId");
                return;
            }

            List<SignoVital> historial = historialPorPaciente.computeIfAbsent(pacienteId,
                    k -> new ArrayList<>());
            historial.add(signo);
            if (historial.size() > 3) {
                historial.remove(0);
            }

            if (historial.size() == 3) {
                EvaluacionSignos evaluacion = evaluador.evaluar(historial);
                log.info("[EVALUA] paciente={} especialista={} requiereAtencion={}",
                        pacienteId, evaluacion.especialista(), evaluacion.requiereAtencion());

                notificacionCliente.enviarSugerenciaEspecialista(signo,
                        evaluacion.especialista(), evaluacion.mensaje());
            } else {
                log.info("[ESPERA] paciente={} signos registrados: {}/3",
                        pacienteId, historial.size());
            }
        } catch (Exception ex) {
            log.error("[ERROR] No se pudo procesar el mensaje: {}", ex.getMessage(), ex);
        }
    }
}
