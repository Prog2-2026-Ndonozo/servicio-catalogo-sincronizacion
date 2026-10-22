package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.entity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;

@Entity           // esto nos indica que es una entidad JPA
@Table(name= "professional_categories")  // a la tabla a la que apunta
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class ProfessionalCategoryEntity {
    @Id //indicamos la clave primaria
    private Long id;
    private String name;
    private String description;
    private boolean enabled;
    private Instant createdAt;
    private Instant updatedAt;

}
