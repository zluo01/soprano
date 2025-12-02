import Picker from '@/components/picker';
import { Button } from '@/components/ui/button.tsx';
import { Sheet, SheetContent } from '@/components/ui/sheet.tsx';
import { useFavorStore } from '@/lib/context';
import { AddSongToPlaylistMutation, GetPlaylistsQuery } from '@/lib/queries';
import { IPlaylist } from '@/type';
import orderBy from 'lodash-es/orderBy';
import { useState } from 'react';
import { toast } from 'sonner';

export default function Favor() {
  const mutation = AddSongToPlaylistMutation();
  const { favorModalState, closeFavorModal } = useFavorStore();

  const { data } = GetPlaylistsQuery();

  const playlists = orderBy(data?.Playlists, ['modifiedTime'], 'desc');

  async function submit(playlistName: string) {
    mutation.mutate({ name: playlistName, songPath: favorModalState.songPath });
    toast(`Successfully add song to ${playlistName}.`);
    closeFavorModal();
  }

  return (
    <Sheet open={favorModalState.open} onOpenChange={closeFavorModal}>
      <SheetContent className="h-[31.8%] rounded-2xl p-6" side="bottom">
        <PlaylistPicker playlists={playlists} submit={submit} />
      </SheetContent>
    </Sheet>
  );
}

interface IPlaylistPicker {
  playlists: IPlaylist[];
  submit: (playlistName: string) => Promise<void>;
}

function PlaylistPicker({ playlists, submit }: IPlaylistPicker) {
  const [index, setIndex] = useState(0);

  return (
    <>
      <div className="flex justify-between">
        <p className="font-medium">Choose a playlist</p>
        <Button
          variant="ghost"
          className="text-blue dark:text-char"
          onClick={() => submit(playlists[index].name)}
        >
          Submit
        </Button>
      </div>
      <Picker slides={playlists.map(o => o.name)} select={setIndex} />
    </>
  );
}
