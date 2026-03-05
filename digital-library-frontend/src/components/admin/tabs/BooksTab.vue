<template>
  <div class="tab-panel">
    <div class="panel-header">
      <h2>Управление книгами</h2>
      <button @click="openModal()" class="btn-primary">+ Добавить книгу</button>
    </div>

    <SearchInput v-model="searchQuery" placeholder="🔍 Поиск книги..." />

    <div v-if="loading" class="loading">Загрузка...</div>

    <DataTable v-else :columns="columns" :data="books">
      <template #cell-authors="{ row }">
        {{ row.authors.join(", ") }}
      </template>
      <template #cell-genres="{ row }">
        {{ row.genres.join(", ") }}
      </template>
      <template #cell-averageRating="{ row }">
        {{ row.averageRating ? row.averageRating.toFixed(2) : "0.00" }}
      </template>
      <template #actions="{ row }">
        <button @click="openModal(row)" class="btn-edit">
          ✏️ Редактировать
        </button>
        <button @click="openVectorizationModal(row)" class="btn-vectorize">
          Векторизовать
        </button>
        <button @click="handleDelete(row.id)" class="btn-delete">
          Удалить
        </button>
      </template>
    </DataTable>

    <Pagination
      :current-page="currentPage"
      :total-pages="totalPages"
      @page-change="loadPage"
    />

    <BookModal
      v-if="showModal"
      :book="editingBook"
      :authors="allAuthors"
      :genres="allGenres"
      :tags="allTags"
      :loading="saving"
      @close="closeModal"
      @save="handleSave"
    />

    <BookVectorizationModal
      v-if="showVectorizationModal && vectorizingBook"
      :book="vectorizingBook"
      @close="
        showVectorizationModal = false;
        vectorizingBook = null;
      "
    />
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from "vue";
import { getCookie } from "@/utils/cookies";
import SearchInput from "@/components/ui/SearchInput.vue";
import DataTable from "@/components/ui/DataTable.vue";
import Pagination from "@/components/ui/Pagination.vue";
import BookModal from "@/components/admin/modals/BookModal.vue";
import BookVectorizationModal from "@/components/admin/modals/BookVectorizationModal.vue";
import { deleteVectorization } from "@/api/aiAdmin";

const books = ref([]);
const allAuthors = ref([]);
const allGenres = ref([]);
const allTags = ref([]);
const loading = ref(false);
const saving = ref(false);
const searchQuery = ref("");
const currentPage = ref(0);
const totalPages = ref(0);
const pageSize = 10;

const showModal = ref(false);
const editingBook = ref(null);
const showVectorizationModal = ref(false);
const vectorizingBook = ref(null);

const columns = [
  { key: "id", label: "ID" },
  { key: "title", label: "Название" },
  { key: "authors", label: "Авторы" },
  { key: "genres", label: "Жанры" },
  { key: "averageRating", label: "Рейтинг" },
];

let searchTimeout = null;

// Загрузка книг
async function loadBooks(page = 0) {
  loading.value = true;
  try {
    const jwt = getCookie("jwt");
    const query = searchQuery.value.trim();
    const url = query
      ? `/api/books/search?title=${encodeURIComponent(query)}&page=${page}&size=${pageSize}`
      : `/api/books?page=${page}&size=${pageSize}`;

    const response = await fetch(url, {
      headers: { Authorization: `Bearer ${jwt}` },
    });

    if (response.ok) {
      const data = await response.json();
      books.value = data.content || [];
      currentPage.value = data.number;
      totalPages.value = data.totalPages;
    }
  } catch (error) {
    console.error("Ошибка загрузки книг:", error);
    alert("Не удалось загрузить книги");
  } finally {
    loading.value = false;
  }
}

function loadPage(page) {
  loadBooks(page);
}

// Загрузка авторов
async function loadAuthors() {
  try {
    const jwt = getCookie("jwt");
    const response = await fetch("/api/authors?page=0&size=1000", {
      headers: { Authorization: `Bearer ${jwt}` },
    });

    if (response.ok) {
      const data = await response.json();
      allAuthors.value = data.content || [];
    }
  } catch (error) {
    console.error("Ошибка загрузки авторов:", error);
  }
}

