package com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.MediaDTO;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyMediaDTO {

    private Long id;
    private String fileUrl;
    private String originalFileName;
    private String contentType;
    private Long sizeBytes;
    private boolean mainImage;
    private Integer sortOrder;
}