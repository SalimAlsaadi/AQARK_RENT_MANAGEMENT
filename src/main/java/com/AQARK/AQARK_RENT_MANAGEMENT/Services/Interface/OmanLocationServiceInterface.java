package com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.LocationDTO.LocationLookupDTO;

import java.util.List;

public interface OmanLocationServiceInterface {

    List<LocationLookupDTO> getRegions();

    List<LocationLookupDTO> getWilayasByRegionId(Long regionId);

    List<LocationLookupDTO> getAreasByWilayaId(Long wilayaId);
}