// Загрузка жанров
async function loadGenres() {
  try {
    const jwt = getCookie("jwt");
    const response = await fetch("/api/genres", {
      headers: { Authorization: `Bearer ${jwt}` },
    });

    if (response.ok) {
      allGenres.value = await response.json();
    }
  } catch (error) {
    console.error("Ошибка загрузки жанров:", error);
  }
}
// Загрузка тегов
async function loadTags() {
  try {
    const jwt = getCookie("jwt");
    const response = await fetch("/api/tags", {
      headers: { Authorization: `Bearer ${jwt}` },
    });
    if (response.ok) {
      allTags.value = await response.json();
    }
  } catch (error) {
    console.error("Ошибка загрузки тегов:", error);
  }
}
// Поиск с задержкой
watch(searchQuery, () => {
  clearTimeout(searchTimeout);
  searchTimeout = setTimeout(() => {
    loadBooks(0);
  }, 300);
});

// Модальное окно
async function openModal(book = null) {
  // Загружаем авторов, жанры и теги если их нет
  if (allAuthors.value.length === 0) await loadAuthors();
  if (allGenres.value.length === 0) await loadGenres();
  if (allTags.value.length === 0) await loadTags();

  editingBook.value = book;
  showModal.value = true;
}

function closeModal() {
  showModal.value = false;
  editingBook.value = null;
}

function openVectorizationModal(book) {
  vectorizingBook.value = book;
  showVectorizationModal.value = true;
}

// Сохранение
async function handleSave(formData) {
  saving.value = true;
  try {
    const jwt = getCookie("jwt");
    const url = editingBook.value
      ? `/api/books/${editingBook.value.id}`
      : "/api/books";

    const requestBody = {
      title: formData.title?.trim() || "",
      description: formData.description?.trim() || null,
      authorIds: formData.authorIds || [],
      genreIds: formData.genreIds || [],
      tagIds: formData.tagIds?.length ? formData.tagIds : null,
      publicationYear: formData.publicationYear || null,
      language: formData.language || null,
      ageRating: formData.ageRating || null,
      seriesName: formData.seriesName || null,
      seriesNumber: formData.seriesNumber || null,
      wordCount: formData.wordCount || null,
    };

    // Добавляем filePath только если он был передан
    if (formData.filePath != null) {
      requestBody.filePath = formData.filePath.trim();
    }

    const response = await fetch(url, {
      method: editingBook.value ? "PUT" : "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${jwt}`,
      },
      body: JSON.stringify(requestBody),
    });

    if (response.ok) {
      alert(editingBook.value ? "Книга обновлена!" : "Книга добавлена!");
      closeModal();
      allTags.value = []; // сбрасываем кэш тегов, чтобы новые теги появились при следующем открытии
      await loadBooks(currentPage.value);
    } else {
      const error = await response.text();
      alert(`Ошибка: ${error}`);
    }
  } catch (error) {
    console.error("Ошибка сохранения книги:", error);
    alert("Не удалось сохранить книгу");
  } finally {
    saving.value = false;
  }
}

// Удаление
async function handleDelete(bookId) {
  if (!confirm("Вы уверены, что хотите удалить эту книгу?")) return;

  try {
    const jwt = getCookie("jwt");
    const response = await fetch(`/api/books/${bookId}`, {
      method: "DELETE",
      headers: { Authorization: `Bearer ${jwt}` },
    });

    if (response.ok) {
      alert("Книга удалена!");
      deleteVectorization(bookId).catch(() => {});
      await loadBooks(currentPage.value);
    } else {
      const error = await response.text();
      alert(`Ошибка: ${error}`);
    }
  } catch (error) {
    console.error("Ошибка удаления книги:", error);
    alert("Не удалось удалить книгу");
  }
}

onMounted(() => {
  loadBooks(0);
});
</script>
