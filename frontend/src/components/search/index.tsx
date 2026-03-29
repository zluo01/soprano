import { HeartFilledIcon, MagnifyingGlassIcon } from '@radix-ui/react-icons';
import { useNavigate } from '@tanstack/react-router';
import isEmpty from 'lodash-es/isEmpty';
import { ListPlus } from 'lucide-react';
import { type PointerEvent, useEffect, useRef, useState } from 'react';
import Cover from '@/components/cover';
import { LoadingList } from '@/components/loading';
import { GeneralTagItem } from '@/components/shares';
import { Song } from '@/components/song';
import {
	Accordion,
	AccordionContent,
	AccordionItem,
	AccordionTrigger,
} from '@/components/ui/accordion';
import { Input } from '@/components/ui/input.tsx';
import { Sheet, SheetContent } from '@/components/ui/sheet.tsx';
import { useDebounce } from '@/hooks/useDebounce.ts';
import { useFavorStore, useSearchStore } from '@/lib/context';
import { addSongsToQueue, playSong, useGetSearchQuery } from '@/lib/queries';
import { cn } from '@/lib/utils.ts';
import { GeneralTag, type IAlbum } from '@/type';

function SearchAlbumItem({ id, name, artist }: IAlbum) {
	const navigate = useNavigate();

	const { updateSearchModalState } = useSearchStore();

	async function route() {
		await navigate({ to: '/albums/$id', params: { id: id.toString() } });
		updateSearchModalState(false);
	}

	return (
		<div
			className="flex w-full cursor-pointer flex-row flex-nowrap items-center space-x-3 py-2 select-none"
			onClick={route}
		>
			<Cover albumId={id} height={50} width={50} alt={name} style={'rounded'} />
			<div className="flex w-[calc(100%-50px)] flex-col justify-center">
				<p className="truncate font-medium">{name}</p>
				<div className="truncate text-sm opacity-35">{artist}</div>
			</div>
		</div>
	);
}

function SearchContent({ searchText }: { searchText: string }) {
	const { data, isLoading } = useGetSearchQuery(searchText);
	const { openFavorModal } = useFavorStore();

	function isSearchResultEmpty(): boolean {
		if (!data) {
			return true;
		}
		return (
			isEmpty(data.Search.albums) &&
			isEmpty(data.Search.artists) &&
			isEmpty(data.Search.songs)
		);
	}

	function accordianDefaultExpand() {
		if (!data) {
			return [];
		}
		if (data.Search.albums) {
			return ['albums'];
		}
		if (data.Search.artists) {
			return ['artists'];
		}

		return ['songs'];
	}

	if (isLoading) {
		return <LoadingList />;
	}

	if (isSearchResultEmpty()) {
		return <p className="m-auto">No Search Result</p>;
	}

	return (
		<Accordion
			type="multiple"
			className="size-full"
			defaultValue={accordianDefaultExpand()}
		>
			{!isEmpty(data?.Search.albums) && (
				<AccordionItem value="albums" className="px-6">
					<AccordionTrigger>
						<span className="text-lg font-bold">Albums</span>
					</AccordionTrigger>
					<AccordionContent>
						{data?.Search.albums.map((o) => (
							<SearchAlbumItem key={o.id} {...o} />
						))}
					</AccordionContent>
				</AccordionItem>
			)}

			{!isEmpty(data?.Search.artists) && (
				<AccordionItem value="artists" className="px-6">
					<AccordionTrigger>
						<span className="text-lg font-bold">Artists</span>
					</AccordionTrigger>
					<AccordionContent>
						{data?.Search.artists.map((o) => (
							<GeneralTagItem key={o.id} tag={GeneralTag.ALBUM_ARTIST} {...o} />
						))}
					</AccordionContent>
				</AccordionItem>
			)}

			{!isEmpty(data?.Search.songs) && (
				<AccordionItem value="songs" className="px-6">
					<AccordionTrigger>
						<span className="text-lg font-bold">Songs</span>
					</AccordionTrigger>
					<AccordionContent>
						{data?.Search.songs.map((o) => (
							<Song
								key={o.path}
								play={() => playSong(o.path)}
								song={o}
								actions={[
									{
										className: 'bg-add',
										action: () => addSongsToQueue([o.path]),
										children: <ListPlus className="size-6" />,
									},
									{
										className: 'bg-favor',
										action: () => openFavorModal(o.path),
										children: <HeartFilledIcon className="ml-1 size-6" />,
									},
								]}
							/>
						))}
					</AccordionContent>
				</AccordionItem>
			)}
		</Accordion>
	);
}

