import { WebSocketProvider } from '@/lib/context/WebSocketContext.tsx';
import { queryClient } from '@/lib/queries';
import { ThemeProvider } from '@/lib/theme';
import { QueryClientProvider } from '@tanstack/react-query';
import { createRouter, RouterProvider } from '@tanstack/react-router';
import { StrictMode } from 'react';
import ReactDOM from 'react-dom/client';

import './globals.css';
import { routeTree } from './routeTree.gen';

// Set up a Router instance
const router = createRouter({
  routeTree,
  context: {
    queryClient,
  },
  defaultPreload: 'intent',
  // Since we're using React Query, we don't want loader calls to ever be stale
  // This will ensure that the loader is always called when the route is preloaded or visited
  defaultPreloadStaleTime: 0,
  scrollRestoration: true,
});

// Register things for typesafety
declare module '@tanstack/react-router' {
  interface Register {
    router: typeof router;
  }
}

const rootElement = document.getElementById('root')!;

if (!rootElement.innerHTML) {
  const root = ReactDOM.createRoot(rootElement);
  root.render(
    <StrictMode>
      <QueryClientProvider client={queryClient}>
        <ThemeProvider defaultTheme="system">
          <WebSocketProvider>
            <RouterProvider router={router} />
          </WebSocketProvider>
        </ThemeProvider>
      </QueryClientProvider>
    </StrictMode>,
  );
}
