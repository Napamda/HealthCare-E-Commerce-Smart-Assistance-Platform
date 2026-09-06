package org.example.Healthcareplatform.auth;

import org.example.Healthcareplatform.auth.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RoleAuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    void patientTokenOnPharmacistEndpointIsForbidden() throws Exception {
        String patientToken = jwtUtil.generateAccessToken(1L, "patient@test.com", "PATIENT");

        mockMvc.perform(get("/api/prescriptions/pharmacist/pending")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + patientToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void doctorCannotAccessPatientCart() throws Exception {
        assertForbiddenForRole("/api/cart", "DOCTOR");
    }

    @Test
    void pharmacistCannotAccessPatientOrders() throws Exception {
        assertForbiddenForRole("/api/orders", "PHARMACIST");
    }

    @Test
    void adminCannotAccessPatientPayments() throws Exception {
        assertForbiddenForRole("/api/payments/methods", "ADMIN");
    }

    @Test
    void patientCanReachStockValidationEndpoint() throws Exception {
        String token = jwtUtil.generateAccessToken(2L, "patient@test.com", "PATIENT");

        mockMvc.perform(post("/api/inventory/validate")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"items\":[]}"))
                // The deliberately empty list is rejected by request validation.
                // A 400 proves security allowed the patient request to reach it.
                .andExpect(status().isBadRequest());
    }

    private void assertForbiddenForRole(String path, String role) throws Exception {
        String token = jwtUtil.generateAccessToken(1L, role.toLowerCase() + "@test.com", role);

        mockMvc.perform(get(path)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
