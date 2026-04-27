package com.tpa.entity;

import com.tpa.enums.DocumentType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "claim_documents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentType type;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String filePath;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "claim_id", nullable = false)
    private Claim claim;

    @Column(name = "validation_status")
    private String validationStatus; // VALID or INVALID

    @Column(name = "validation_issues", columnDefinition = "TEXT")
    private String validationIssues;

    @Column(name = "confidence_score")
    private Integer confidenceScore;

    @Column(name = "file_type")
    private String fileType; // PDF or IMAGE
}
