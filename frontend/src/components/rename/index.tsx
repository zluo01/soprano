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
import { RenamePlaylistMutation } from '@/lib/queries';

export default function RenamePlaylistModal() {
  const { renamePlaylistModalState, closeRenamePlaylistModal, updateName } =
    useRenamePlaylistStore();

  const mutation = RenamePlaylistMutation();

  function submit() {
    mutation.mutate({
      name: renamePlaylistModalState.prevName,
      newName: renamePlaylistModalState.name,
    });
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
            value={renamePlaylistModalState.name}
            onChange={e => updateName(e.target.value)}
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
