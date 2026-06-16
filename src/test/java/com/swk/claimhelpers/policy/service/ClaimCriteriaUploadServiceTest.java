package com.swk.claimhelpers.policy.service;

import com.swk.claimhelpers.common.exception.CustomException;
import com.swk.claimhelpers.common.exception.ErrorCode;
import com.swk.claimhelpers.common.storage.S3FileStorage;
import com.swk.claimhelpers.policy.entity.ClaimCriteria;
import com.swk.claimhelpers.policy.entity.ClaimCriteriaStatus;
import com.swk.claimhelpers.policy.entity.Document;
import com.swk.claimhelpers.policy.repository.ClaimCriteriaRepository;
import com.swk.claimhelpers.policy.repository.DocumentRepository;
import com.swk.claimhelpers.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ClaimCriteriaUploadServiceTest {

    @Mock
    private ClaimCriteriaRepository claimCriteriaRepository;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private S3FileStorage s3FileStorage;

    @Mock
    private MultipartProperties multipartProperties;

    @InjectMocks
    private ClaimCriteriaUploadService uploadService;

    // multipart 한도 30MB → 앱 검증 한도는 그 80% = 24MB
    private void givenMultipartLimit30Mb() {
        given(multipartProperties.getMaxFileSize()).willReturn(DataSize.ofMegabytes(30));
    }

    private MultipartFile pdfFile(long size) throws IOException {
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
        given(file.getContentType()).willReturn("application/pdf");
        given(file.getSize()).willReturn(size);
        return file;
    }

    @Test
    @DisplayName("PDF가 아닌 파일이면 INVALID_FILE_TYPE 예외를 던진다")
    void PDF가_아니면_예외() {
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
        given(file.getContentType()).willReturn("image/png");

        assertThatThrownBy(() -> uploadService.upload(file, null, "session-1"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_FILE_TYPE);

        then(s3FileStorage).should(never()).upload(anyString(), any(), anyLong(), anyString());
    }

    @Test
    @DisplayName("앱 한도를 초과하면 FILE_SIZE_EXCEEDED 예외를 던진다")
    void 크기_초과면_예외() {
        givenMultipartLimit30Mb();
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
        given(file.getContentType()).willReturn("application/pdf");
        given(file.getSize()).willReturn(DataSize.ofMegabytes(25).toBytes()); // 24MB 초과

        assertThatThrownBy(() -> uploadService.upload(file, null, "session-1"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FILE_SIZE_EXCEEDED);

        then(s3FileStorage).should(never()).upload(anyString(), any(), anyLong(), anyString());
    }

    @Test
    @DisplayName("정상 업로드: 메타데이터 저장 + S3 업로드 호출 + 상태 PROCESSING")
    void 정상_업로드() throws IOException {
        givenMultipartLimit30Mb();
        long size = DataSize.ofMegabytes(10).toBytes();
        MultipartFile file = pdfFile(size);
        given(file.getOriginalFilename()).willReturn("약관.pdf");
        InputStream stream = new ByteArrayInputStream(new byte[]{1, 2, 3});
        given(file.getInputStream()).willReturn(stream);

        Document document = uploadService.upload(file, null, "session-1");

        // claim_criteria + documents 저장
        then(claimCriteriaRepository).should().save(any(ClaimCriteria.class));
        then(documentRepository).should().save(any(Document.class));
        // S3 업로드는 InputStream 시그니처로 호출
        then(s3FileStorage).should().upload(anyString(), eq(stream), eq(size), eq("application/pdf"));
        // object_key 설정 + 상태 PROCESSING 전환
        assertThat(document.getObjectKey()).isNotBlank();
        assertThat(document.getClaimCriteria().getStatus()).isEqualTo(ClaimCriteriaStatus.PROCESSING);
    }

    @Test
    @DisplayName("로그인 사용자는 user 로 소유자를 잡고 session_key 는 비운다")
    void 로그인_사용자_분기() throws IOException {
        givenMultipartLimit30Mb();
        MultipartFile file = pdfFile(DataSize.ofMegabytes(1).toBytes());
        given(file.getOriginalFilename()).willReturn("약관.pdf");
        given(file.getInputStream()).willReturn(new ByteArrayInputStream(new byte[]{1}));
        User user = User.create("a@gmail.com");

        Document document = uploadService.upload(file, user, null);

        ClaimCriteria claimCriteria = document.getClaimCriteria();
        assertThat(claimCriteria.getUser()).isSameAs(user);
        assertThat(claimCriteria.getSessionKey()).isNull();
    }

    @Test
    @DisplayName("비로그인 사용자는 session_key 로 소유자를 잡고 user 는 비운다")
    void 비로그인_사용자_분기() throws IOException {
        givenMultipartLimit30Mb();
        MultipartFile file = pdfFile(DataSize.ofMegabytes(1).toBytes());
        given(file.getOriginalFilename()).willReturn("약관.pdf");
        given(file.getInputStream()).willReturn(new ByteArrayInputStream(new byte[]{1}));

        Document document = uploadService.upload(file, null, "session-1");

        ClaimCriteria claimCriteria = document.getClaimCriteria();
        assertThat(claimCriteria.getSessionKey()).isEqualTo("session-1");
        assertThat(claimCriteria.getUser()).isNull();
    }
}