package org.nahap.bookcatalogservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.nahap.bookcatalogservice.configuration.BookStorageProperties;
import org.nahap.bookcatalogservice.service.BookCoverService;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.UUID;

/**
 * Реализация сервиса для работы с обложками книг в формате FB2
 */
@Service
@RequiredArgsConstructor
public class BookCoverServiceImpl implements BookCoverService {

    private final BookStorageProperties storageProperties;
    private static final String XLINK_NAMESPACE = "http://www.w3.org/1999/xlink";

    @Override
    public String extractAndSaveCover(String fb2Content) {
        try {
            // fb2Content — уже корректная Java-строка (Unicode).
            // Убираем объявление encoding из XML-декларации, чтобы парсер не пытался
            // переинтерпретировать UTF-8-байты с другой кодировкой (напр. windows-1251).
            String normalized = fb2Content.replaceFirst(
                    "(<\\?xml[^?]*?)\\s+encoding\\s*=\\s*['\"][^'\"]*['\"]", "$1");

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(normalized.getBytes(StandardCharsets.UTF_8)));

            NodeList coverPageNodes = doc.getElementsByTagName("coverpage");
            if (coverPageNodes.getLength() == 0) {
                return null;
            }

            Element coverPage = (Element) coverPageNodes.item(0);
            NodeList imageNodes = coverPage.getElementsByTagName("image");
            if (imageNodes.getLength() == 0) {
                return null;
            }

            Element imageElement = (Element) imageNodes.item(0);
            String href = imageElement.getAttributeNS(XLINK_NAMESPACE, "href");

            if (href == null || href.isEmpty()) {
                href = imageElement.getAttribute("href");
            }

            if (href == null || !href.startsWith("#")) {
                return null;
            }

            String imageId = href.substring(1);

            NodeList binaryNodes = doc.getElementsByTagName("binary");
            Element binaryElement = null;
            for (int i = 0; i < binaryNodes.getLength(); i++) {
                Element binary = (Element) binaryNodes.item(i);
                if (imageId.equals(binary.getAttribute("id"))) {
                    binaryElement = binary;
                    break;
                }
            }

            if (binaryElement == null) {
                return null;
            }

            String base64Data = binaryElement.getTextContent()
                    .trim()
                    .replaceAll("\\s+", "");

            byte[] imageBytes;
            try {
                imageBytes = Base64.getDecoder().decode(base64Data);
            } catch (IllegalArgumentException e) {
                return null;
            }

            String contentType = binaryElement.getAttribute("content-type");
            String extension = getExtensionFromContentType(contentType);
            String fileName = UUID.randomUUID() + extension;

            Path coversDir = Paths.get(storageProperties.getStoragePath(), "covers");
            Files.createDirectories(coversDir);

            Path coverPath = coversDir.resolve(fileName);
            Files.write(coverPath, imageBytes);

            return "covers/" + fileName;

        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void deleteCover(String coverImagePath) {
        if (coverImagePath == null || coverImagePath.isEmpty()) {
            return;
        }

        try {
            Path coverPath = Paths.get(storageProperties.getStoragePath(), coverImagePath);
            if (Files.exists(coverPath)) {
                Files.delete(coverPath);
            }
        } catch (Exception e) {
            // Suppress exception
        }
    }

    /**
     * Определяет расширение файла по MIME-типу
     *
     * @param contentType MIME-тип изображения
     * @return расширение файла с точкой (.jpg, .png, .gif, .webp)
     */
    private String getExtensionFromContentType(String contentType) {
        if (contentType == null || contentType.isEmpty()) {
            return ".jpg";
        }

        contentType = contentType.toLowerCase();

        if (contentType.contains("png")) return ".png";
        if (contentType.contains("jpeg") || contentType.contains("jpg")) return ".jpg";
        if (contentType.contains("gif")) return ".gif";
        if (contentType.contains("webp")) return ".webp";

        return ".jpg";
    }
}