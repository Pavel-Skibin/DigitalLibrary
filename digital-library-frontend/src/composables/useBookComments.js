import { ref } from 'vue'
import { getCookie } from '@/utils/cookies'

export function useBookComments() {
    const comments = ref([])
    const loading = ref(false)
    const submitting = ref(false)
    const currentPage = ref(0)
    const hasMore = ref(false)
    const pageSize = 10

    async function loadComments(bookId, page = 0) {
        if (!bookId) return

        loading.value = true
        currentPage.value = page

        try {
            const response = await fetch(
                `/api/comments/books/${bookId}?page=${page}&size=${pageSize}&sort=createdAt,desc`
            )

            if (response.ok) {
                const pageData = await response.json()
                const commentsData = pageData.content || []

                if (page === 0) {
                    comments.value = commentsData
                } else {
                    comments.value = [...comments.value, ...commentsData]
                }

                hasMore.value = !pageData.last
            }
        } catch (error) {
            console.error('Ошибка загрузки комментариев:', error)
            comments.value = []
        } finally {
            loading.value = false
        }
    }

    async function submitComment(bookId, text) {
        submitting.value = true
        try {
            const jwt = getCookie('jwt')
            const response = await fetch('/api/comments', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${jwt}`
                },
                body: JSON.stringify({
                    bookId,
                    text
                })
            })

            if (response.ok) {
                const newComment = await response.json()
                comments.value.unshift(newComment)
                alert('Комментарий добавлен!')
                return true
            } else if (response.status === 400) {
                alert('Вы уже оставили комментарий к этой книге')
                return false
            }
        } catch (error) {
            console.error('Ошибка отправки комментария:', error)
            alert('Не удалось отправить комментарий')
            return false
        } finally {
            submitting.value = false
        }
    }

    async function updateComment(commentId, text) {
        submitting.value = true
        try {
            const jwt = getCookie('jwt')
            const response = await fetch(`/api/comments/${commentId}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${jwt}`
                },
                body: JSON.stringify(text)
            })

            if (response.ok) {
                const updatedComment = await response.json()
                const index = comments.value.findIndex(c => c.id === commentId)
                if (index !== -1) {
                    comments.value[index] = updatedComment
                }
                alert('Комментарий обновлен!')
                return true
            }
        } catch (error) {
            console.error('Ошибка обновления комментария:', error)
            alert('Не удалось обновить комментарий')
            return false
        } finally {
            submitting.value = false
        }
    }

    async function deleteComment(commentId) {
        try {
            const jwt = getCookie('jwt')
            const response = await fetch(`/api/comments/${commentId}`, {
                method: 'DELETE',
                headers: { 'Authorization': `Bearer ${jwt}` }
            })

            if (response.ok) {
                const comment = comments.value.find(c => c.id === commentId)
                if (comment) {
                    comment.deletedAt = new Date().toISOString()
                }
                alert('Комментарий удален')
                return true
            }
        } catch (error) {
            console.error('Ошибка удаления:', error)
            alert('Не удалось удалить комментарий')
            return false
        }
    }

    async function moderateDeleteComment(commentId) {
        try {
            const jwt = getCookie('jwt')
            const response = await fetch(`/api/comments/${commentId}/moderate`, {
                method: 'DELETE',
                headers: { 'Authorization': `Bearer ${jwt}` }
            })

            if (response.ok) {
                const comment = comments.value.find(c => c.id === commentId)
                if (comment) {
                    comment.deletedAt = new Date().toISOString()
                }
                alert('Комментарий удален')
                return true
            }
        } catch (error) {
            console.error('Ошибка удаления:', error)
            alert('Не удалось удалить комментарий')
            return false
        }
    }


    async function restoreComment(commentId) {
        try {
            const jwt = getCookie('jwt')
            const response = await fetch(`/api/comments/${commentId}/restore`, {
                method: 'POST',
                headers: { 'Authorization': `Bearer ${jwt}` }
            })

            if (response.ok) {
                const comment = comments.value.find(c => c.id === commentId)
                if (comment) {
                    comment.deletedAt = null
                }
                alert('Комментарий восстановлен!')
                return true
            }
        } catch (error) {
            console.error('Ошибка восстановления:', error)
            alert('Не удалось восстановить комментарий')
            return false
        }
    }

    function loadMore(bookId) {
        loadComments(bookId, currentPage.value + 1)
    }

    return {
        comments,
        loading,
        submitting,
        hasMore,
        loadComments,
        submitComment,
        updateComment,
        deleteComment,
        moderateDeleteComment,
        restoreComment,
        loadMore
    }
}