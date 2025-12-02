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
import { Sheet, SheetContent, SheetHeader } from '@/components/ui/sheet.tsx';
import { useDebounce } from '@/hooks/useDebounce.ts';
import { useFavorStore, useSearchStore } from '@/lib/context';
import { addSongsToQueue, useGetSearchQuery, playSong } from '@/lib/queries';
import { GeneralTag, IAlbum } from '@/type';
import { HeartFilledIcon, MagnifyingGlassIcon } from '@radix-ui/react-icons';
import { useNavigate } from '@tanstack/react-router';
import isEmpty from 'lodash-es/isEmpty';
import { ListPlus } from 'lucide-react';
import { motion, PanInfo } from 'motion/react';
import { useState } from 'react';

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
            {data?.Search.albums.map(o => (
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
            {data?.Search.artists.map(o => (
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
            {data?.Search.songs.map(o => (
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

export default function Search() {
  const { searchModalState, updateSearchModalState } = useSearchStore();

  const [searchText, setSearchText] = useState('');
  const debounceSearch = useDebounce(searchText);

  function dragEndHandler(dragInfo: PanInfo) {
    const draggedDistance = dragInfo.offset.x;
    const swipeThreshold = 100;
    // swipe to the right
    if (draggedDistance > swipeThreshold) {
      updateSearchModalState(false);
    }
  }

  return (
    <Sheet
      open={searchModalState}
      onOpenChange={open => updateSearchModalState(open)}
    >
      <SheetContent className="max-h-screen w-full overflow-y-scroll px-0 pt-0">
        <SheetHeader className="sticky top-0 z-20 border-b-2 bg-primary-foreground px-6 pt-[calc(env(safe-area-inset-top)+12px)] pb-0.5">
          <div className="relative space-x-1">
            <MagnifyingGlassIcon className="absolute top-2.5 left-2 size-5 text-muted-foreground" />
            <Input
              autoFocus
              placeholder="Search Albums, Artists, Songs..."
              className="border-0 pl-8 placeholder:text-lg placeholder:opacity-35 focus-visible:ring-0"
              value={searchText}
              onChange={e => setSearchText(e.target.value)}
            />
          </div>
        </SheetHeader>
        <motion.div
          className="flex min-h-[calc(100%-120px)] w-full flex-col py-2"
          animate="active"
          exit="exit"
          variants={sliderVariants}
          transition={sliderTransition}
          //Only on x-axis
          drag="x"
          //End of the window either side
          dragConstraints={{ left: 0, right: 0 }}
          // The degree of movement allowed outside constraints.
          dragElastic={0}
          onDragEnd={(_, dragInfo) => dragEndHandler(dragInfo)}
        >
          <SearchContent searchText={debounceSearch} />
        </motion.div>
      </SheetContent>
    </Sheet>
  );
}

const sliderVariants = {
  active: { x: 0, scale: 1, opacity: 1 },
  exit: {
    x: '-100%',
    scale: 1,
    opacity: 0.2,
  },
};

const sliderTransition = {
  duration: 0.5,
  ease: [0.56, 0.03, 0.12, 1.04],
};
