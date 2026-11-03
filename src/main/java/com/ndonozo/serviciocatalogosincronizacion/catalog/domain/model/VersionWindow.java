package com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.exception.InvalidCatalogDataException;
import lombok.Builder;
import lombok.Getter;

@Getter
public class VersionWindow {
    private final long currentVersion;
    private final long oldestAvailableVersion;



    @Builder
    private VersionWindow(long currentVersion, long oldestAvailableVersion){
        if (oldestAvailableVersion < 0){
            throw new InvalidCatalogDataException("Version Window OldestAvailableVersion can't be negative " + oldestAvailableVersion);

        }
        if (currentVersion < oldestAvailableVersion){
            throw new InvalidCatalogDataException("Version window OldestAvailableVersion " + oldestAvailableVersion + " can't " +
                    " be greater than currentVersion (" + currentVersion + ")");
        }
        this.currentVersion = currentVersion;
        this.oldestAvailableVersion = oldestAvailableVersion;

    }

}





// currentVersion y oldestAvaiableVersion para saber si podemos aplicar incrementos o reconstruir. Esta en el statement