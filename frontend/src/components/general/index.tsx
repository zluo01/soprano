import { LoadingAlbums, LoadingList } from '@/components/loading';
import { AlbumGridView, GeneralTagItems } from '@/components/shares';
import { ScrollArea } from '@/components/ui/scroll-area.tsx';
import { Separator } from '@/components/ui/separator.tsx';
import { GetGeneralTagAlbumsQuery, GetGeneralTagQuery } from '@/lib/queries';
import { GeneralTag } from '@/type';
import { useParams } from 'react-router';

export interface IGeneralTagViewProps {
  tag: GeneralTag;
}

export function GeneralTagView({ tag }: IGeneralTagViewProps) {
  const { data, isLoading } = GetGeneralTagQuery(tag);
  if (isLoading) {
    return <LoadingList />;
  }
  return (
    <ScrollArea className="h-[calc(100%-100px)] w-full pt-2">
      <GeneralTagItems tag={tag} data={data} />
    </ScrollArea>
  );
}

export function GeneralTagAlbumsView({ tag }: IGeneralTagViewProps) {
  const { id } = useParams();
  const { data, isLoading } = GetGeneralTagAlbumsQuery(tag, id);

  return (
    <>
      <div className="px-6 py-3">
        <Separator />
      </div>
      {isLoading ? (
        <LoadingAlbums />
      ) : (
        <ScrollArea className="h-[calc(100%-70px)] w-full pt-2">
          <AlbumGridView albums={data!} />
        </ScrollArea>
      )}
    </>
  );
}
