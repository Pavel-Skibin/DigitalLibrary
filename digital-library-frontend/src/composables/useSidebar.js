// Shared module-level state — one instance across all components
import { ref } from "vue";

const isMobileOpen = ref(false);

export function useSidebar() {
  function toggleMobile() {
    isMobileOpen.value = !isMobileOpen.value;
  }
  function closeMobile() {
    isMobileOpen.value = false;
  }
  return { isMobileOpen, toggleMobile, closeMobile };
}
