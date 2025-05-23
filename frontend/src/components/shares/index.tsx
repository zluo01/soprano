import Cover from '@/components/cover';
import { useSearchStore } from '@/lib/context';
import { cn } from '@/lib/utils.ts';
import { GeneralTag, IAlbum, IGeneralTag } from '@/type';
import { DiscIcon } from '@radix-ui/react-icons';
import { Link, useNavigate } from '@tanstack/react-router';

interface IAlbumGridView {
  albums: IAlbum[];
}

export function AlbumGridView({ albums }: IAlbumGridView) {
  return (
    <div
      className={cn(
        'm-0 border-0 px-6',
        'grid grid-flow-dense auto-rows-fr',
        'grid-cols-2 gap-x-8 gap-y-4',
      )}
    >
      {albums.map(album => (
        <Link
          key={album.id}
          to="/albums/$id"
          params={{ id: album.id.toString() }}
        >
          <div className="relative isolate w-full cursor-pointer">
            <Cover
              albumId={album?.id}
              alt={album.name}
              width={180}
              height={180}
              style={'rounded-full'}
            />
            <p className="cursor-default truncate pt-1 text-center text-sm">
              {album.name}
            </p>
          </div>
        </Link>
      ))}
    </div>
  );
}

interface IGeneralTagItemProps extends IGeneralTag {
  tag: GeneralTag;
}

export function GeneralTagItem({
  tag,
  id,
  name,
  albumCount,
}: IGeneralTagItemProps) {
  const { updateSearchModalState } = useSearchStore();

  const navigate = useNavigate();

  async function route() {
    switch (tag) {
      case GeneralTag.GENRE:
        await navigate({
          to: '/genres/$id/$name',
          params: { id: id.toString(), name },
        });
        break;
      case GeneralTag.ARTIST:
        await navigate({
          to: '/artists/$id/$name',
          params: { id: id.toString(), name },
        });
        break;
      case GeneralTag.ALBUM_ARTIST:
        await navigate({
          to: '/albumArtists/$id/$name',
          params: { id: id.toString(), name },
        });
        break;
    }
    updateSearchModalState(false);
  }

  return (
    <div className="cursor-pointer space-y-1 py-2 select-none" onClick={route}>
      <p className="truncate font-medium">{name}</p>
      <div className="flex flex-row flex-nowrap items-center gap-1.5 text-sm opacity-35">
        <DiscIcon />
        <span>
          {albumCount} album{albumCount > 1 && 's'}
        </span>
      </div>
    </div>
  );
}

interface IGeneralTagItemsPros {
  tag: GeneralTag;
  data?: IGeneralTag[];
}

export function GeneralTagItems({ tag, data }: IGeneralTagItemsPros) {
  return (
    <div className="w-full px-6">
      {data?.map(t => <GeneralTagItem key={t.id} tag={tag} {...t} />)}
    </div>
  );
}
