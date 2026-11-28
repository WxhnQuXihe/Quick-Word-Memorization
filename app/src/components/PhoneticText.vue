<script setup lang="ts">
withDefaults(defineProps<{
  uk?: string | null
  us?: string | null
  small?: boolean
}>(), {
  uk: '',
  us: '',
  small: false
})
</script>

<template>
  <span v-if="uk || us" class="phonetic-text" :class="{ small }">
    <span v-if="uk" class="phonetic-item">
      <span class="phonetic-label">英音</span>
      <span class="phonetic-value">{{ uk }}</span>
    </span>
    <span v-if="us" class="phonetic-item">
      <span class="phonetic-label">美音</span>
      <span class="phonetic-value">{{ us }}</span>
    </span>
  </span>
</template>

<style scoped>
.phonetic-text {
  display: inline-flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 2px 12px;
  min-width: 0;
  color: var(--muted);
  font-size: 12px;
  line-height: 1.55;
}
.phonetic-text.small {
  gap: 1px 9px;
  font-size: 11px;
}
.phonetic-item {
  display: inline-flex;
  align-items: baseline;
  gap: 4px;
  min-width: 0;
}
.phonetic-label {
  flex: none;
  color: var(--accent);
  font-family: "Iowan Old Style", "Palatino Linotype", Georgia, serif;
  font-size: 10px;
  font-style: italic;
  line-height: 1.3;
}
.small .phonetic-label {
  font-size: 9px;
}
.phonetic-value {
  overflow-wrap: anywhere;
}
</style>
