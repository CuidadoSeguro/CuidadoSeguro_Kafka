package cl.duoc.dsy1107.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;

import cl.duoc.dsy1107.demo.model.SignoVital;

import java.util.Map;

@Service
public class NotificacionCliente {

    private final RestClient correoClient;

    public NotificacionCliente(RestClient.Builder builder,
                               @Value("${notificacion.service.base-url:http://localhost:8089}") String baseUrl) {
        this.correoClient = builder.baseUrl(baseUrl).build();
    }

    public void enviarSugerenciaEspecialista(SignoVital signoVital, String especialista,
                                             String mensaje) {
        try {
            correoClient.post()
                    .uri("/api/correos/enviar")
                    .body(Map.of(
                            "destinatario", signoVital.correoPaciente(),
                            "asunto", "Cuidado Seguro - Sugerencia de especialista",
                            "mensaje", "Hola " + (signoVital.nombrePaciente() != null
                                    ? signoVital.nombrePaciente() : "paciente")
                                    + ".\n\n" + mensaje + "\n\nEspecialista sugerido: "
                                    + especialista + ".\n\nEste mensaje se generó automáticamente "
                                    + "a partir de tus últimos 3 signos vitales registrados."))
                    .retrieve()
                    .body(Void.class);
        } catch (RestClientResponseException ex) {
            throw new RuntimeException("El servicio de notificaciones no aceptó la solicitud: "
                    + ex.getResponseBodyAsString(), ex);
        } catch (ResourceAccessException ex) {
            throw new RuntimeException("El servicio de notificaciones no está disponible", ex);
        }
    }
}
