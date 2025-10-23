import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { getCookie } from '@/utils/cookies'

export function useUser() {
    const username = ref(null)
    const loading = ref(false)
    const router = useRouter()

    async function fetchUserProfile() {
        loading.value = true
        try {
            const jwt = getCookie('jwt')

            const response = await fetch('/api/users/me', {
                credentials: 'include',
                headers: jwt ? { 'Authorization': `Bearer ${jwt}` } : {}
            })

            if (response.ok) {
                const user = await response.json()
                username.value = user.username
                return user
            } else {
                console.warn('Не авторизован')
                router.push('/login')
                return null
            }
        } catch (error) {
            console.error('Ошибка загрузки профиля:', error)
            router.push('/login')
            return null
        } finally {
            loading.value = false
        }
    }

    async function logout() {
        try {
            await fetch('/api/auth/logout', {
                method: 'POST',
                credentials: 'include'
            })
        } catch (e) {
            console.error('Logout error:', e)
        }

        // Очищаем cookie (если есть)
        document.cookie = 'jwt=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;'

        username.value = null
        router.push('/login')
    }

    return {
        username,
        loading,
        fetchUserProfile,
        logout
    }
}