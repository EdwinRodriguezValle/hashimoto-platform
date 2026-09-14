package com.hashimoto.patient_service.api;

import com.hashimoto.patient_service.command.CreatePatientCommand;
import com.hashimoto.patient_service.query.FindAllPatientsQuery;
import com.hashimoto.patient_service.query.PatientView;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;


@RestController
@RequestMapping("/api/patients")
public class PatientControllerAPI {

    private final CommandGateway commandGateway;
    private final QueryGateway queryGateway; // <-- 1. INYECTAMOS EL GATEWAY DE CONSULTAS

    // Actualizamos el constructor
    public PatientControllerAPI(CommandGateway commandGateway, QueryGateway queryGateway) {
        this.commandGateway = commandGateway;
        this.queryGateway = queryGateway;
    }

    @PostMapping
    public CompletableFuture<String> createPatient(@RequestBody CreatePatientRequest request) {

        // Aquí puedes generar un UUID único para el paciente
        String patientId = java.util.UUID.randomUUID().toString();

        // Crear el comando con los datos del request
        CreatePatientCommand command = CreatePatientCommand.builder()
                .patientId(patientId)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .gender(request.getGender())
                .dni(request.getDni())
                .email(request.getEmail())
                .age(request.getAge())
                .build();

        // // Enviamos el comando de forma asíncrona a través de Axon
        return commandGateway.send(command);
    }

    // --- 2. IMPLEMENTAMOS EL ENDPOINT GET ---
    @GetMapping
    public CompletableFuture<List<PatientView>> getAllPatients() {
        // Despachamos la Query al bus de Axon esperando una lista de PatientView
        return queryGateway.query(
                new FindAllPatientsQuery(),
                ResponseTypes.multipleInstancesOf(PatientView.class)
        );
    }
}
