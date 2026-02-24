import React from 'react'
import ReactDOM from 'react-dom/client'
import { Provider } from 'react-redux'
import { BrowserRouter } from 'react-router-dom'
import App from './App'
import { store } from './store'
import './index.css'

/**
 * Application bootstrap.
 *
 * Renders the React tree into the {@code #root} DOM element defined in {@code index.html}.
 * The tree is wrapped with:
 * - {@link React.StrictMode} — activates extra development-mode checks and warnings.
 * - {@link Provider} — makes the Redux {@link store} available to all child components.
 * - {@link BrowserRouter} — enables client-side routing via the HTML5 History API.
 */
ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <Provider store={store}>
      <BrowserRouter>
        <App />
      </BrowserRouter>
    </Provider>
  </React.StrictMode>
)
