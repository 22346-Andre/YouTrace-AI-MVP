import { defineConfig } from 'vite';
export default defineConfig({server:{proxy:{'/api':'http://localhost:8080'}},preview:{proxy:{'/api':'http://localhost:8080'}}});
