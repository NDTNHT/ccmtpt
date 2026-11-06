import { createApp } from 'vue';
import App from './App.vue';
import './styles/brutalist-tokens.css';

import { initializeTheme } from './composables/useTheme.js';

initializeTheme();

const app = createApp(App);
app.mount('#app');
