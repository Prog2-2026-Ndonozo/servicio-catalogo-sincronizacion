package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.entity;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.SyncStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name= "sync_state")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SyncStateEntity {
    public static final short SINGLE_ROW_ID = 1 ;

    @Id
    private short id ;

    @Enumerated(EnumType.STRING)
    private SyncStatus status;
    private String detail;

    @UpdateTimestamp
    private Instant updatedAt;


}
