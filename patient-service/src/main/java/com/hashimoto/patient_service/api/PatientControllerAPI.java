package com.hashimoto.patient_service.api;

import com.hashimoto.patient_service.command.CreatePatientCommand;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;
@RestController
@RequestMapping("/api/patients")
public class PatientControllerAPI {

    private final CommandGateway commandGateway;

    public PatientControllerAPI(CommandGateway commandGateway) {
        this.commandGateway = commandGateway;
    }

    @PostMapping
    public CompletableFuture<String> createPatient(CreatePatientRequest request) {
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
}
