import { QUERY_CLIENT } from '@/lib/queries';
import { ThemeProvider } from '@/lib/theme/theme-provider.tsx';
import { QueryClientProvider } from '@tanstack/react-query';
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';

import App from './App';
import './globals.css';

createRoot(document.getElementById('root') as HTMLElement).render(
  <StrictMode>
    <QueryClientProvider client={QUERY_CLIENT}>
      <ThemeProvider defaultTheme="dark" storageKey="vite-ui-theme">
        <App />
      </ThemeProvider>
    </QueryClientProvider>
  </StrictMode>,
);
