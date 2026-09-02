package org.example.Healthcareplatform.location.mapper;

import org.example.Healthcareplatform.location.dto.NearbyProfessionalRow;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:healthcare;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
@Sql(scripts = "/sql/profile-schema.sql", executionPhase = ExecutionPhase.BEFORE_TEST_CLASS)
@Sql(scripts = "/sql/profile-data.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
class ProfessionalProfileGeoMapperTest {

    @Autowired
    private ProfessionalProfileGeoMapper geoMapper;

    @Test
    void shouldReturnActiveProfilesWithinRadiusSortedByDistance() {
        List<NearbyProfessionalRow> rows =
                geoMapper.findNearby(5.6037, -0.1870, 10.0, null);

        assertThat(rows)
                .extracting(NearbyProfessionalRow::getId)
                .containsExactly(1L, 3L, 2L);
        assertThat(rows).allSatisfy(row -> {
            assertThat(row.isActive()).isTrue();
            assertThat(row.getDistanceKm()).isLessThanOrEqualTo(10.0);
        });
        assertThat(rows.get(0).getDistanceKm()).isLessThan(0.01);
    }

    @Test
    void shouldExcludeInactiveFarAndBoundingBoxCornerProfiles() {
        List<NearbyProfessionalRow> rows =
                geoMapper.findNearby(5.6037, -0.1870, 10.0, null);

        assertThat(rows)
                .extracting(NearbyProfessionalRow::getId)
                .doesNotContain(4L, 5L, 6L);
    }

    @Test
    void shouldFilterBySpecialtyCaseInsensitively() {
        List<NearbyProfessionalRow> rows =
                geoMapper.findNearby(5.6037, -0.1870, 10.0, "Cardio");

        assertThat(rows)
                .extracting(NearbyProfessionalRow::getId)
                .containsExactly(1L);
    }

    @Test
    void shouldMapSnakeCaseColumns() {
        List<NearbyProfessionalRow> rows =
                geoMapper.findNearby(5.6037, -0.1870, 10.0, null);

        NearbyProfessionalRow first = rows.get(0);
        assertThat(first.getUserId()).isEqualTo(1L);
        assertThat(first.getFirstName()).isEqualTo("Ama");
        assertThat(first.getLastName()).isEqualTo("Mensah");
        assertThat(first.getSpecialty()).isEqualTo("Cardiology");
        assertThat(first.getCity()).isEqualTo("Accra");
        assertThat(first.getCreatedAt()).isNotNull();
    }
}
