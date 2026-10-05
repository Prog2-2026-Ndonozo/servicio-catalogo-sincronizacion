package com.ndonozo.arquitecturafixtures.violarSinInyeccionPorCampo;

import org.springframework.beans.factory.annotation.Autowired;

public class ClaseConAutowiredEnCampo {

    @Autowired
    private String noSeInyectaPorConstructor;
}