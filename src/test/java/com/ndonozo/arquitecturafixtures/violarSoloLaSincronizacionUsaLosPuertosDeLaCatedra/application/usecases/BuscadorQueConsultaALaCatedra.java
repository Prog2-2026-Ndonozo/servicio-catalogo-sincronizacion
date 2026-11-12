package com.ndonozo.arquitecturafixtures.violarSoloLaSincronizacionUsaLosPuertosDeLaCatedra.application.usecases;

import com.ndonozo.arquitecturafixtures.violarSoloLaSincronizacionUsaLosPuertosDeLaCatedra.domain.ports.out.CatedraCatalogReadPort;

public class BuscadorQueConsultaALaCatedra {

    public String buscar(CatedraCatalogReadPort catedra) {
        return catedra.leer();
    }
}