package cl.duoc.dsy1107.demo.service;

import cl.duoc.dsy1107.demo.model.SignoVital;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

@Service 
public class EvaluadorSignosVitales {

    private static final double PS_MIN = 90.0;
    private static final double PS_MAX = 140.0;
    private static final double PD_MIN = 60.0;
    private static final double PD_MAX = 90.0;
    private static final double FC_MIN = 60.0;
    private static final double FC_MAX = 100.0;
    private static final double FR_MIN = 12.0;
    private static final double FR_MAX = 20.0;
    private static final double TEMP_MIN = 36.1;
    private static final double TEMP_MAX = 37.2;
    private static final double SPO2_MIN = 95.0;
    private static final double SPO2_MAX = 100.0;

    public EvaluacionSignos evaluar(List<SignoVital> ultimosTres) {
        if (ultimosTres == null || ultimosTres.isEmpty()) {
            return new EvaluacionSignos("Medicina General",
                    "No hay suficientes signos vitales para generar una sugerencia.", false);
        }

        int anomaliasCardio = 0;
        int anomaliasResp = 0;

        for (SignoVital s : ultimosTres) {
            if (fueraDeRango(s.presionSistolica(), PS_MIN, PS_MAX)
                    || fueraDeRango(s.presionDiastolica(), PD_MIN, PD_MAX)
                    || fueraDeRango(s.frecuenciaCardiaca(), FC_MIN, FC_MAX)) {
                anomaliasCardio++;
            }
            if (fueraDeRango(s.temperatura(), TEMP_MIN, TEMP_MAX)
                    || fueraDeRango(s.saturacionOxigeno(), SPO2_MIN, SPO2_MAX)
                    || fueraDeRango(s.frecuenciaRespiratoria(), FR_MIN, FR_MAX)) {
                anomaliasResp++;
            }
        }

        String especialista;
        String mensaje;
        boolean requiereAtencion;

        if (anomaliasCardio >= 2 && anomaliasResp >= 2) {
            especialista = "Cardiología y Neumología";
            requiereAtencion = true;
            mensaje = "Tus últimos 3 signos vitales presentan valores fuera de rango tanto "
                    + "cardiovasculares como respiratorios. Se recomienda valoración integral con "
                    + "cardiología y neumología.";
        } else if (anomaliasCardio >= 2) {
            especialista = "Cardiología";
            requiereAtencion = true;
            mensaje = "Tus últimos 3 signos vitales muestran valores de presión arterial o "
                    + "frecuencia cardíaca fuera de rango. Se recomienda una evaluación con "
                    + "cardiología.";
        } else if (anomaliasResp >= 2) {
            especialista = "Neumología";
            requiereAtencion = true;
            mensaje = "Tus últimos 3 signos vitales muestran valores de temperatura, saturación "
                    + "de oxígeno o frecuencia respiratoria fuera de rango. Se recomienda una "
                    + "evaluación con neumología.";
        } else if (anomaliasCardio == 1 || anomaliasResp == 1) {
            especialista = "Medicina General";
            requiereAtencion = false;
            mensaje = "Tus últimos 3 signos vitales presentan uno o más valores levemente fuera "
                    + "de rango. Se recomienda una valoración de medicina general para control y "
                    + "seguimiento.";
        } else {
            especialista = "Medicina General";
            requiereAtencion = false;
            mensaje = "Tus últimos 3 signos vitales se encuentran dentro de rangos normales. "
                    + "Se recomienda control rutinario con medicina general.";
        }

        return new EvaluacionSignos(especialista, mensaje, requiereAtencion);
    }

    private boolean fueraDeRango(Double valor, double min, double max) {
        return valor != null && (valor < min || valor > max);
    }
}
