import { Button } from '@/components/ui/button.tsx';
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Input } from '@/components/ui/input.tsx';
import { useCreatePlaylistStore } from '@/lib/context';
import { useCreatePlaylistMutation, useGetPlaylistsQuery } from '@/lib/queries';
import { cn } from '@/lib/utils.ts';
import { useState } from 'react';

export default function CreatePlaylistModal() {
  const { data } = useGetPlaylistsQuery();

  const playlists = data?.Playlists.map(o => o.name);

  const [name, setName] = useState('');

  const { createPlaylistModalState, updateCreatePlaylistModalState } =
    useCreatePlaylistStore();

  const mutation = useCreatePlaylistMutation();

  function isValid(name: string, playlists: string[] | undefined) {
    if (playlists === undefined) {
      return false;
    }
    return !playlists.includes(name);
  }

  const valid = isValid(name, playlists);

  function submit() {
    if (valid)
      mutation.mutate({
        name,
      });
    updateCreatePlaylistModalState(false);
  }

  return (
    <Dialog
      open={createPlaylistModalState}
      onOpenChange={updateCreatePlaylistModalState}
    >
      <DialogContent className="w-full max-w-[61.8%] rounded-xl border-0 shadow-2xl">
        <DialogHeader>
          <DialogTitle>Create New Playlist</DialogTitle>
        </DialogHeader>
        <div className="grid gap-4 py-4">
          <Input
            className={cn(!valid && 'border border-red-500')}
            value={name}
            placeholder="Name..."
            onChange={e => setName(e.target.value)}
            type="text"
            required
            autoFocus
          />
        </div>
        <DialogFooter>
          <Button onClick={submit} disabled={!data || !valid}>
            Save
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
