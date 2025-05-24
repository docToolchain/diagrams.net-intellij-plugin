<template>
  <div class="editor-container">
    <div ref="editorElement" class="editor-content">
      <BuildTimestamp class="editor-timestamp" />
    </div>
  </div>
</template>

<script setup>
import {ref, watch, defineProps, defineEmits, onMounted, onBeforeUnmount, computed} from 'vue'
import {baseExtensionsFactory, zenumlExtensions} from "./extensions";
import {EditorView} from '@codemirror/view'
import {Compartment, EditorState} from '@codemirror/state'
import {useHostCommunication} from '../composables/useHostCommunication'
import BuildTimestamp from './BuildTimestamp.vue'

const props = defineProps({
  initialContent: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['content-change'])
const editorContent = ref(props.initialContent)
const editorElement = ref()
const cmView = ref(null)
const { notifyContentChanged, messages, getHost } = useHostCommunication()

// Create a compartment for diagram-specific extensions
const diagramCompartment = new Compartment()

const updateContent = (newContent) => {
  if (newContent !== editorContent.value) {
    editorContent.value = newContent
    emit('content-change', newContent)
    notifyContentChanged(newContent)
  }
}

const onEditorCodeChange = (newCode) => {
  updateContent(newCode)
}

const baseExtensions = computed(() => baseExtensionsFactory(onEditorCodeChange))

onMounted(() => {
  cmView.value = new EditorView({
    state: EditorState.create({
      doc: editorContent.value,
      extensions: [
        ...baseExtensions.value,
        diagramCompartment.of(zenumlExtensions)
      ]
    }),
    parent: editorElement.value,
  })
})

onBeforeUnmount(() => {
  if (cmView.value) cmView.value.destroy()
})

// Watch for messages from the host
watch(messages, (newMessages) => {
  const latestMessage = newMessages[newMessages.length - 1]
  if (latestMessage) {
    if (latestMessage.action === 'load' || latestMessage.action === 'update') {
      if (latestMessage.code !== editorContent.value) {
        editorContent.value = latestMessage.code || ''
        if (cmView.value) {
          cmView.value.dispatch({
            changes: { from: 0, to: cmView.value.state.doc.length, insert: editorContent.value }
          })
        }
        emit('content-change', editorContent.value)
      }
    }
  }
}, { deep: true })

// Listen for host messages directly
onMounted(() => {
  const host = getHost()
  const messageListener = (message) => {
    if (message.action === 'load' || message.action === 'update') {
      if (message.code !== editorContent.value) {
        editorContent.value = message.code || ''
        if (cmView.value) {
          cmView.value.dispatch({
            changes: { from: 0, to: cmView.value.state.doc.length, insert: editorContent.value }
          })
        }
        emit('content-change', editorContent.value)
      }
    }
  }
  host.addMessageListener(messageListener)
})
</script>

<style scoped>
.editor-container {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.editor-content {
  flex: 1;
  min-height: 0;
  position: relative;
}

.editor-timestamp {
  position: absolute;
  bottom: 8px;
  right: 8px;
  z-index: 10;
}

/* Styles are in main.css */
:deep(.cm-editor) {
  min-height: 200px;
  font-family: Menlo, 'Fira Code', Monaco, source-code-pro, "Ubuntu Mono", "DejaVu sans mono", Consolas, monospace;
  font-size: 15px;
  height: 100% !important;
  width: 100%;
}
</style>
