import React from 'react';
import {createRoot} from 'react-dom/client';
import './style.css';
import './app.js';
createRoot(document.getElementById('root')).render(React.createElement(globalThis.createYouTraceApp(React)));