const DISMISS_RATIO = 0.61;

export default function Search() {
	const { searchModalState, updateSearchModalState } = useSearchStore();

	const [searchText, setSearchText] = useState('');
	const debounceSearch = useDebounce(searchText);

	const contentRef = useRef<HTMLDivElement>(null);
	const scrollRef = useRef<HTMLDivElement>(null);
	const dragStartY = useRef(0);
	const isDragging = useRef(false);

	useEffect(() => {
		const vv = window.visualViewport;
		if (!vv || !searchModalState) return;

		function update() {
			if (!contentRef.current || !vv) return;
			contentRef.current.style.height = `${vv.height}px`;
			contentRef.current.style.top = `${vv.offsetTop}px`;
		}

		update();
		vv.addEventListener('resize', update);
		vv.addEventListener('scroll', update);
		return () => {
			vv.removeEventListener('resize', update);
			vv.removeEventListener('scroll', update);
		};
	}, [searchModalState]);

	function canDismiss() {
		if (!scrollRef.current) return true;
		return scrollRef.current.scrollTop <= 0;
	}

	function onPointerDown(e: PointerEvent) {
		dragStartY.current = e.clientY;
	}

	function onPointerMove(e: PointerEvent) {
		if (!contentRef.current) return;
		const delta = e.clientY - dragStartY.current;

		if (!isDragging.current) {
			if (delta > 0 && canDismiss()) {
				isDragging.current = true;
				dragStartY.current = e.clientY;
			}
			return;
		}

		const dragDelta = e.clientY - dragStartY.current;
		if (dragDelta > 0) {
			contentRef.current.style.transform = `translateY(${dragDelta}px)`;
			contentRef.current.style.transition = 'none';
		}
	}

	function onPointerUp() {
		if (!isDragging.current || !contentRef.current) {
			isDragging.current = false;
			return;
		}
		isDragging.current = false;
		const sheetHeight = contentRef.current.getBoundingClientRect().height;
		const threshold = sheetHeight * DISMISS_RATIO;
		const currentY = new DOMMatrixReadOnly(
			getComputedStyle(contentRef.current).transform,
		).m42;
		contentRef.current.style.transition = 'transform 0.3s ease-out';
		if (currentY > threshold) {
			contentRef.current.style.transform = 'translateY(100%)';
			setTimeout(() => updateSearchModalState(false), 300);
		} else {
			contentRef.current.style.transform = '';
		}
	}

	return (
		<Sheet
			open={searchModalState}
			onOpenChange={(open) => updateSearchModalState(open)}
		>
			<SheetContent
				ref={contentRef}
				side="bottom"
				className={cn(
					'flex h-full flex-col rounded-t-3xl gap-0 px-0 pt-0 top-0 bottom-auto',
					'pt-[env(safe-area-inset-top)]',
				)}
				onPointerDown={onPointerDown}
				onPointerMove={onPointerMove}
				onPointerUp={onPointerUp}
			>
				<div className="flex shrink-0 justify-center py-4">
					<div className="h-1.5 w-12 rounded-full bg-muted" />
				</div>
				<div className="shrink-0 border-b-2 bg-primary-foreground px-6 py-1.5">
					<div className="relative space-x-1">
						<MagnifyingGlassIcon className="absolute top-2.5 left-2 size-5 text-muted-foreground" />
						<Input
							autoFocus
							placeholder="Search Albums, Artists, Songs..."
							className="border-0 pl-8 placeholder:text-lg placeholder:opacity-35 focus-visible:ring-0"
							value={searchText}
							onChange={(e) => setSearchText(e.target.value)}
						/>
					</div>
				</div>
				<div
					ref={scrollRef}
					className="flex flex-1 flex-col overflow-y-auto py-2"
				>
					<SearchContent searchText={debounceSearch} />
				</div>
			</SheetContent>
		</Sheet>
	);
}
