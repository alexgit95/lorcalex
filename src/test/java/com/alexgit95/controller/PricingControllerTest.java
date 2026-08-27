package com.alexgit95.controller;

import com.alexgit95.service.CollectionValueTrendService;
import com.alexgit95.service.PricingInsightsService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

class PricingControllerTest {

    @Test
    void recomputeValue_returnsMessageAndRootCauseOnFailure() {
        CollectionValueTrendService trendService = mock(CollectionValueTrendService.class);
        RuntimeException rootCause = new IllegalStateException("no cards priced");
        doThrow(new RuntimeException("snapshot computation failed", rootCause))
                .when(trendService).persistSnapshotFromCurrentCollection();

        PricingController controller = new PricingController(mock(PricingInsightsService.class), trendService);

        ResponseEntity<Map<String, Object>> response = controller.recomputeValue();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().get("success")).isEqualTo(false);
        assertThat(response.getBody().get("message")).isEqualTo("snapshot computation failed");
        assertThat(response.getBody().get("rootCause")).isEqualTo("no cards priced");
    }
}
