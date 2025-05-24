<template>
  <div class="build-timestamp" :title="`Built at: ${buildTimestamp}`">
    <span class="timestamp-icon">🕒</span>
    <span class="timestamp-text">{{ formatTimestamp(buildTimestamp) }}</span>
  </div>
</template>

<script setup>
import { computed } from 'vue'

// Get build timestamp from environment variable
const buildTimestamp = import.meta.env.VITE_BUILD_TIMESTAMP || 'Development'

const formatTimestamp = (timestamp) => {
  if (timestamp === 'Development') {
    return 'Dev'
  }

  try {
    const date = new Date(timestamp)
    // Check if the date is valid
    if (Number.isNaN(date.getTime())) {
      console.warn('Invalid timestamp:', timestamp)
      return 'Invalid'
    }
    
    // Format as "MMM dd, HH:mm"
    return date.toLocaleDateString('en-US', {
      month: 'short',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      hour12: false
    }).replace(',', '')
  } catch (e) {
    console.error('Error parsing timestamp:', e, timestamp)
    return 'Error'
  }
}
</script>

<style scoped>
.build-timestamp {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  font-size: 11px;
  color: var(--text-muted, #888);
  background: var(--background-secondary, #f5f5f5);
  border-radius: 4px;
  border: 1px solid var(--border-color, #ddd);
  white-space: nowrap;
  user-select: none;
  cursor: default;
  opacity: 0.7;
  transition: opacity 0.2s ease;
}

.build-timestamp:hover {
  opacity: 1;
}

.timestamp-icon {
  font-size: 10px;
  opacity: 0.6;
}

.timestamp-text {
  font-family: monospace;
  font-size: 10px;
}

/* Dark theme support */
:global(.dark-theme) .build-timestamp {
  background: var(--background-secondary, #2d2d2d);
  color: var(--text-muted, #aaa);
  border-color: var(--border-color, #444);
}
</style>
