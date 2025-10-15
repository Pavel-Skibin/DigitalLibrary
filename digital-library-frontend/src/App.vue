<template>
  <div id="app">
    <router-view></router-view>
  </div>
</template>


<script setup>
import { onMounted } from 'vue';
import { useRouter } from 'vue-router';

const router = useRouter();

onMounted(() => {
  const token = localStorage.getItem('token');
  if (!token) {
    router.push('/login');
  }
});
</script>

<script>

export default {
  name: 'App',
  components: {
  }
};
</script>

<style>
#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: #5c4033;
  background-color: #fdf6e9;
  height: 100vh;
  overflow: hidden;
}
</style>