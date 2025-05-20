<template>
  <div class="editor-container">
    <div class="header">ZenUML Code</div>
    <textarea
      ref="editorElement"
      class="editor-content"
      spellcheck="false"
      v-model="editorContent"
      @input="handleContentChange"
    ></textarea>
  </div>
</template>

<script setup>
import { ref, watch, defineProps, defineEmits, onMounted } from 'vue'
import { useHostCommunication } from '../composables/useHostCommunication'

const props = defineProps({
  initialContent: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['content-change'])
const editorContent = ref(props.initialContent)
const editorElement = ref(null)
const { notifyContentChanged, messages, getHost } = useHostCommunication()

const handleContentChange = () => {
  emit('content-change', editorContent.value)
  notifyContentChanged(editorContent.value)
}

// Watch for messages from the host
watch(messages, (newMessages) => {
  const latestMessage = newMessages[newMessages.length - 1]
  if (latestMessage) {
    if (latestMessage.action === 'load' || latestMessage.action === 'update') {
      if (latestMessage.code !== editorContent.value) {
        editorContent.value = latestMessage.code || ''
        emit('content-change', editorContent.value)
      }
    }
  }
}, { deep: true })

// Listen for host messages directly
onMounted(() => {
  const host = getHost()
  
  // Add listener for host messages
  const messageListener = (message) => {
    if (message.action === 'load' || message.action === 'update') {
      if (message.code !== editorContent.value) {
        editorContent.value = message.code || ''
        emit('content-change', editorContent.value)
      }
    }
  }
  
  host.addMessageListener(messageListener)
})
</script>

<style scoped>
/* Styles are in main.css */
</style> 