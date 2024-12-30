import Cover from '@/components/cover';
import { LoadingList } from '@/components/loading';
import { GeneralTagItem } from '@/components/shares';
import { Song } from '@/components/song';
import { SwipeActions } from '@/components/swipe';
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
import { AddSongsToQueue, GetSearchQuery, PlaySong } from '@/lib/queries';
import { GeneralTag, IAlbum } from '@/type';
import { HeartFilledIcon, MagnifyingGlassIcon } from '@radix-ui/react-icons';
import isEmpty from 'lodash/isEmpty';
import { ListPlus } from 'lucide-react';
import { motion, PanInfo } from 'motion/react';
import { useState } from 'react';
import { useNavigate } from 'react-router';

function SearchAlbumItem({ id, name, artist }: IAlbum) {
  const navigate = useNavigate();

  const { updateSearchModalState } = useSearchStore();

  function route() {
    navigate(`/albums/${id}`);
    updateSearchModalState(false);
  }

  return (
    <div
      className="flex w-full cursor-pointer select-none flex-row flex-nowrap items-center space-x-3 py-2"
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
  const { data, isLoading } = GetSearchQuery(searchText);
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
                play={() => PlaySong(o.path)}
                song={o}
                actions={
                  <>
                    <SwipeActions.Action
                      className="bg-add"
                      onClick={() => AddSongsToQueue([o.path])}
                    >
                      <ListPlus className="size-6" />
                    </SwipeActions.Action>
                    <SwipeActions.Action
                      className="bg-favor"
                      onClick={() => openFavorModal(o.path)}
                    >
                      <HeartFilledIcon className="ml-1 size-6" />
                    </SwipeActions.Action>
                  </>
                }
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
        <SheetHeader className="sticky top-0 z-20 border-b-2 bg-primary-foreground px-6 pb-0.5 pt-[calc(env(safe-area-inset-top)+12px)]">
          <div className="relative space-x-1">
            <MagnifyingGlassIcon className="absolute left-2 top-2.5 size-5 text-muted-foreground" />
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
