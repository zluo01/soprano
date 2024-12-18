import Albums from '@/components/albums';
import { GeneralTagView } from '@/components/general';
import Playlists from '@/components/playlists';
import RecentlyAdded from '@/components/recent';
import {
  Carousel,
  type CarouselApi,
  CarouselContent,
  CarouselItem,
} from '@/components/ui/carousel';
import { cn } from '@/lib/utils.ts';
import { GeneralTag } from '@/type';
import { useEffect, useState } from 'react';

const TABS = [
  'Recently Added',
  'Playlists',
  'Albums',
  'Album Artists',
  'Artists',
  'Genres',
];

const TAB_INDEX_STORAGE_KEY = 'carousel-tab-index';

export default function MobileMain() {
  const [api, setApi] = useState<CarouselApi>();
  const [current, setCurrent] = useState<number>(
    localStorage.getItem(TAB_INDEX_STORAGE_KEY)
      ? parseInt(localStorage.getItem(TAB_INDEX_STORAGE_KEY) as string)
      : 0,
  );

  useEffect(() => {
    if (!api) {
      return;
    }

    api.on('select', () => {
      const selected = api.selectedScrollSnap();
      // Todo: Change to async to prevent blocking
      localStorage.setItem(TAB_INDEX_STORAGE_KEY, selected.toString());
      setCurrent(selected);
      const anchor = document.getElementById(`tab-${selected}`);
      if (anchor) {
        anchor.scrollIntoView({
          behavior: 'smooth',
          inline: 'center',
          block: 'center',
        });
      }
    });

    const selected = api.selectedScrollSnap();
    if (selected !== current) {
      api.scrollTo(current);
    }
  }, [api]);

  function navigate(index: number) {
    api?.scrollTo(index);
  }

  return (
    <div className="flex size-full flex-col flex-nowrap sm:hidden">
      <div className="no-scrollbar flex w-full flex-row items-center space-x-2 overflow-auto bg-background px-6 pb-6">
        {TABS.map((name, index) => (
          <span
            id={`tab-${index}`}
            key={name}
            className={cn(
              'cursor-pointer select-none whitespace-nowrap p-1.5 text-lg font-bold opacity-30',
              index === current &&
                'scale-105 opacity-100 border-b-4 border-[#F04A4A]',
            )}
            onClick={() => navigate(index)}
          >
            {name}
          </span>
        ))}
      </div>
      <Carousel className="size-full" setApi={setApi}>
        <CarouselContent className="m-0 h-[calc(100vh-240px)] w-full p-0">
          <CarouselItem className="size-full p-0 px-6">
            <RecentlyAdded />
          </CarouselItem>
          <CarouselItem className="size-full p-0 px-6">
            <Playlists />
          </CarouselItem>
          <CarouselItem className="size-full p-0 px-6">
            <Albums />
          </CarouselItem>
          <CarouselItem className="size-full p-0 px-6">
            <GeneralTagView tag={GeneralTag.ALBUM_ARTIST} />
          </CarouselItem>
          <CarouselItem className="size-full p-0 px-6">
            <GeneralTagView tag={GeneralTag.ARTIST} />
          </CarouselItem>
          <CarouselItem className="size-full p-0 px-6">
            <GeneralTagView tag={GeneralTag.GENRE} />
          </CarouselItem>
        </CarouselContent>
      </Carousel>
    </div>
  );
}
