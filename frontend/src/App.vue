<template>
  <div class="page-container">
    <Navbar />
    <div class="page-content">
      <router-view />
    </div>
  </div>
</template>

<script setup>
import Navbar from './components/Navbar.vue'
</script>
