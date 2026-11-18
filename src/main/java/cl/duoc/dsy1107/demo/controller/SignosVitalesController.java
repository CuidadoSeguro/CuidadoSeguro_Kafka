package cl.duoc.dsy1107.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.dsy1107.demo.model.SignoVital;
import cl.duoc.dsy1107.demo.service.SignosVitalesService;

@RestController
@RequestMapping("/api/signos-vitales")
public class SignosVitalesController {

    private final SignosVitalesService signosVitalesService;

    public SignosVitalesController(SignosVitalesService signosVitalesService) {
        this.signosVitalesService = signosVitalesService;
    }

    @PostMapping
    public ResponseEntity<SignoVital> registrar(@RequestBody SignoVital signoVital) {
        return ResponseEntity.accepted().body(signosVitalesService.publicar(signoVital));
    }
}
