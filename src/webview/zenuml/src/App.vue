<template>
  <div :class="theme">
    <div class="container">
      <Editor
        :initial-content="content"
        @content-change="handleContentChange"
      />
      <Viewer :content="content" />
    </div>
  </div>
</template>

<script setup>
import { ref, inject, onMounted } from 'vue'
import Editor from './components/Editor.vue'
import Viewer from './components/Viewer.vue'
import { useHostCommunication } from './composables/useHostCommunication'

// Get initial theme from injected data
const initialData = inject('initialData', { theme: 'light' })
const theme = ref(initialData.theme === 'dark' ? 'dark-theme' : 'light-theme')
const content = ref('')
const { messages, sendToHost, getHost } = useHostCommunication()

// Handle content changes from editor
const handleContentChange = (newContent) => {
  content.value = newContent
  // Notify the host about content changes
  sendToHost({
    event: 'contentChanged',
    code: newContent
  })
}

// Set up host communication
onMounted(() => {
  console.log('App mounted, setting up host communication')
  
  // Set up initial theme
  document.body.classList.add(theme.value)

  // Add a message listener for the host
  const host = getHost()

  const handleHostMessage = (message) => {
    console.log('Host message in App.vue:', message)

    // Handle theme changes
    if (message.theme) {
      theme.value = message.theme === 'dark' ? 'dark-theme' : 'light-theme'
      document.body.className = theme.value
    }

    // Check for string message that needs parsing
    const msg = typeof message === 'string' ? JSON.parse(message) : message
    
    // Handle initial content loading
    if (msg.action === 'load' && msg.code) {
      console.log('Received file content:', msg.code)
      content.value = msg.code
    }
  }

  host.addMessageListener(handleHostMessage)
  
  // Explicitly request content from the host after initialization
  setTimeout(() => {
    console.log('Requesting content from host')
    sendToHost({
      event: 'ready',
      message: 'Editor is ready to receive content'
    })
  }, 500)

  // Add a sample content if empty - but only after waiting for host communication
  setTimeout(() => {
    if (!content.value) {
      console.log('No content received, using sample')
      content.value = `// Sample ZenUML Diagram
@Actor User
@Boundary UI
@Control Controller
@Entity Database

User -> UI.click() {
  Controller.process() {

  }
}`
    }
  }, 1000) // Wait longer before falling back to sample content
})
</script>

<style>
/* Global styles in main.css */
</style>
