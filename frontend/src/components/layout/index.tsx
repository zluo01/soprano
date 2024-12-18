import AppBar from '@/components/appBar';
import PlaybackQueue from '@/components/queue';
import { Outlet } from 'react-router';
import Preview from 'src/components/preview';

export default function Layout() {
  return (
    <div className="flex size-full flex-col flex-nowrap">
      <AppBar />
      <Outlet />
      <Preview />
      <PlaybackQueue />
    </div>
  );
}
