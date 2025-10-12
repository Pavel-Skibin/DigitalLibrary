import { ref } from 'vue'
import { makeFB2 } from '@/foliate-js/fb2.js'

export function useBookCover() {
    const coverImageUrls = ref({})

    async function fetchBookCover(bookId) {
        if (coverImageUrls.value[bookId]) return // Уже загружена

        try {
            const response = await fetch(`http://localhost:8080/api/books/${bookId}/fb2`)
            if (!response.ok) throw new Error('Ошибка загрузки FB2')

            const fb2Blob = await response.blob()
            const book = await makeFB2(fb2Blob)

            if (book.getCover) {
                const coverBlob = await book.getCover()
                coverImageUrls.value[bookId] = URL.createObjectURL(coverBlob)
            } else {
                coverImageUrls.value[bookId] = null
            }
        } catch (error) {
            console.error(`Ошибка загрузки обложки ${bookId}:`, error)
            coverImageUrls.value[bookId] = null
        }
    }

    return {
        coverImageUrls,
        fetchBookCover
    }
}