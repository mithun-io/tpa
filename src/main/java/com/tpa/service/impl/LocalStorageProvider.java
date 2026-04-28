package com.tpa.service.impl;

import com.tpa.exception.NoResourceFoundException;
import com.tpa.service.StorageProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Component
public class LocalStorageProvider implements StorageProvider {

    private final Path uploadPath;

    public LocalStorageProvider(@Value("${file.upload-dir}") String uploadDir) {
        this.uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadPath);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    @Override
    public String storeFile(MultipartFile file) {
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "document.pdf");
        String fileName = UUID.randomUUID().toString() + "_" + originalFileName;

        try {
            if (fileName.contains("..")) {
                throw new RuntimeException("Sorry! Filename contains invalid path sequence " + fileName);
            }

            Path targetLocation = this.uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return targetLocation.toString();
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + fileName + ". Please try again!", ex);
        }
    }

    @Override
    public Resource loadFileAsResource(String filePathStr) {
        try {
            Path storedPath = Paths.get(filePathStr);
            String fileName = storedPath.getFileName().toString();
            Path filePath = this.uploadPath.resolve(fileName).normalize();

            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return resource;
            } else {
                throw new NoResourceFoundException("File not found " + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new NoResourceFoundException("File not found " + filePathStr);
        }
    }
}
