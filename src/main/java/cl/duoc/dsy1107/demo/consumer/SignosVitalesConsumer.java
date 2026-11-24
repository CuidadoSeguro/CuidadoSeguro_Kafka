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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SignosVitalesConsumer {

    private static final Logger log = LoggerFactory.getLogger(SignosVitalesConsumer.class);

    private final ObjectMapper objectMapper;
    private final EvaluadorSignosVitales evaluador;
    private final NotificacionCliente notificacionCliente;

    // Acumula lotes de tres registros por paciente mientras el consumidor permanece activo.
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
        SignoVital signo;
        try {
            signo = objectMapper.readValue(record.value(), SignoVital.class);
        } catch (Exception ex) {
            log.error("[ERROR-DESERIALIZACION] topic={} partition={} offset={} causa={}",
                    record.topic(), record.partition(), record.offset(), ex.getMessage(), ex);
            throw new IllegalArgumentException("Mensaje de signos vitales inválido", ex);
        }

        if (signo.pacienteId() == null) {
            log.warn("[IGNORA] topic={} offset={} evento sin pacienteId",
                    record.topic(), record.offset());
            return;
        }

        if (signo.correoPaciente() == null || signo.correoPaciente().isBlank()) {
            log.error("[ERROR-CORREO] pacienteId={} signoId={} no contiene correoPaciente",
                    signo.pacienteId(), record.offset());
            throw new IllegalArgumentException(
                    "El evento de signos vitales no contiene correoPaciente");
        }

        List<SignoVital> historial = historialPorPaciente.computeIfAbsent(
                signo.pacienteId(), id -> new ArrayList<>());

        synchronized (historial) {
            historial.add(signo);
            log.info("[KAFKA-RECIBIDO] pacienteId={} signoId={} topic={} partition={} offset={} lote={}/3",
                    signo.pacienteId(), record.offset(), record.topic(),
                    record.partition(), record.offset(), historial.size());

            if (historial.size() < 3) {
                log.info("[ESPERA-3-SIGNOS] pacienteId={} registrosAcumulados={}/3",
                        signo.pacienteId(), historial.size());
                return;
            }

            List<SignoVital> tresRegistros = List.copyOf(historial.subList(0, 3));
            EvaluacionSignos evaluacion = evaluador.evaluar(tresRegistros);

            log.info("[EVALUACION-3-SIGNOS] pacienteId={} especialista={} requiereAtencion={} ",
                    signo.pacienteId(), evaluacion.especialista(), evaluacion.requiereAtencion());

            try {
                // Se envía después de cada lote completo de tres signos vitales.
                notificacionCliente.enviarSugerenciaEspecialista(
                        signo, evaluacion.especialista(), evaluacion.mensaje(),
                        evaluacion.requiereAtencion());

                historial.subList(0, 3).clear();
                log.info("[CORREO-SOLICITADO-OK] pacienteId={} destinatario={} loteProcesado=3",
                        signo.pacienteId(), signo.correoPaciente());
            } catch (RuntimeException ex) {
                // No limpiar el lote si el servicio de correo falla.
                log.error("[ERROR-ENVIO-CORREO] pacienteId={} destinatario={} causa={}",
                        signo.pacienteId(), signo.correoPaciente(), ex.getMessage(), ex);
                throw ex;
            }
        }
    }
}
