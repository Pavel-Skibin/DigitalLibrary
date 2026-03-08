package org.nahap.bookcatalogservice.service;

/**
 * Сервис для работы с обложками книг в формате FB2
 */
public interface BookCoverService {

    /**
     * Извлекает обложку из FB2-файла и сохраняет её на диск
     *
     * @param fb2Content XML-контент FB2-файла
     * @return относительный путь к сохранённой обложке или null, если обложка не найдена
     */
    String extractAndSaveCover(String fb2Content);

    /**
     * Удаляет файл обложки с диска
     *
     * @param coverImagePath относительный путь к файлу обложки
     */
    void deleteCover(String coverImagePath);
}