package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.startup;

import com.ndonozo.serviciocatalogosincronizacion.catalog.application.service.CatalogSyncService;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.exception.CatedraUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "catedra.sync-on-startup", havingValue = "true", matchIfMissing = true)
public class StartupSyncRunner implements ApplicationRunner {
    private final CatalogSyncService syncService;

    @Override
    public void run(ApplicationArguments args){
        try{
            if (syncService.synchronizeIfEmpty()){
                log.info("Sincronizacion inicial del catalogo completada " );

            } else {
                log.info("Catalogo ya sincronizado: no se pide snapshot al arrancar");
            }
        } catch(CatedraUnavailableException e){
            log.warn("Catedra no disponible al arrancar (status={}). El servicio sigue activo sin catalogo.", e.getHttpStatus());
        }
    }
}
//Utilizamos log.info("") para eventos normales importantes
//log.warn("") para algo que Importa pero no me tira la aplicacion

// el catch hace que el servicio siga vivo si la catedra no responde.
// @ConditionalOnProperty permite apagar la sincronizacion inicial a mano (catedra.sync-on-startup=false)