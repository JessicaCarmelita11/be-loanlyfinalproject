package com.example.loanlyFinalProject.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "plafond_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlafondDocument {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_plafond_id", nullable = false)
  @JsonIgnore
  private UserPlafond userPlafond;

  @Column(name = "document_type", nullable = false, length = 30)
  @Enumerated(EnumType.STRING)
  private DocumentType documentType;

  @Column(name = "file_url", nullable = false, length = 500)
  private String fileUrl;

  @Column(name = "file_name", length = 100)
  private String fileName;

  @Column(name = "uploaded_at", nullable = false, updatable = false)
  private LocalDateTime uploadedAt;

  @PrePersist
  protected void onCreate() {
    uploadedAt = LocalDateTime.now();
  }

  public enum DocumentType {
    KTP,
    KK,
    SLIP_GAJI,
    NPWP,
    SURAT_KETERANGAN_KERJA,
    REKENING_KORAN,
    OTHER
  }
}
