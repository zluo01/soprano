import { Button } from '@/components/ui/button.tsx';
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Input } from '@/components/ui/input.tsx';
import { useRenamePlaylistStore } from '@/lib/context';
import { GetPlaylistsQuery, RenamePlaylistMutation } from '@/lib/queries';
import { cn } from '@/lib/utils.ts';

export default function RenamePlaylistModal() {
  const { renamePlaylistModalState, closeRenamePlaylistModal, updateName } =
    useRenamePlaylistStore();

  const { data } = GetPlaylistsQuery();

  const playlists = data?.Playlists.map(o => o.name).filter(
    o => o !== renamePlaylistModalState.prevName,
  );

  const mutation = RenamePlaylistMutation();

  function isValid(name: string | undefined, playlists: string[] | undefined) {
    if (playlists === undefined || !name) {
      return false;
    }
    return !playlists.includes(name);
  }

  const valid = isValid(renamePlaylistModalState.name, playlists);

  function submit() {
    if (renamePlaylistModalState.prevName !== renamePlaylistModalState.name) {
      mutation.mutate({
        name: renamePlaylistModalState.prevName,
        newName: renamePlaylistModalState.name,
      });
    }
    closeRenamePlaylistModal();
  }

  return (
    <Dialog
      open={renamePlaylistModalState.open}
      onOpenChange={closeRenamePlaylistModal}
    >
      <DialogContent className="w-full max-w-[61.8%] rounded-xl border-0 shadow-2xl">
        <DialogHeader>
          <DialogTitle>Rename Playlist</DialogTitle>
        </DialogHeader>
        <div className="grid gap-4 py-4">
          <Input
            className={cn(!valid && 'border border-red-500')}
            value={renamePlaylistModalState.name}
            onChange={e => updateName(e.target.value)}
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
