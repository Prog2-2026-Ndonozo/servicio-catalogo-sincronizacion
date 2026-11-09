package com.ndonozo.serviciocatalogosincronizacion.catalog.domain.ports.out;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.CatalogVersion;

/**
 * Cursor de sincronización: la versión del catálogo aplicada localmente.
 *
 * Es un dato de la sincronización y no del catálogo: separa "qué datos tengo" (CatalogRepositoryPort)
 * de "hasta qué versión llegué".
 *
 * No abre transacciones: participa de la del caso de uso, de modo que la versión avanza si y solo si
 * los datos también se confirmaron.
 */
public interface CatalogVersionPort {

    /** Versión aplicada localmente. Nunca es null: una base sin sincronizar devuelve la versión 0 (ADR-0014). */
    CatalogVersion getCatalogVersion();

    /** Registra la versión aplicada. Se llama al final, después de guardar los datos. */
    void saveCatalogVersion(CatalogVersion version);
}
