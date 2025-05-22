<template>
  <div :class="theme">
    <Splitpanes class="workspace" :horizontal="isMobile">
      <Pane min-size="20" :size="30">
        <Editor
          :initial-content="content"
          @content-change="handleContentChange"
        />
      </Pane>
      <Pane min-size="20" :size="70">
        <Viewer :content="content" />
      </Pane>
    </Splitpanes>
  </div>
</template>

<script setup>
import { ref, inject, onMounted } from 'vue'
import Editor from './components/Editor.vue'
import Viewer from './components/Viewer.vue'
import { useHostCommunication } from './composables/useHostCommunication'
import { Splitpanes, Pane } from 'splitpanes'
import 'splitpanes/dist/splitpanes.css'

// Get initial theme from injected data
const initialData = inject('initialData', { theme: 'light' })
const theme = ref(initialData.theme === 'dark' ? 'dark-theme' : 'light-theme')
const content = ref('')
const { sendToHost, getHost } = useHostCommunication()

// Responsive handling
const isMobile = ref(window.innerWidth <= 768)
// Update isMobile value on window resize
onMounted(() => {
  window.addEventListener('resize', () => {
    isMobile.value = window.innerWidth <= 768
  })
})

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
/* Override only necessary splitpanes styling to match theme */
.splitpanes__splitter {
  background-color: var(--border-color) !important;
  width: 4px !important;
  min-width: 4px !important;
}

.splitpanes__splitter:hover {
  background-color: var(--resizer-hover, #0078d7) !important;
}

/* Fix container styling */
.workspace {
  height: 100vh;
  overflow: hidden;
}
</style>
