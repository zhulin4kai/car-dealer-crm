<script setup lang="ts">
import ConfirmDialog from '@/shared/ui/ConfirmDialog.vue'
import { Toaster } from '@/components/ui/sonner'
</script>

<template>
  <router-view/>
  <Toaster :duration="3000" rich-colors position="top-center" />
  <ConfirmDialog />
</template>
