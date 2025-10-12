import { ref } from 'vue'
import { getCookie } from '@/utils/cookies'

export function useAdminAuth() {
    const isAdmin = ref(false)
    const isModerator = ref(false)
    const isLoading = ref(false)

    async function checkAccess() {
        isLoading.value = true
        const jwt = getCookie('jwt')

        if (!jwt) {
            return false
        }

        try {
            const response = await fetch('http://localhost:8080/api/users/me', {
                headers: { 'Authorization': `Bearer ${jwt}` }
            })

            if (response.ok) {
                const userData = await response.json()
                console.log('User role:', userData.roleName)

                isAdmin.value = userData.roleName === 'ROLE_ADMIN' || userData.roleName === 'ADMIN'
                isModerator.value = userData.roleName === 'ROLE_MODERATOR' || userData.roleName === 'MODERATOR'

                return isAdmin.value || isModerator.value
            }

            return false
        } catch (error) {
            console.error('Auth check error:', error)
            return false
        } finally {
            isLoading.value = false
        }
    }

    return {
        isAdmin,
        isModerator,
        isLoading,
        checkAccess
    }
}