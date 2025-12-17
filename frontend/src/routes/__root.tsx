import AppBar from '@/components/appBar';
import Preview from '@/components/preview';
import Tabs from '@/components/tabs';
import { Toaster } from '@/components/ui/sonner.tsx';
import type { QueryClient } from '@tanstack/react-query';
import { ReactQueryDevtools } from '@tanstack/react-query-devtools';
import { createRootRouteWithContext, Outlet } from '@tanstack/react-router';
import { TanStackRouterDevtools } from '@tanstack/react-router-devtools';
import { lazy, Suspense } from 'react';

const Favor = lazy(() => import('src/components/favor'));
const PlaybackQueue = lazy(() => import('src/components/queue'));
const PlaybackDrawer = lazy(() => import('src/components/playback'));

export const Route = createRootRouteWithContext<{
  queryClient: QueryClient;
}>()({
  component: () => (
    <>
      <div className="h-full w-screen scroll-smooth select-none">
        <div className="h-full pb-18">
          <AppBar />
          <Tabs />
          <Outlet />
        </div>
      </div>
      <div className="fixed bottom-0 z-20 w-full">
        <Preview />
      </div>
      <Suspense>
        <Favor />
        <PlaybackQueue />
        <PlaybackDrawer />
      </Suspense>
      <Toaster position="top-center" expand={false} />
      <ReactQueryDevtools buttonPosition="top-right" />
      <TanStackRouterDevtools position="bottom-right" />
    </>
  ),
});
