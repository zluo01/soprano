import { LoadingAlbums, LoadingList } from '@/components/loading';
import { AlbumGridView, GeneralTagItems } from '@/components/shares';
import { Separator } from '@/components/ui/separator.tsx';
import { GetGeneralTagAlbumsQuery, GetGeneralTagQuery } from '@/lib/queries';
import { GeneralTag } from '@/type';
import { useParams } from 'react-router-dom';

export interface IGeneralTagViewProps {
  tag: GeneralTag;
}

export function GeneralTagView({ tag }: IGeneralTagViewProps) {
  const { data, isLoading } = GetGeneralTagQuery(tag);
  if (isLoading) {
    return <LoadingList />;
  }
  return <GeneralTagItems tag={tag} data={data} />;
}

export function GeneralTagAlbumsView({ tag }: IGeneralTagViewProps) {
  const { id } = useParams();
  const { data, isLoading } = GetGeneralTagAlbumsQuery(tag, id);

  return (
    <>
      <div className="p-6">
        <Separator />
      </div>
      <div className="size-full px-6">
        {isLoading ? <LoadingAlbums /> : <AlbumGridView albums={data!} />}
      </div>
    </>
  );
}
