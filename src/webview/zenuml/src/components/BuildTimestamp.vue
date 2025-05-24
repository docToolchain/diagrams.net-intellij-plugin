<template>
  <div class="build-timestamp" :title="`Built at: ${buildTimestamp}`">
    <span class="timestamp-text">{{ formatTimestamp(buildTimestamp) }}</span>
  </div>
</template>

<script setup>
import { computed } from 'vue'

// Get build timestamp from environment variable
const buildTimestamp = import.meta.env.VITE_BUILD_TIMESTAMP || 'Development'

const formatTimestamp = (timestamp) => {
  if (timestamp === 'Development') {
    return '1970-01-01T00:00:00.000Z'
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
  gap: 2px;
  padding: 2px 4px;
  font-size: 9px;
  color: var(--text-muted, #ccc);
  white-space: nowrap;
  user-select: none;
  cursor: default;
  opacity: 0.4;
}

.timestamp-icon {
  font-size: 8px;
  opacity: 0.8;
}

.timestamp-text {
  font-family: monospace;
  font-size: 8px;
  font-weight: 300;
}

/* Dark theme support */
:global(.dark-theme) .build-timestamp {
  color: var(--text-muted, #666);
  opacity: 0.4;
}
</style>
