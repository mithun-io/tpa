package com.tpa.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface StorageProvider {
    String storeFile(MultipartFile file);
    Resource loadFileAsResource(String filePath);
}
