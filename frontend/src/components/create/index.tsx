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
import { CreatePlaylistMutation } from '@/lib/queries';
import { useState } from 'react';

export default function CreatePlaylistModal() {
  const [name, setName] = useState('');

  const { createPlaylistModalState, updateCreatePlaylistModalState } =
    useCreatePlaylistStore();

  const mutation = CreatePlaylistMutation();

  function submit() {
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
            value={name}
            placeholder="Name..."
            onChange={e => setName(e.target.value)}
            type="text"
            required
            autoFocus
          />
        </div>
        <DialogFooter>
          <Button onClick={submit}>Save</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
