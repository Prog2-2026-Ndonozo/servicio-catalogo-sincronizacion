package com.ndonozo.arquitecturafixtures.violarWebYPersistenciaNoUsanLaCatedra.infrastructure.persistence;

import com.ndonozo.arquitecturafixtures.violarWebYPersistenciaNoUsanLaCatedra.infrastructure.catedraredis.ClienteRedisDeLaCatedraFalso;

public class BuscadorQueUsaRedisDeLaCatedra {

    public String buscar(ClienteRedisDeLaCatedraFalso cliente) {
        return cliente.leer();
    }
}