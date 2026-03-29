import { Link, useLocation } from '@tanstack/react-router';
import { useEffect, useRef } from 'react';
import { cn } from '@/lib/utils.ts';

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

const TAB_ENTRIES = Array.from(TABS.entries());
const ALLOW_PATHS = new Set(TABS.keys());

export default function Tabs() {
	const location = useLocation();
	const tabRefs = useRef<Map<string, HTMLSpanElement>>(new Map());

	useEffect(() => {
		const anchor = tabRefs.current.get(location.pathname);
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
			{TAB_ENTRIES.map(([path, v]) => (
				<Link key={path} to={path}>
					<span
						ref={(el) => {
							if (el) tabRefs.current.set(path, el);
						}}
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
