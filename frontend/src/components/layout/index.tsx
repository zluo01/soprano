import AppBar from '@/components/appBar';
import PlaybackQueue from '@/components/queue';
import Tabs from '@/components/tabs';
import { Outlet } from 'react-router';
import Preview from 'src/components/preview';

export default function Layout() {
  return (
    <>
      <div className="h-screen w-screen select-none scroll-smooth">
        <div className="relative pb-[calc(env(safe-area-inset-bottom)+72px)]">
          <div className="sticky top-0 z-10 flex flex-col bg-background pt-[env(safe-area-inset-top)]">
            <AppBar />
            <Tabs />
          </div>
          <Outlet />
        </div>
      </div>
      <div className="fixed bottom-0 z-10 w-full bg-background pb-[env(safe-area-inset-bottom)]">
        <Preview />
      </div>
      <PlaybackQueue />
    </>
  );
}
