package com.hashimoto.patient_service.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Data // <-- Cambiado de @Value a @Data para asegurar getters estándar
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePatientCommand {
    @TargetAggregateIdentifier
    String patientId;  // UUID único para identificar al paciente
    String firstName;
    String lastName;
    String gender;
    String dni;
    String email;
    int age;
}
