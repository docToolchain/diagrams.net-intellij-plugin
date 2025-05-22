<template>
  <div class="preview-container">
    <div class="preview-content">
      <div ref="zenumlRef" class="zenuml-container"></div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, inject } from 'vue'
import { useHostCommunication } from '../composables/useHostCommunication'

// Dynamic import for ZenUML core
let ZenUml = null
const loadZenUml = async () => {
  if (!ZenUml) {
    // Dynamic import
    const module = await import('@zenuml/core')
    ZenUml = module.default
    console.log('ZenUML Core version:', ZenUml.version)
  }
  return ZenUml
}

const props = defineProps({
  content: {
    type: String,
    default: ''
  }
})

const zenumlRef = ref(null)
const zenumlInstance = ref(null)
const initialData = inject('initialData', { theme: 'light' })
const { sendToHost } = useHostCommunication()

// Function to get theme storage key with fallback
const getThemeStorageKey = (id) => {
  if (id === "global") {
    return `${location.hostname || 'local'}-zenuml-conf-theme`
  }
  return id
    ? `${location.hostname || 'local'}-${id}-zenuml-conf-theme`
    : `${location.hostname || 'local'}-preserve-zenuml-conf-theme`
}

// Function to render diagram
const renderDiagram = async () => {
  if (!zenumlRef.value || !props.content) return

  try {
    const ZenUml = await loadZenUml()

    if (!zenumlInstance.value) {
      zenumlInstance.value = new ZenUml(zenumlRef.value)
    }

    // Get theme preferences
    const id = 'local' // Use appropriate ID if available
    const globalTheme = localStorage.getItem(getThemeStorageKey("global"))
    const scopeTheme = id
      ? localStorage.getItem(getThemeStorageKey(id))
      : sessionStorage.getItem(getThemeStorageKey())

    // Render diagram with options
    await zenumlInstance.value.render(props.content, {
      theme: scopeTheme || globalTheme || (initialData.theme === 'dark' ? 'theme-dark' : 'theme-default'),
      enableScopedTheming: Boolean(scopeTheme),
      stickyOffset: 0,
      onContentChange: (newCode) => {
        // Handle content changes if needed
        console.log('Content changed in diagram')
      },
      onThemeChange: ({ theme, scoped }) => {
        if (!scoped) {
          // Global theme change
          localStorage.setItem(getThemeStorageKey("global"), theme)
          localStorage.setItem(getThemeStorageKey(id), "")
          return
        }
        // Scoped theme change
        if (id) {
          localStorage.setItem(getThemeStorageKey(id), theme)
        } else {
          sessionStorage.setItem(getThemeStorageKey(), theme)
        }
      }
    })
  } catch (error) {
    console.error('Error rendering ZenUML diagram:', error)
    // Display error in the preview
    if (zenumlRef.value) {
      zenumlRef.value.innerHTML = `
        <div style="color: red; padding: 10px;">
          <h3>Error rendering diagram</h3>
          <pre>${error.message}</pre>
        </div>
      `
    }
  }
}

// Watch for content changes
watch(() => props.content, () => {
  renderDiagram()
}, { immediate: false })

// Initialize on mount
onMounted(async () => {
  // Apply theme class to body
  document.body.classList.add(initialData.theme === 'dark' ? 'dark-theme' : 'light-theme')

  // Initial render if content available
  if (props.content) {
    await renderDiagram()
  } else {
    // Show placeholder
    zenumlRef.value.innerHTML = '<p>ZenUML diagram will be rendered here</p>'
  }
})
</script>

<style scoped>
.zenuml-container {
  width: 100%;
  height: 100%;
  min-height: 300px;
}
</style>
