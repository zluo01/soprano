import { LoadingAlbums } from '@/components/loading';
import ScrollContainer from '@/components/scroll';
import { AlbumGridView, GeneralTagItems } from '@/components/shares';
import { Separator } from '@/components/ui/separator.tsx';
import {
  generalTagAlbumsQueryOptions,
  generalTagQueryOptions,
} from '@/lib/queries';
import { GeneralTag } from '@/type';
import { useSuspenseQuery } from '@tanstack/react-query';

interface IGeneralTagViewProps {
  tag: GeneralTag;
}

export function GeneralTagView({ tag }: IGeneralTagViewProps) {
  const { data } = useSuspenseQuery(generalTagQueryOptions(tag));
  return (
    <ScrollContainer className="h-[calc(100%-100px)] w-full pt-2">
      <GeneralTagItems tag={tag} data={data} />
    </ScrollContainer>
  );
}

interface IGeneralTagAlbumViewProps extends IGeneralTagViewProps {
  id: string;
}

export function GeneralTagAlbumsView({ tag, id }: IGeneralTagAlbumViewProps) {
  const { data, isLoading } = useSuspenseQuery(
    generalTagAlbumsQueryOptions(tag, id),
  );

  return (
    <>
      <div className="px-6 py-3">
        <Separator />
      </div>
      {isLoading ? (
        <LoadingAlbums />
      ) : (
        <ScrollContainer className="h-[calc(100%-70px)] w-full pt-2 duration-200 animate-in slide-in-from-right-1/2 sm:animate-none">
          <AlbumGridView albums={data!} />
        </ScrollContainer>
      )}
    </>
  );
}
