package com.AQARK.AQARK_RENT_MANAGEMENT.Services.Implementation;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.StatusDTO.UnitStatusDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.StatusDTO.UnitStatusSaveRequestDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.StatusDTO.UnitStatusUpdateRequestDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.EntityUnitStatus;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Enums.StatusDomain;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Enums.StatusLevel;
import com.AQARK.AQARK_RENT_MANAGEMENT.Repositories.UnitStatusRepository;
import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface.UnitStatusServiceInterface;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UnitStatusServiceImpl implements UnitStatusServiceInterface {

    private final UnitStatusRepository unitStatusRepository;

    @Override
    @Transactional
    public UnitStatusDTO createStatus(UnitStatusSaveRequestDTO dto) {
        validateSaveRequest(dto);

        String domain = normalizeDomain(dto.getStatusDomain());
        String code = normalizeCode(dto.getCode());

        if (unitStatusRepository.existsByStatusDomainIgnoreCaseAndCodeIgnoreCase(domain, code)) {
            throw new IllegalStateException(
                    "Status already exists: " + domain + "/" + code
            );
        }

        EntityUnitStatus status = EntityUnitStatus.builder()
                .statusDomain(domain)
                .code(code)
                .nameEn(cleanRequired(dto.getNameEn(), "English status name is required"))
                .nameAr(clean(dto.getNameAr()))
                .descriptionEn(clean(dto.getDescriptionEn()))
                .descriptionAr(clean(dto.getDescriptionAr()))
                .colorCode(clean(dto.getColorCode()))
                .icon(clean(dto.getIcon()))
                .statusLevel(requireStatusLevel(dto.getStatusLevel()))
                .active(dto.getActive() == null || dto.getActive())
                .build();

        EntityUnitStatus saved = unitStatusRepository.save(status);

        return toDTO(saved);
    }

    @Override
    @Transactional
    public UnitStatusDTO updateStatus(Long id, UnitStatusUpdateRequestDTO dto) {


        EntityUnitStatus status = unitStatusRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Status not found with id: " + id));

        String newDomain = normalizeDomain(dto.getStatusDomain());
        String newCode = normalizeCode(dto.getCode());

        boolean identityChanged =
                !status.getStatusDomain().equalsIgnoreCase(newDomain)
                        || !status.getCode().equalsIgnoreCase(newCode);

        if (
                identityChanged
                        && unitStatusRepository.existsByStatusDomainIgnoreCaseAndCodeIgnoreCase(newDomain, newCode)
        ) {
            throw new IllegalStateException(
                    "Status already exists: " + newDomain + "/" + newCode
            );
        }

        status.setStatusDomain(newDomain);
        status.setCode(newCode);
        status.setNameEn(cleanRequired(dto.getNameEn(), "English status name is required"));
        status.setNameAr(clean(dto.getNameAr()));
        status.setDescriptionEn(clean(dto.getDescriptionEn()));
        status.setDescriptionAr(clean(dto.getDescriptionAr()));
        status.setColorCode(clean(dto.getColorCode()));
        status.setIcon(clean(dto.getIcon()));
        status.setStatusLevel(requireStatusLevel(dto.getStatusLevel()));

        if (dto.getActive() != null) {
            status.setActive(dto.getActive());
        }

        EntityUnitStatus updated = unitStatusRepository.save(status);

        return toDTO(updated);
    }

    @Override
    public UnitStatusDTO getStatusById(Long id) {
        EntityUnitStatus status = unitStatusRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Status not found with id: " + id));

        return toDTO(status);
    }

    @Override
    public UnitStatusDTO getStatusByCode(StatusDomain domain, String code) {
        EntityUnitStatus status = getRequiredStatus(domain, code);

        return toDTO(status);
    }

    @Override
    public List<UnitStatusDTO> getActiveStatusesByDomain(StatusDomain domain) {
        String normalizedDomain = normalizeDomain(domain);

        return unitStatusRepository
                .findByStatusDomainIgnoreCaseAndActiveTrueOrderByNameEnAsc(normalizedDomain)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public List<UnitStatusDTO> getActiveStatusesByDomainAndLevel(
            StatusDomain domain,
            StatusLevel level
    ) {
        String normalizedDomain = normalizeDomain(domain);
        StatusLevel requiredLevel = Objects.requireNonNull(level, "Status level is required.");

        return unitStatusRepository
                .findByStatusDomainIgnoreCaseAndStatusLevelAndActiveTrueOrderByNameEnAsc(
                        normalizedDomain,
                        requiredLevel
                )
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public List<UnitStatusDTO> getSetupStatusesByDomain(StatusDomain domain) {
        return getActiveStatusesByDomainAndLevel(domain, StatusLevel.S);
    }

    @Override
    public List<UnitStatusDTO> getAllActiveStatuses() {
        return unitStatusRepository
                .findByActiveTrueOrderByStatusDomainAscStatusLevelAscNameEnAsc()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public List<UnitStatusDTO> getAllStatuses() {
        return unitStatusRepository
                .findAllByOrderByStatusDomainAscStatusLevelAscNameEnAsc()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    @Transactional
    public void activateStatus(Long id) {
        EntityUnitStatus status = unitStatusRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Status not found with id: " + id));

        status.setActive(true);

        unitStatusRepository.save(status);
    }

    @Override
    @Transactional
    public void deactivateStatus(Long id) {
        EntityUnitStatus status = unitStatusRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Status not found with id: " + id));

        status.setActive(false);

        unitStatusRepository.save(status);
    }

    /*
     * Resolver methods
     * These replace the old UnitStatusResolver class.
     */

    @Override
    public EntityUnitStatus getRequiredStatus(StatusDomain domain, String code) {
        String normalizedDomain = normalizeDomain(domain);
        String normalizedCode = normalizeCode(code);

        return unitStatusRepository
                .findByStatusDomainIgnoreCaseAndCodeIgnoreCaseAndActiveTrue(
                        normalizedDomain,
                        normalizedCode
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Invalid or inactive status: "
                                        + normalizedDomain + "/" + normalizedCode
                        )
                );
    }

    @Override
    public EntityUnitStatus getRequiredStatus(
            StatusDomain domain,
            String code,
            StatusLevel level
    ) {
        String normalizedDomain = normalizeDomain(domain);
        String normalizedCode = normalizeCode(code);
        StatusLevel requiredLevel = Objects.requireNonNull(level, "Status level is required.");

        return unitStatusRepository
                .findByStatusDomainIgnoreCaseAndCodeIgnoreCaseAndStatusLevelAndActiveTrue(
                        normalizedDomain,
                        normalizedCode,
                        requiredLevel
                )
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invalid, inactive, or disallowed status: "
                                + normalizedDomain + "/" + normalizedCode
                                + " for level " + requiredLevel
                ));
    }

    @Override
    public EntityUnitStatus getRequiredSetupStatus(StatusDomain domain, String code) {
        return getRequiredStatus(domain, code, StatusLevel.S);
    }

    @Override
    public EntityUnitStatus getDefaultBuildingStatus() {
        return getRequiredStatus(StatusDomain.BUILDING, "ACTIVE");
    }


    @Override
    public EntityUnitStatus getDefaultStatus(StatusDomain domain) {
        return switch (domain) {
            case BUILDING -> getRequiredSetupStatus(StatusDomain.BUILDING, "ACTIVE");
            case HOME, FLAT, ROOM, SHOP -> getRequiredSetupStatus(domain, "AVAILABLE");
            case CONTRACT -> getRequiredSetupStatus(StatusDomain.CONTRACT, "DRAFT");
            case MAINTENANCE -> getRequiredSetupStatus(StatusDomain.MAINTENANCE, "OPEN");
            case PAYMENT -> getRequiredSetupStatus(StatusDomain.PAYMENT, "PENDING");
        };
    }

    @Override
    public EntityUnitStatus getDefaultContractStatus() {
        return getRequiredStatus(StatusDomain.CONTRACT, "DRAFT");
    }

    @Override
    public EntityUnitStatus getDefaultMaintenanceStatus() {
        return getRequiredStatus(StatusDomain.MAINTENANCE, "OPEN");
    }

    @Override
    public EntityUnitStatus getDefaultPaymentStatus() {
        return getRequiredStatus(StatusDomain.PAYMENT, "PENDING");
    }

    @Override
    public boolean isUnitAvailableForRent(String statusCode) {
        return "AVAILABLE".equalsIgnoreCase(normalizeCode(statusCode));
    }

    @Override
    public boolean isUnitOccupied(String statusCode) {
        return "RENTED".equalsIgnoreCase(normalizeCode(statusCode));
    }

    /*
     * Private helpers
     */

    private void validateSaveRequest(UnitStatusSaveRequestDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Status request is required.");
        }

        if (dto.getStatusDomain() == null || dto.getStatusDomain().trim().isEmpty()) {
            throw new IllegalArgumentException("Status domain is required.");
        }

        if (dto.getCode() == null || dto.getCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Status code is required.");
        }

        if (dto.getNameEn() == null || dto.getNameEn().trim().isEmpty()) {
            throw new IllegalArgumentException("English status name is required.");
        }

        if (dto.getStatusLevel() == null) {
            throw new IllegalArgumentException("Status level is required.");
        }
    }

    private StatusLevel requireStatusLevel(StatusLevel level) {
        return Objects.requireNonNull(level, "Status level is required.");
    }

    private String normalizeDomain(StatusDomain domain) {
        if (domain == null) {
            throw new IllegalArgumentException("Status domain is required.");
        }

        return domain.name();
    }

    private String normalizeDomain(String domain) {
        if (domain == null || domain.trim().isEmpty()) {
            throw new IllegalArgumentException("Status domain is required.");
        }

        return domain
                .trim()
                .toUpperCase()
                .replace(" ", "_")
                .replace("-", "_");
    }

    private String normalizeCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Status code is required.");
        }

        return code
                .trim()
                .toUpperCase()
                .replace(" ", "_")
                .replace("-", "_");
    }

    private String cleanRequired(String value, String message) {
        String cleaned = clean(value);

        if (cleaned == null) {
            throw new IllegalArgumentException(message);
        }

        return cleaned;
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }

        String cleaned = value.trim();

        return cleaned.isEmpty() ? null : cleaned;
    }

    private UnitStatusDTO toDTO(EntityUnitStatus status) {
        return UnitStatusDTO.builder()
                .id(status.getId())
                .statusDomain(status.getStatusDomain())
                .code(status.getCode())
                .nameEn(status.getNameEn())
                .nameAr(status.getNameAr())
                .descriptionEn(status.getDescriptionEn())
                .descriptionAr(status.getDescriptionAr())
                .colorCode(status.getColorCode())
                .icon(status.getIcon())
                .statusLevel(status.getStatusLevel())
                .active(status.isActive())
                .build();
    }
}