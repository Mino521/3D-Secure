package com.jamiewang.secure3d.service;

import com.jamiewang.secure3d.dto.BulkImportResponseDTO;
import com.jamiewang.secure3d.dto.PResMessageDTO;

public interface IStorePResService {

    BulkImportResponseDTO processPResMessage(PResMessageDTO presMessage);

}
