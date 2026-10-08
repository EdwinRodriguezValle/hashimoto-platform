package com.hashimoto.patient_service.api;

import com.hashimoto.patient_service.command.CreatePatientCommand;
import com.hashimoto.patient_service.query.FindAllPatientsQuery;
import com.hashimoto.patient_service.query.PatientView;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;


@RestController
@RequestMapping("/api/patients")
public class PatientControllerAPI {

    private final CommandGateway commandGateway;
    private final QueryGateway queryGateway;

    public PatientControllerAPI(CommandGateway commandGateway,
                               QueryGateway queryGateway) {
        this.commandGateway = commandGateway;
        this.queryGateway = queryGateway;
    }

    @PostMapping
   // @PreAuthorize("hasRole('ROLE_ADMIN')") // <-- 2. PROTEGEMOS EL ENDPOINT PARA SOLO ADMIN
    public CompletableFuture<String> createPatient(@RequestBody CreatePatientRequest request) {
        String patientId = java.util.UUID.randomUUID().toString();

        CreatePatientCommand command = CreatePatientCommand.builder()
                .patientId(patientId)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .gender(request.getGender())
                .dni(request.getDni())
                .email(request.getEmail())
                .age(request.getAge())
                .build();

        return commandGateway.send(command);
    }

    @GetMapping
    public CompletableFuture<List<PatientView>> getAllPatients() {
        return queryGateway.query(
                new FindAllPatientsQuery(),
                ResponseTypes.multipleInstancesOf(PatientView.class)
        );
    }
}
