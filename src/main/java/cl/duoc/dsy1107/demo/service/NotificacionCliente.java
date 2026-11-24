package cl.duoc.dsy1107.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;

import cl.duoc.dsy1107.demo.model.SignoVital;

import java.util.Map;

@Service
public class NotificacionCliente {

    private static final Logger log = LoggerFactory.getLogger(NotificacionCliente.class);
    private final RestClient correoClient;

    public NotificacionCliente(RestClient.Builder builder,
                               @Value("${notificacion.service.base-url:http://localhost:8089}") String baseUrl) {
        this.correoClient = builder.baseUrl(baseUrl).build();
        log.info("[NOTIFICACION-CONFIG] baseUrl={}", baseUrl);
    }

    public void enviarSugerenciaEspecialista(SignoVital signoVital, String especialista,
                                             String mensaje, boolean requiereAtencion) {
        String destinatario = signoVital.correoPaciente();
        if (destinatario == null || destinatario.isBlank()) {
            throw new IllegalArgumentException(
                    "El evento no contiene un correo de paciente válido (correoPaciente).");
        }

        try {
            Map<String, String> solicitud = Map.of(
                    "destinatario", destinatario.trim(),
                    "asunto", requiereAtencion
                            ? "Cuidado Seguro - Revisión de signos vitales"
                            : "Cuidado Seguro - Resumen de signos vitales",
                    "mensaje", "Hola " + (signoVital.nombrePaciente() != null
                            ? signoVital.nombrePaciente() : "paciente")
                            + ".\n\n" + mensaje + "\n\nEspecialista sugerido: "
                            + especialista + ".\n\nEste mensaje se generó automáticamente "
                            + "a partir de tus últimos 3 signos vitales registrados.");

            Map<?, ?> respuesta = correoClient.post()
                    .uri("/api/correos/enviar")
                    .body(solicitud)
                    .retrieve()
                    .body(Map.class);

            log.info("[NOTIFICATION-HTTP-OK] destinatario={} respuesta={}",
                    destinatario, respuesta);
        } catch (RestClientResponseException ex) {
            throw new RuntimeException("Notification Service respondió HTTP "
                    + ex.getStatusCode().value() + ": " + ex.getResponseBodyAsString(), ex);
        } catch (ResourceAccessException ex) {
            throw new RuntimeException("No se pudo conectar con Notification Service. "
                    + "Verifica NOTIFICACION_SERVICE_URL=" + ex.getMessage(), ex);
        }
    }
}
