import Cover from '@/components/cover';
import { LoadingList } from '@/components/loading';
import { GeneralTagItem, SongItemWithCover } from '@/components/shares';
import {
  Accordion,
  AccordionContent,
  AccordionItem,
  AccordionTrigger,
} from '@/components/ui/accordion';
import { Drawer, DrawerContent } from '@/components/ui/drawer';
import { Input } from '@/components/ui/input.tsx';
import { useDebounce } from '@/hooks/useDebounce.ts';
import { useSearchStore } from '@/lib/context';
import { GetSearchQuery, PlaySong } from '@/lib/queries';
import { GeneralTag, IAlbum } from '@/type';
import { DialogTitle } from '@radix-ui/react-dialog';
import { MagnifyingGlassIcon } from '@radix-ui/react-icons';
import isEmpty from 'lodash/isEmpty';
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
      className="flex w-full cursor-pointer select-none flex-row flex-nowrap items-center space-x-2 py-2"
      onClick={route}
    >
      <Cover albumId={id} height={48} width={48} alt={name} style={'rounded'} />
      <div className="flex w-fit flex-col justify-center">
        <p className="truncate font-medium">{name}</p>
        <div className="truncate text-sm opacity-35">{artist}</div>
      </div>
    </div>
  );
}

function SearchContent({ searchText }: { searchText: string }) {
  const { data, isLoading } = GetSearchQuery(searchText);

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
    return <span>No Search Result</span>;
  }

  return (
    <Accordion
      type="multiple"
      className="size-full"
      defaultValue={accordianDefaultExpand()}
    >
      {!isEmpty(data?.Search.albums) && (
        <AccordionItem value="albums">
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
        <AccordionItem value="artists">
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
        <AccordionItem value="songs">
          <AccordionTrigger>
            <span className="text-lg font-bold">Songs</span>
          </AccordionTrigger>
          <AccordionContent>
            {data?.Search.songs.map(o => (
              <SongItemWithCover
                key={o.path}
                play={() => PlaySong(o.path)}
                song={o}
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

  return (
    <Drawer
      open={searchModalState}
      onOpenChange={open => updateSearchModalState(open)}
    >
      <DrawerContent className="h-screen rounded-2xl bg-primary-foreground pb-[env(safe-area-inset-bottom)] pt-[env(safe-area-inset-top)]">
        <DialogTitle>
          <div className="border-b-2 px-6 pt-6">
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
          </div>
        </DialogTitle>
        <div className="flex size-full flex-col items-center justify-center overflow-y-scroll bg-background px-6 py-2">
          <SearchContent searchText={debounceSearch} />
        </div>
      </DrawerContent>
    </Drawer>
  );
}
