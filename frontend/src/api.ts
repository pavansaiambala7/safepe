import axios from 'axios';

// Create an Axios instance pointing to our relative /api path.
// The vite.config.ts proxy will magically route this to http://localhost:8080/api
const api = axios.create({
  baseURL: '/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
});

export default api;
