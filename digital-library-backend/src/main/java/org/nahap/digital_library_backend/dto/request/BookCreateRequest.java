package org.nahap.digital_library_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record BookCreateRequest(
        @NotBlank(message = "Название книги не может быть пустым")
        String title,

        String description,

        @NotBlank(message = "Путь к файлу книги не может быть пустым")
        String filePath,

        @NotNull(message = "Список ID авторов не может быть null")
        @NotEmpty(message = "Должен быть указан хотя бы один автор")
        List<@NotNull Integer> authorIds,

        @NotNull(message = "Список ID жанров не может быть null")
        @NotEmpty(message = "Должен быть указан хотя бы один жанр")
        List<@NotNull Integer> genreIds
) {}