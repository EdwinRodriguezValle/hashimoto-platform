package com.hashimoto.patient_service.query;

import com.hashimoto.patient_service.event.PatientCreatedEvent;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PatientEventHandler {
    private PatientViewRepository patientViewRepository;

    public PatientEventHandler(PatientViewRepository patientViewRepository) {
        this.patientViewRepository = patientViewRepository;
    }

    // Axon detecta esta anotación y le envía el evento automáticamente en cuanto ocurre
    @EventHandler
    public void on(PatientCreatedEvent event) {
        System.out.println("CARA DE QUERY: Escuchando evento de Edwin para guardarlo en la tabla de lectura");
        PatientView patientView = new PatientView(
                event.getPatientId(),
                event.getFirstName(),
                event.getLastName(),
                event.getGender(),
                event.getDni(),
                event.getEmail(),
                event.getAge()
        );
        patientViewRepository.save(patientView);
    }

    @QueryHandler
    public List<PatientView> handle(FindAllPatientsQuery query) {
        System.out.println("CARA DE QUERY: Buscando todos los pacientes en la tabla de lectura");
        return patientViewRepository.findAll();
    }

}