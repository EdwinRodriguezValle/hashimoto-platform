package com.hashimoto.patient_service.aggregate;

import com.hashimoto.patient_service.command.CreatePatientCommand;
import com.hashimoto.patient_service.event.PatientCreatedEvent;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;

@Aggregate
public class PatientAgregate {
    @AggregateIdentifier
    private String patientId;
    private String firstName;
    private String lastName;
    private String gender;
    private String dni;
    private String email;
    private int age;

    // Axon requiere obligatoriamente un constructor vacío
    public PatientAgregate() {}

    // 1. El Manejador del Comando
    @CommandHandler
    public PatientAgregate(CreatePatientCommand command) {
        // Aquí puedes agregar lógica de validación si es necesario
        // Por ejemplo, verificar que el DNI sea único, que la edad sea válida, etc.
        if(command.getAge() < 0){
            throw new IllegalArgumentException("La edad no puede ser negativa");
        }

        if(command.getDni() == null || command.getDni().isEmpty()){
            throw new IllegalArgumentException("El DNI no puede estar vacío");
        }

        // 2. Publicar el evento, si la validación pasa, disparamos el evento al ciclo de vida de Axon
        AggregateLifecycle.apply(PatientCreatedEvent.builder()
                .patientId(command.getPatientId())
                .firstName(command.getFirstName())
                .lastName(command.getLastName())
                .gender(command.getGender())
                .dni(command.getDni())
                .email(command.getEmail())
                .age(command.getAge())
                .build());
    }

    // 2. El Manejador del Evento (Aquí es donde el Agregado muta su estado interno)
    @EventSourcingHandler
    public void on(PatientCreatedEvent event){
        this.patientId = event.getPatientId();
        this.firstName = event.getFirstName();
        this.lastName = event.getLastName();
        this.gender = event.getGender();
        this.dni = event.getDni();
        this.email = event.getEmail();
        this.age = event.getAge();
    }

}
