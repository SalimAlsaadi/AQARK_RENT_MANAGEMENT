package com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface PropertyCommonServiceInterface {

    public Object create (String propertyType, String data, List<MultipartFile> pictures, Long Landlord) throws IOException;
}
