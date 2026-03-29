import type { QueryClient } from '@tanstack/react-query';
import { ReactQueryDevtools } from '@tanstack/react-query-devtools';
import { createRootRouteWithContext, Outlet } from '@tanstack/react-router';
import { TanStackRouterDevtools } from '@tanstack/react-router-devtools';
import { lazy, Suspense } from 'react';
import AppBar from '@/components/appBar';
import Preview from '@/components/preview';
import Tabs from '@/components/tabs';
import { Toaster } from '@/components/ui/sonner.tsx';

const Favor = lazy(() => import('@/components/favor'));
const PlaybackQueue = lazy(() => import('@/components/queue'));
const PlaybackDrawer = lazy(() => import('@/components/playback'));

export const Route = createRootRouteWithContext<{
	queryClient: QueryClient;
}>()({
	component: RootComponent,
});

function RootComponent() {
	return (
		<>
			<div className="flex min-h-dvh flex-col select-none">
				<div className="sticky top-0 z-30 bg-background pt-[env(safe-area-inset-top)]">
					<AppBar />
					<Tabs />
				</div>
				<div className="flex flex-1 flex-col pb-preview">
					<Outlet />
				</div>
			</div>
			<div className="fixed bottom-0 z-20 w-full bg-background">
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
	);
}
