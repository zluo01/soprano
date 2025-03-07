import { cn } from '@/lib/utils.ts';
import { useEffect } from 'react';
import { Link, useLocation } from 'react-router';

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
        'no-scrollbar flex h-full max-h-14 min-h-14 w-full flex-row items-center space-x-2 overflow-x-scroll overflow-y-hidden px-6 py-3 sm:hidden',
        !ALLOW_PATHS.has(location.pathname) && 'hidden',
      )}
    >
      {Array.from(TABS.entries()).map(([path, v], index) => (
        <Link key={index} to={path}>
          <span
            id={`tab-${index}`}
            className={cn(
              'text cursor-pointer p-1.5 font-bold whitespace-nowrap opacity-30 select-none',
              location.pathname === path &&
                'scale-105 border-b-4 border-[#5f86c7] opacity-100 dark:border-[#F04A4A]',
            )}
          >
            {v.name}
          </span>
        </Link>
      ))}
    </div>
  );
}
