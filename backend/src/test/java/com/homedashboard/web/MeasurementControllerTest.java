package com.homedashboard.web;

import com.homedashboard.model.ChartDataResponse;
import com.homedashboard.model.ChartRange;
import com.homedashboard.model.Room;
import com.homedashboard.service.MeasurementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MeasurementControllerTest {

    private MeasurementService measurementService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        measurementService = mock(MeasurementService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new MeasurementController(measurementService))
                .setControllerAdvice(GlobalExceptionHandler.class)
                .build();
    }

    @Test
    void returnsBadRequestForUnknownRoom() throws Exception {
        mockMvc.perform(get("/api/measurements/garage/chart/day"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(measurementService);
    }

    @Test
    void returnsBadRequestForUnknownRange() throws Exception {
        mockMvc.perform(get("/api/measurements/office/chart/year"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(measurementService);
    }

    @Test
    void returnsBadRequestForInvalidToParameter() throws Exception {
        mockMvc.perform(get("/api/measurements/office/chart/month")
                        .param("to", "not-a-date"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(measurementService);
    }

    @Test
    void returnsMetadataAndEmptyPointsForValidRequestWithoutData() throws Exception {
        OffsetDateTime to = OffsetDateTime.parse("2026-06-20T12:00:00+02:00");
        OffsetDateTime from = to.minusHours(24);
        ChartDataResponse response = new ChartDataResponse(
                Room.OFFICE,
                ChartRange.DAY,
                from,
                to,
                ChartRange.DAY.getResolution(),
                List.of());

        when(measurementService.getChartData(Room.OFFICE, ChartRange.DAY))
                .thenReturn(response);

        mockMvc.perform(get("/api/measurements/office/chart/day"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.room").value("OFFICE"))
                .andExpect(jsonPath("$.range").value("day"))
                .andExpect(jsonPath("$.from").value("2026-06-19T12:00:00+02:00"))
                .andExpect(jsonPath("$.to").value("2026-06-20T12:00:00+02:00"))
                .andExpect(jsonPath("$.resolution").value("raw"))
                .andExpect(jsonPath("$.points").isEmpty());
    }

    @Test
    void passesExplicitToParameterForHistoricalChartRequest() throws Exception {
        OffsetDateTime to = OffsetDateTime.parse("2026-04-30T23:59:59+02:00");
        OffsetDateTime from = to.minusDays(30);
        ChartDataResponse response = new ChartDataResponse(
                Room.OFFICE,
                ChartRange.MONTH,
                from,
                to,
                ChartRange.MONTH.getResolution(),
                List.of());

        when(measurementService.getChartData(Room.OFFICE, ChartRange.MONTH, to))
                .thenReturn(response);

        mockMvc.perform(get("/api/measurements/office/chart/month")
                        .param("to", to.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.room").value("OFFICE"))
                .andExpect(jsonPath("$.range").value("month"))
                .andExpect(jsonPath("$.from").value("2026-03-31T23:59:59+02:00"))
                .andExpect(jsonPath("$.to").value("2026-04-30T23:59:59+02:00"))
                .andExpect(jsonPath("$.resolution").value("daily"))
                .andExpect(jsonPath("$.points").isEmpty());

        verify(measurementService).getChartData(Room.OFFICE, ChartRange.MONTH, to);
    }

}
