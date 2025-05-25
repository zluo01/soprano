import AppBar from '@/components/appBar';
import Favor from '@/components/favor';
import Preview from '@/components/preview';
import PlaybackQueue from '@/components/queue';
import Tabs from '@/components/tabs';
import { Toaster } from '@/components/ui/sonner.tsx';
import type { QueryClient } from '@tanstack/react-query';
import { ReactQueryDevtools } from '@tanstack/react-query-devtools';
import { createRootRouteWithContext, Outlet } from '@tanstack/react-router';
import { TanStackRouterDevtools } from '@tanstack/react-router-devtools';
import { Suspense } from 'react';

export const Route = createRootRouteWithContext<{
  queryClient: QueryClient;
}>()({
  component: () => (
    <>
      <div className="h-full w-screen scroll-smooth pt-[env(safe-area-inset-top)] select-none">
        <div className="h-full pb-[72px]">
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
      </Suspense>
      <Toaster position="top-center" expand={false} />
      <ReactQueryDevtools buttonPosition="top-right" />
      <TanStackRouterDevtools position="bottom-right" />
    </>
  ),
});
