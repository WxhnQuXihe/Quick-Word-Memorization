<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const showTab = computed(() => route.name === 'home' || route.name === 'me')
</script>

<template>
  <router-view />
  <van-tabbar v-if="showTab" route active-color="#B4432F" inactive-color="#8A8177" safe-area-inset-bottom>
    <van-tabbar-item replace to="/" icon="home-o">主页</van-tabbar-item>
    <van-tabbar-item replace to="/me" icon="user-o">我的</van-tabbar-item>
  </van-tabbar>
</template>
