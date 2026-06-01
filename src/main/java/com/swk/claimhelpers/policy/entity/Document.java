package com.swk.claimhelpers.policy.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // claim_criteria와 1:1 관계 — UNIQUE 제약으로 DB 레벨에서 보장
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "claim_criteria_id", nullable = false, unique = true)
    private ClaimCriteria claimCriteria;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    // S3 오브젝트 키 — 업로드 완료 전에는 null
    @Column(name = "object_key", length = 1000)
    private String objectKey;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static Document create(ClaimCriteria claimCriteria, String fileName, Long fileSize) {
        Document document = new Document();
        document.claimCriteria = claimCriteria;
        document.fileName = fileName;
        document.fileSize = fileSize;
        return document;
    }

    public void updateObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }
}