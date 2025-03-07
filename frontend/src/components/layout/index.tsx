import AppBar from '@/components/appBar';
import Tabs from '@/components/tabs';
import { lazy, Suspense } from 'react';
import { Outlet } from 'react-router';
import Preview from 'src/components/preview';

const Favor = lazy(() => import('@/components/favor'));
const PlaybackQueue = lazy(() => import('@/components/queue'));

export default function Layout() {
  return (
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
    </>
  );
}
