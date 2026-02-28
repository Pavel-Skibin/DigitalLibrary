<template>
  <div id="app">
    <router-view></router-view>
    <AiChatWidget />
  </div>
</template>

<script setup>
import AiChatWidget from "@/components/chat/AiChatWidget.vue";
</script>

<script>
export default {
  name: "App",
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
