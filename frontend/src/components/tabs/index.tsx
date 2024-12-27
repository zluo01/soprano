import { cn } from '@/lib/utils.ts';
import { useEffect } from 'react';
import { useLocation, useNavigate } from 'react-router';

interface ITab {
  name: string;
}

const TABS = new Map<string, ITab>([
  ['/', { name: 'Recently Added' }],
  ['/playlists', { name: 'Playlists' }],
  ['/albums', { name: 'Albums' }],
  ['/albumArtists', { name: 'Album Artists' }],
  ['/artists', { name: 'Artists' }],
  ['/genres', { name: 'Genres' }],
]);

const ALLOW_PATHS = new Set(TABS.keys());

export default function Tabs() {
  const navigate = useNavigate();
  const location = useLocation();

  useEffect(() => {
    const paths = TABS.keys();
    const selected = Array.from(paths).indexOf(location.pathname);
    const anchor = document.getElementById(`tab-${selected}`);
    if (anchor) {
      anchor.scrollIntoView({
        behavior: 'smooth',
        inline: 'center',
        block: 'center',
      });
    }
  }, [location.pathname]);

  return (
    <div
      className={cn(
        'no-scrollbar flex w-full flex-row items-center space-x-2 overflow-x-scroll overflow-y-hidden px-6 py-3 sm:hidden h-full min-h-14 max-h-14',
        !ALLOW_PATHS.has(location.pathname) && 'hidden',
      )}
    >
      {Array.from(TABS.entries()).map(([path, v], index) => (
        <span
          id={`tab-${index}`}
          key={index}
          className={cn(
            'cursor-pointer select-none whitespace-nowrap p-1.5 text font-bold opacity-30',
            location.pathname === path &&
              'scale-105 opacity-100 border-b-4 dark:border-[#F04A4A]',
          )}
          onClick={() => navigate(path)}
        >
          {v.name}
        </span>
      ))}
    </div>
  );
}
