package com.hashimoto.patient_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data // <-- Cambiado de @Value a @Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientCreatedEvent {
    String patientId;
    String firstName;
    String lastName;
    String gender;
    String dni;
    String email;
    int age;
}

