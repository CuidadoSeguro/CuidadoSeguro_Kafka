package cl.duoc.dsy1107.demo.model;

public record SignoVital(
        Long pacienteId,
        String nombrePaciente,
        String correoPaciente,
        Double presionSistolica,
        Double presionDiastolica,
        Double frecuenciaCardiaca,
        Double frecuenciaRespiratoria,
        Double temperatura,
        Double saturacionOxigeno,
        String fechaHora
) {
}
