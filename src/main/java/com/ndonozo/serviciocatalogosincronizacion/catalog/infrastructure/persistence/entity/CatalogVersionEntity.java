package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.entity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;



@Entity           // esto nos indica que es una entidad JPA
@Table(name= "catalog_version")  // a la tabla a la que apunta
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class CatalogVersionEntity {
    @Id
    private short id;;
    private long snapshotVersion;

}
