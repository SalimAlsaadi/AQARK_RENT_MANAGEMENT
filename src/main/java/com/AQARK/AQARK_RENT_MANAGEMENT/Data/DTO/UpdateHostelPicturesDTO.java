package com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO;

import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class UpdateHostelPicturesDTO {

    private Long propertyId;
    private String hostelType;
    private List<String> picturesToRemove;
    private List<MultipartFile> newPictures;

}
