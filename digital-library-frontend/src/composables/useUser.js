import { ref } from "vue";
import { useRouter } from "vue-router";
import { getCookie, deleteCookie } from "@/utils/cookies";

export function useUser() {
  const username = ref(null);
  const userId = ref(null);
  const loading = ref(false);
  const router = useRouter();

  async function fetchUserProfile() {
    loading.value = true;
    try {
      const jwt = getCookie("jwt");

      const response = await fetch("/api/users/me", {
        credentials: "include",
        headers: jwt ? { Authorization: `Bearer ${jwt}` } : {},
      });

      if (response.ok) {
        const user = await response.json();
        username.value = user.username;
        userId.value = user.id;
        return user;
      } else {
        router.push("/login");
        return null;
      }
    } catch (error) {
      router.push("/login");
      return null;
    } finally {
      loading.value = false;
    }
  }

  async function logout() {
    try {
      await fetch("/api/auth/logout", {
        method: "POST",
        credentials: "include",
      });
    } catch (e) {
    }

    // Очищаем cookie используя безопасную функцию
    deleteCookie("jwt");

    username.value = null;
    userId.value = null;
    router.push("/login");
  }

  return {
    username,
    userId,
    loading,
    fetchUserProfile,
    logout,
  };
}
