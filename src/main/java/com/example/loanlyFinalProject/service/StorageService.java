package com.example.loanlyFinalProject.service;

import com.example.loanlyFinalProject.entity.PlafondDocument;
import com.example.loanlyFinalProject.entity.UserPlafond;
import com.example.loanlyFinalProject.exception.ResourceNotFoundException;
import com.example.loanlyFinalProject.repository.PlafondDocumentRepository;
import com.example.loanlyFinalProject.repository.UserPlafondRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class StorageService {

  private final PlafondDocumentRepository plafondDocumentRepository;
  private final UserPlafondRepository userPlafondRepository;

  @Value("${file.upload-dir:uploads}")
  private String uploadDir;

  /** Upload document to LOCAL storage and save metadata to database */
  @Transactional
  public PlafondDocument uploadPlafondDocument(
      Long userPlafondId, MultipartFile file, PlafondDocument.DocumentType documentType) {
    UserPlafond userPlafond =
        userPlafondRepository
            .findById(userPlafondId)
            .orElseThrow(() -> new ResourceNotFoundException("User Plafond", "id", userPlafondId));

    String originalFilename = file.getOriginalFilename();
    String extension = getFileExtension(originalFilename);
    String uniqueFilename =
        String.format(
            "%s_%s%s", documentType.name().toLowerCase(), UUID.randomUUID().toString(), extension);

    // Create directory structure: uploads/plafonds/{userPlafondId}/
    String relativePath = String.format("plafonds/%d/%s", userPlafondId, uniqueFilename);
    Path uploadPath = Paths.get(uploadDir, "plafonds", String.valueOf(userPlafondId));

    try {
      // Create directories if not exist
      Files.createDirectories(uploadPath);

      // Save file
      Path filePath = uploadPath.resolve(uniqueFilename);
      Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

      log.info("Document uploaded to local storage: {}", filePath);

      // Generate accessible URL
      String fileUrl = "/uploads/" + relativePath;

      // Save document metadata to database
      PlafondDocument document =
          PlafondDocument.builder()
              .userPlafond(userPlafond)
              .documentType(documentType)
              .fileUrl(fileUrl)
              .fileName(originalFilename)
              .build();

      return plafondDocumentRepository.save(document);

    } catch (IOException e) {
      log.error("Failed to upload document", e);
      throw new RuntimeException("Failed to upload document: " + e.getMessage());
    }
  }

  /** Delete document from local storage */
  public void deleteDocument(String fileUrl) {
    try {
      // Convert URL to path: /uploads/plafonds/1/file.jpg ->
      // uploads/plafonds/1/file.jpg
      String relativePath =
          fileUrl.startsWith("/uploads/") ? fileUrl.substring("/uploads/".length()) : fileUrl;
      Path filePath = Paths.get(uploadDir, relativePath);

      if (Files.exists(filePath)) {
        Files.delete(filePath);
        log.info("Document deleted: {}", filePath);
      }
    } catch (IOException e) {
      log.error("Failed to delete document: {}", fileUrl, e);
    }
  }

  /** Get file extension from filename */
  private String getFileExtension(String filename) {
    if (filename == null || !filename.contains(".")) {
      return "";
    }
    return filename.substring(filename.lastIndexOf("."));
  }
}
