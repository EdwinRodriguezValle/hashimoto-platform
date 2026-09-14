package com.hashimoto.patient_service.query;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PatientView {
    @Id
    private String patientId;
    private String firstName;
    private String lastName;
    private String gender;
    private String dni;
    private String email;
    private int age;
}
