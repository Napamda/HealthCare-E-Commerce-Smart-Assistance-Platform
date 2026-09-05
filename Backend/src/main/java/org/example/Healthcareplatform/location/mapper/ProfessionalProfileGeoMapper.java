package org.example.Healthcareplatform.location.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.Healthcareplatform.location.dto.NearbyProfessionalRow;

import java.util.List;

@Mapper
public interface ProfessionalProfileGeoMapper {

    List<NearbyProfessionalRow> findNearby(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radiusKm") double radiusKm,
            @Param("specialty") String specialty);
}
