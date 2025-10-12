import { ref } from 'vue'
import * as authorsApi from '@/api/authors'

export function useAuthors() {
    const authors = ref([])
    const loading = ref(false)
    const currentPage = ref(0)
    const totalPages = ref(0)
    const totalElements = ref(0)
    const pageSize = ref(20)
    const searchQuery = ref('')
    const isSearchMode = ref(false)

    let searchTimeout = null

    async function loadAuthors(page = 0, query = '') {
        loading.value = true
        isSearchMode.value = !!query.trim()

        try {
            const data = await authorsApi.getAuthors(page, pageSize.value, query)
            authors.value = data.content || []
            currentPage.value = data.number
            totalPages.value = data.totalPages
            totalElements.value = data.totalElements
        } catch (error) {
            console.error('Error loading authors:', error)
            alert('Не удалось загрузить авторов')
            authors.value = []
        } finally {
            loading.value = false
        }
    }

    function performSearch(query) {
        searchQuery.value = query
        if (!query.trim()) {
            resetToMainList()
            return
        }
        loadAuthors(0, query)
    }

    function handleSearchInput(query) {
        searchQuery.value = query

        if (!query.trim()) {
            resetToMainList()
            return
        }

        clearTimeout(searchTimeout)
        searchTimeout = setTimeout(() => {
            if (query.length >= 2) {
                performSearch(query)
            }
        }, 300)
    }

    function resetToMainList() {
        isSearchMode.value = false
        searchQuery.value = ''
        loadAuthors(0)
    }

    function nextPage() {
        if (currentPage.value < totalPages.value - 1) {
            loadAuthors(currentPage.value + 1, searchQuery.value)
        }
    }

    function prevPage() {
        if (currentPage.value > 0) {
            loadAuthors(currentPage.value - 1, searchQuery.value)
        }
    }

    async function saveAuthor(id, authorData) {
        try {
            if (id) {
                await authorsApi.updateAuthor(id, authorData.firstName, authorData.lastName)
                alert('Автор обновлён!')
            } else {
                await authorsApi.createAuthor(authorData.firstName, authorData.lastName)
                alert('Автор добавлен!')
            }
        } catch (error) {
            alert(`Ошибка: ${error.message}`)
            throw error
        }
    }

    async function deleteAuthor(id) {
        try {
            await authorsApi.deleteAuthor(id)
            alert('Автор удалён!')
        } catch (error) {
            alert(`Ошибка: ${error.message}`)
            throw error
        }
    }

    return {
        authors,
        loading,
        currentPage,
        totalPages,
        totalElements,
        searchQuery,
        isSearchMode,
        loadAuthors,
        performSearch,
        handleSearchInput,
        resetToMainList,
        nextPage,
        prevPage,
        saveAuthor,
        deleteAuthor
    }
}