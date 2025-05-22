import { ref, onMounted, onUnmounted } from 'vue'

export function useHostCommunication() {
  const messages = ref([])
  const lastSavedContent = ref('')
  let debounceTimer = null
  let hostInstance = null

  class Host {
    listeners = []

    constructor() {
      console.log('Initializing host communication handler')

      // Check if host is already defined by the script in index.html
      if (window.host) {
        console.log('Using existing window.host instance')
        // Use existing host but add our message processing
        const originalAddMessageListener = window.host.addMessageListener.bind(window.host)
        window.host.addMessageListener = (listener) => {
          this.listeners.push(listener)
          originalAddMessageListener(listener)
        }
        hostInstance = window.host
        return window.host
      }

      window.processMessageFromHost = (message) => {
        try {
          console.log('Raw message from host:', message)
          const msg = typeof message === 'string' ? JSON.parse(message) : message
          console.log('Processed message from host:', msg)
          messages.value.push(msg)
          for (const listener of this.listeners) {
            listener(msg)
          }
        } catch (error) {
          console.error('Error processing message from host:', error, message)
        }
      }

      const queue = []
      if (window.sendMessageToHost) {
        this.sendMessageToHost = window.sendMessageToHost
      } else {
        this.sendMessageToHost = (message) => {
          console.log('Queueing message to host:', message)
          queue.push(message)
        }
        Object.defineProperty(window, "sendMessageToHost", {
          get: () => this.sendMessageToHost,
          set: (value) => {
            console.log('sendMessageToHost is now available')
            this.sendMessageToHost = value
            for (const item of queue) {
              console.log('Sending queued message:', item)
              this.sendMessageToHost(item)
            }
            queue.length = 0
          },
        })
      }
    }

    sendMessage(message) {
      console.log('Sending message to host:', message)
      this.sendMessageToHost(typeof message === "string" ? message : JSON.stringify(message))
    }

    addMessageListener(listener) {
      console.log('Adding message listener')
      this.listeners.push(listener)
    }

    removeMessageListener(listener) {
      const index = this.listeners.indexOf(listener)
      if (index !== -1) {
        this.listeners.splice(index, 1)
      }
    }
  }

  const getHost = () => {
    if (!hostInstance) {
      console.log('Creating new host instance')
      hostInstance = new Host()
    }
    return hostInstance
  }

  const sendToHost = (message) => {
    console.log('sendToHost called with:', message)
    getHost().sendMessage(message)
  }

  // Simple debounced content change notification
  const notifyContentChanged = (content) => {
    // Don't update if content is empty
    if (!content || content.trim() === '') {
      return
    }
    
    // Standard debounce pattern
    clearTimeout(debounceTimer)
    debounceTimer = setTimeout(() => {
      if (content !== lastSavedContent.value) {
        console.log('Content changed, sending update to host')
        lastSavedContent.value = content
        sendToHost({
          event: "contentChanged",
          code: content
        })
      }
    }, 300)
  }

  onMounted(() => {
    // Initialize the host
    const host = getHost()
    
    // Notify the host that the page is initialized
    setTimeout(() => {
      console.log('Page loaded, sending init event')
      sendToHost({
        event: "init"
      })
    }, 100)
  })

  return {
    messages,
    lastSavedContent,
    getHost,
    sendToHost,
    notifyContentChanged
  }
} 