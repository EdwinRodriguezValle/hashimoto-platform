package com.hashimoto.patient_service.config;

import jakarta.persistence.EntityManager; // <-- IMPORTANTE PARA JAVA 21 / SPRING BOOT 3
import org.axonframework.springboot.util.jpa.ContainerManagedEntityManagerProvider;
import org.axonframework.common.jpa.EntityManagerProvider;
import org.axonframework.eventsourcing.eventstore.EmbeddedEventStore;
import org.axonframework.eventsourcing.eventstore.EventStorageEngine;
import org.axonframework.eventsourcing.eventstore.EventStore;
import org.axonframework.eventsourcing.eventstore.jpa.JpaEventStorageEngine;
import org.axonframework.serialization.Serializer;
import org.axonframework.spring.messaging.unitofwork.SpringTransactionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class AxonStorageConfig {
    // 1. EL BEAN QUE FALTA: Provee el puente de conexión con el gestor de entidades JPA
    @Bean
    public EntityManagerProvider entityManagerProvider(EntityManager entityManager) {
        ContainerManagedEntityManagerProvider provider = new ContainerManagedEntityManagerProvider();
        provider.setEntityManager(entityManager);
        return provider;
    }

    // 2. Configurar el motor de almacenamiento JPA acoplado a PostgreSQL
    @Bean
    public EventStorageEngine eventStorageEngine(
            Serializer serializer,
            EntityManagerProvider entityManagerProvider,
            PlatformTransactionManager transactionManager) {

        return JpaEventStorageEngine.builder()
                .snapshotSerializer(serializer)
                .eventSerializer(serializer)
                .entityManagerProvider(entityManagerProvider)
                .transactionManager(new SpringTransactionManager(transactionManager))
                .build();
    }

    // 3. Crear el Event Store oficial
    @Bean
    @Primary
    public EventStore eventStore(EventStorageEngine storageEngine) {
        return EmbeddedEventStore.builder()
                .storageEngine(storageEngine)
                .build();
    }
}
