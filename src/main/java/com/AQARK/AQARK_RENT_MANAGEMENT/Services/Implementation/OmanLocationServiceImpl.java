package com.AQARK.AQARK_RENT_MANAGEMENT.Services.Implementation;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.LocationDTO.LocationDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.LocationDTO.LocationLookupDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.OmanAreaEntity;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.OmanRegionEntity;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.OmanWilayaEntity;
import com.AQARK.AQARK_RENT_MANAGEMENT.Repositories.OmanAreaRepository;
import com.AQARK.AQARK_RENT_MANAGEMENT.Repositories.OmanRegionRepository;
import com.AQARK.AQARK_RENT_MANAGEMENT.Repositories.OmanWilayaRepository;
import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface.OmanLocationServiceInterface;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OmanLocationServiceImpl implements OmanLocationServiceInterface {

    private final OmanRegionRepository regionRepository;
    private final OmanWilayaRepository wilayaRepository;
    private final OmanAreaRepository areaRepository;

    @Override
    public List<LocationLookupDTO> getRegions() {
        return regionRepository
                .findByActiveTrueOrderByDisplayOrderAscNameEnAsc()
                .stream()
                .map(this::toRegionDTO)
                .toList();
    }

    @Override
    public List<LocationLookupDTO> getWilayasByRegionId(Long regionId) {
        return wilayaRepository
                .findByRegionIdAndActiveTrueOrderByDisplayOrderAscNameEnAsc(regionId)
                .stream()
                .map(this::toWilayaDTO)
                .toList();
    }

    @Override
    public List<LocationLookupDTO> getAreasByWilayaId(Long wilayaId) {
        return areaRepository
                .findByWilayaIdAndActiveTrueOrderByDisplayOrderAscNameEnAsc(wilayaId)
                .stream()
                .map(this::toAreaDTO)
                .toList();
    }

    private LocationLookupDTO toRegionDTO(OmanRegionEntity entity) {
        return new LocationLookupDTO(
                entity.getId(),
                entity.getCode(),
                entity.getNameEn(),
                entity.getNameAr()
        );
    }

    private LocationLookupDTO toWilayaDTO(OmanWilayaEntity entity) {
        return new LocationLookupDTO(
                entity.getId(),
                entity.getCode(),
                entity.getNameEn(),
                entity.getNameAr()
        );
    }

    private LocationLookupDTO toAreaDTO(OmanAreaEntity entity) {
        return new LocationLookupDTO(
                entity.getId(),
                entity.getCode(),
                entity.getNameEn(),
                entity.getNameAr()
        );
    }

    public LocationDTO location(Long regionId, Long wilayaId, Long areaId){

        OmanRegionEntity region=regionRepository.findById(regionId).orElseThrow(()-> new EntityNotFoundException("Region not found for ID: "+ regionId));
        OmanWilayaEntity wilaya=wilayaRepository.findByIdAndRegionId(wilayaId, regionId);
        OmanAreaEntity area=areaRepository.findByIdAndWilayaId(areaId, wilayaId);

       return LocationDTO.builder()
                .regionId(region.getId())
                .regionNameEn(region.getNameEn())
                .regionNameAr(region.getNameAr())
                .wilayaId(wilaya.getId())
                .wilayaNameEn(wilaya.getNameEn())
                .wilayaNameAr(wilaya.getNameAr())
                .areaId(area.getId())
                .areaNameEn(area.getNameEn())
                .areaNameAr(area.getNameAr())
                .build();
    }
}