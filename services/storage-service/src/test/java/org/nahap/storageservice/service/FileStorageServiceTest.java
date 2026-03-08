package org.nahap.storageservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.nahap.storageservice.config.StorageProperties;
import org.nahap.storageservice.dto.FileUploadResponse;
import org.nahap.storageservice.exception.FileStorageException;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    @Mock
    private StorageProperties storageProperties;

    @InjectMocks
    private FileStorageService fileStorageService;

    private Path booksDir;
    private Path coversDir;

    @BeforeEach
    void setUp() {
        booksDir = tempDir.resolve("books");
        coversDir = tempDir.resolve("covers");
        when(storageProperties.getBooksPath()).thenReturn(booksDir.toString());
        when(storageProperties.getCoversPath()).thenReturn(coversDir.toString());
    }

    @Test
    void uploadBook_validFb2File_returnsFileUploadResponse() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.fb2",
                "application/octet-stream",
                "fb2 content".getBytes()
        );

        FileUploadResponse response = fileStorageService.uploadBook(file, "Иванов", "ТестоваяКнига");

        assertThat(response).isNotNull();
        assertThat(response.filePath()).startsWith("/");
        assertThat(response.originalName()).isEqualTo("test.fb2");
        assertThat(response.size()).isGreaterThan(0);
    }

    @Test
    void uploadBook_emptyFile_throwsFileStorageException() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.fb2",
                "application/octet-stream",
                new byte[0]
        );

        assertThatThrownBy(() -> fileStorageService.uploadBook(emptyFile, null, null))
                .isInstanceOf(FileStorageException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void uploadBook_withoutAuthorAndTitle_usesOriginalFilename() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "mybook.fb2",
                "application/octet-stream",
                "content".getBytes()
        );

        FileUploadResponse response = fileStorageService.uploadBook(file, null, null);

        assertThat(response).isNotNull();
        assertThat(response.savedName()).contains("mybook");
    }

    @Test
    void uploadCover_validJpegFile_returnsFileUploadResponse() {
        MockMultipartFile coverFile = new MockMultipartFile(
                "file", "cover.jpg",
                "image/jpeg",
                "jpeg content".getBytes()
        );

        FileUploadResponse response = fileStorageService.uploadCover(coverFile);

        assertThat(response).isNotNull();
        assertThat(response.filePath()).startsWith("covers/");
        assertThat(response.originalName()).isEqualTo("cover.jpg");
    }

    @Test
    void uploadCover_emptyFile_throwsFileStorageException() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "cover.jpg",
                "image/jpeg",
                new byte[0]
        );

        assertThatThrownBy(() -> fileStorageService.uploadCover(emptyFile))
                .isInstanceOf(FileStorageException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void uploadCover_nonImageFile_throwsFileStorageException() {
        MockMultipartFile textFile = new MockMultipartFile(
                "file", "document.pdf",
                "application/pdf",
                "pdf content".getBytes()
        );

        assertThatThrownBy(() -> fileStorageService.uploadCover(textFile))
                .isInstanceOf(FileStorageException.class);
    }

    @Test
    void loadFile_existingBookFile_returnsResource() throws IOException {
        Files.createDirectories(booksDir);
        Path bookFile = booksDir.resolve("Иванов.ТестоваяКнига.fb2");
        Files.writeString(bookFile, "fb2 content");

        Resource resource = fileStorageService.loadFile("/Иванов.ТестоваяКнига.fb2");

        assertThat(resource).isNotNull();
        assertThat(resource.exists()).isTrue();
    }

    @Test
    void loadFile_nonExistentFile_throwsFileStorageException() {
        assertThatThrownBy(() -> fileStorageService.loadFile("/nonexistent.fb2"))
                .isInstanceOf(FileStorageException.class);
    }

    @Test
    void deleteFile_existingFile_deletesSuccessfully() throws IOException {
        Files.createDirectories(booksDir);
        Path bookFile = booksDir.resolve("delete_me.fb2");
        Files.writeString(bookFile, "content");

        assertThat(bookFile).exists();

        fileStorageService.deleteFile("/delete_me.fb2");

        assertThat(bookFile).doesNotExist();
    }

    @Test
    void deleteFile_nonExistentFile_doesNotThrow() {
        assertThatCode(() -> fileStorageService.deleteFile("/nonexistent.fb2"))
                .doesNotThrowAnyException();
    }
}
