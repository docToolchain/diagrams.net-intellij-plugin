import { createApp } from 'vue'
import App from './App.vue'
import './styles/main.css'

// Function to wait until window.host is available
function waitForHost() {
  return new Promise((resolve) => {
    if (window.host) {
      resolve(window.host)
    } else {
      const checkInterval = setInterval(() => {
        if (window.host) {
          clearInterval(checkInterval)
          resolve(window.host)
        }
      }, 50)
    }
  })
}

// Parse initial data or provide defaults
let initialData = { theme: 'light' }
try {
  // In development, initialData will contain $$initialData$$
  // In production, it will be replaced with the actual data
  if (window.initialData) {
    initialData = window.initialData
  } else if (typeof initialData === 'string' && !initialData.includes('$$')) {
    initialData = JSON.parse(initialData)
  }
} catch (error) {
  console.error('Error parsing initialData:', error)
}

// Wait for host to be available before mounting the app
waitForHost().then(() => {
  console.log('Host is available, mounting app')
  
  // Create Vue app
  const app = createApp(App)
  
  // Provide global initialData
  app.provide('initialData', initialData)
  
  // Mount the app
  app.mount('#app')
}) 