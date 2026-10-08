package com.hashimoto.patient_service.api;

import com.hashimoto.patient_service.command.CreatePatientCommand;
import com.hashimoto.patient_service.query.FindAllPatientsQuery;
import com.hashimoto.patient_service.query.PatientView;
import com.hashimoto.patient_service.query.PatientViewRepository;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;


@RestController
@RequestMapping("/api/patients")
public class PatientControllerAPI {

    private final CommandGateway commandGateway;
    private final QueryGateway queryGateway;
    private final PatientViewRepository patientViewRepository;
    private final boolean localFastMode;

    public PatientControllerAPI(CommandGateway commandGateway,
                               QueryGateway queryGateway,
                               PatientViewRepository patientViewRepository,
                               @Value("${app.local.fast-mode:false}") boolean localFastMode) {
        this.commandGateway = commandGateway;
        this.queryGateway = queryGateway;
        this.patientViewRepository = patientViewRepository;
        this.localFastMode = localFastMode;
    }

    @PostMapping
    public CompletableFuture<String> createPatient(@RequestBody CreatePatientRequest request) {
        String patientId = java.util.UUID.randomUUID().toString();

        if (localFastMode) {
            patientViewRepository.save(new PatientView(
                    patientId,
                    request.getFirstName(),
                    request.getLastName(),
                    request.getGender(),
                    request.getDni(),
                    request.getEmail(),
                    request.getAge()
            ));
            return CompletableFuture.completedFuture(patientId);
        }

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
