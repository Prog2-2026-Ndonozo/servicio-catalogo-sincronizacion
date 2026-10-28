package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.catedrarest.client;


import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.exception.CatedraUnavailableException;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.catedrarest.dto.CatedraSnapshotResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@RequiredArgsConstructor

public class CatedraRestClient {

    private static final String SNAPSHOT_PATH= "/api/synchronization/snapshot";

    private final RestClient restClient;

    public CatedraSnapshotResponse fetchSnapshot() {
        try {
            CatedraSnapshotResponse response = restClient.get()
                    .uri(SNAPSHOT_PATH)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, httpResponse) -> {
                        int status = httpResponse.getStatusCode().value();
                        throw new CatedraUnavailableException(
                                "Catedra answered HTTP " + status + " to GET "+ SNAPSHOT_PATH, status
                        );
                    }).body(CatedraSnapshotResponse.class);
            if (response == null){
                throw new CatedraUnavailableException("Catedra returned an empty body for " + SNAPSHOT_PATH);
            }
            return response ;

        } catch (RestClientException e) {
            throw new CatedraUnavailableException("Could not read the snapshot from catedra: " + e.getMessage(), e);
        }
    }
}
