import AppBar from '@/components/appBar';
import PlaybackQueue from '@/components/queue';
import Tabs from '@/components/tabs';
import { Outlet } from 'react-router';
import Preview from 'src/components/preview';

export default function Layout() {
  return (
    <>
      <div className="flex h-full w-screen flex-col flex-nowrap pb-[env(safe-area-inset-bottom)] pt-[env(safe-area-inset-top)]">
        <AppBar />
        <Tabs />
        <Outlet />
        <Preview />
      </div>
      <PlaybackQueue />
    </>
  );
}
