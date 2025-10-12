import { ref } from 'vue'

export function useExtendedSearch() {
    const authors = ref([])
    const genres = ref([])
    const showModal = ref(false)
    const filters = ref({
        title: '',
        selectedAuthors: [],
        selectedGenres: [],
        minRating: null,
        maxRating: null,
        sortBy: 'titleAsc'
    })
    const activeFilters = ref(null)

    async function loadAuthors() {
        try {
            const response = await fetch('http://localhost:8080/api/authors?size=1000')
            if (!response.ok) throw new Error('Ошибка загрузки авторов')
            const data = await response.json()
            authors.value = data.content || []
        } catch (error) {
            console.error('Ошибка загрузки авторов:', error)
        }
    }

    async function loadGenres() {
        try {
            const response = await fetch('http://localhost:8080/api/genres')
            if (!response.ok) throw new Error('Ошибка загрузки жанров')
            genres.value = await response.json()
        } catch (error) {
            console.error('Ошибка загрузки жанров:', error)
        }
    }

    function openModal(currentSearchQuery = '') {
        if (activeFilters.value) {
            filters.value = { ...activeFilters.value }
        } else {
            filters.value.title = currentSearchQuery
        }
        showModal.value = true
    }

    function closeModal() {
        showModal.value = false
    }

    function resetFilters() {
        filters.value = {
            title: '',
            selectedAuthors: [],
            selectedGenres: [],
            minRating: null,
            maxRating: null,
            sortBy: 'titleAsc'
        }
        activeFilters.value = null
    }

    function buildSearchUrl(page, pageSize) {
        const params = new URLSearchParams()
        params.append('page', page.toString())
        params.append('size', pageSize.toString())
        params.append('title', activeFilters.value.title.trim())

        activeFilters.value.selectedAuthors.forEach(authorId => {
            params.append('authorIds', authorId.toString())
        })

        activeFilters.value.selectedGenres.forEach(genreId => {
            params.append('genreIds', genreId.toString())
        })

        if (activeFilters.value.minRating !== null && activeFilters.value.minRating !== '') {
            params.append('minRating', activeFilters.value.minRating.toString())
        }
        if (activeFilters.value.maxRating !== null && activeFilters.value.maxRating !== '') {
            params.append('maxRating', activeFilters.value.maxRating.toString())
        }

        if (activeFilters.value.sortBy === 'titleAsc') {
            params.append('sort', 'title,asc')
        } else if (activeFilters.value.sortBy === 'ratingDesc') {
            params.append('sort', 'averageRating,desc')
        }

        return `http://localhost:8080/api/books/search?${params.toString()}`
    }

    function performSearch(newFilters) {
        activeFilters.value = {
            title: newFilters.title,
            selectedAuthors: [...newFilters.selectedAuthors],
            selectedGenres: [...newFilters.selectedGenres],
            minRating: newFilters.minRating,
            maxRating: newFilters.maxRating,
            sortBy: newFilters.sortBy
        }



        closeModal()
        return activeFilters.value
    }

    return {
        authors,
        genres,
        showModal,
        filters,
        activeFilters,
        loadAuthors,
        loadGenres,
        openModal,
        closeModal,
        resetFilters,
        buildSearchUrl,
        performSearch
    }
}