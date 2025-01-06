import { Picker, PickerColumn, PickerItem } from '@/components/picker';
import { PickerValue } from '@/components/picker/Picker.tsx';
import { Button } from '@/components/ui/button.tsx';
import { Sheet, SheetContent } from '@/components/ui/sheet.tsx';
import { useFavorStore } from '@/lib/context';
import { AddSongToPlaylistMutation, GetPlaylistsQuery } from '@/lib/queries';
import { cn } from '@/lib/utils.ts';
import { IPlaylist } from '@/type';
import orderBy from 'lodash/orderBy';
import { useState } from 'react';

export default function Favor() {
  const mutation = AddSongToPlaylistMutation();
  const { favorModalState, closeFavorModal } = useFavorStore();

  const { data } = GetPlaylistsQuery();

  const playlists = orderBy(data?.Playlists, ['modifiedTime'], 'desc');

  async function submit(playlistName: string) {
    mutation.mutate({ name: playlistName, songPath: favorModalState.songPath });
    closeFavorModal();
  }

  return (
    <Sheet open={favorModalState.open} onOpenChange={closeFavorModal}>
      <SheetContent className="max-h-[31.8%] rounded-2xl" side="bottom">
        <div className="flex size-full flex-col flex-nowrap">
          <PlaylistPicker playlists={playlists} submit={submit} />
        </div>
      </SheetContent>
    </Sheet>
  );
}

interface IPlaylistPicker {
  playlists: IPlaylist[];
  submit: (playlistName: string) => Promise<void>;
}

function PlaylistPicker({ playlists, submit }: IPlaylistPicker) {
  const [value, setValue] = useState<PickerValue>({
    playlist: playlists[0]?.name || '',
  });

  return (
    <>
      <div className="flex justify-between">
        <p className="font-medium">Choose a playlist</p>
        <Button
          variant="ghost"
          className="text-blue dark:text-char"
          onClick={() => submit(value.playlist as string)}
        >
          Submit
        </Button>
      </div>
      <div className="flex h-full flex-col items-center justify-center">
        <Picker
          className="w-[61.8%] px-4"
          value={value}
          onChange={setValue}
          wheelMode="natural"
        >
          <PickerColumn name="playlist">
            {playlists.map(o => (
              <PickerItem key={o.name} value={o.name}>
                {({ selected }) => (
                  <div
                    className={cn(
                      'opacity-35',
                      selected && 'scale-105 opacity-100',
                    )}
                  >
                    {o.name}
                  </div>
                )}
              </PickerItem>
            ))}
          </PickerColumn>
        </Picker>
      </div>
    </>
  );
}
