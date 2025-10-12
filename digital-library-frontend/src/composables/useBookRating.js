import { ref } from 'vue'
import { getCookie } from '@/utils/cookies'

export function useBookRating() {
    const userRating = ref(null)
    const submitting = ref(false)

    async function loadUserRating(bookId) {
        if (!bookId) return

        try {
            const jwt = getCookie('jwt')
            if (!jwt) return

            const response = await fetch('http://localhost:8080/api/ratings/me', {
                headers: { 'Authorization': `Bearer ${jwt}` }
            })

            if (response.ok) {
                const ratings = await response.json()
                userRating.value = ratings.find(r => r.bookId === bookId)
            }
        } catch (error) {
            console.error('Ошибка загрузки рейтинга:', error)
        }
    }

    async function submitRating(bookId, value) {
        submitting.value = true
        try {
            const jwt = getCookie('jwt')
            const response = await fetch('http://localhost:8080/api/ratings', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${jwt}`
                },
                body: JSON.stringify({ bookId, value })
            })

            if (response.ok) {
                userRating.value = await response.json()
                alert('Оценка успешно сохранена!')
                return true
            }
        } catch (error) {
            console.error('Ошибка сохранения оценки:', error)
            alert('Не удалось сохранить оценку')
            return false
        } finally {
            submitting.value = false
        }
    }

    return {
        userRating,
        submitting,
        loadUserRating,
        submitRating
    }
}