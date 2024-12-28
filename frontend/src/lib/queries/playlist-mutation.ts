import { request } from './utils.ts';

const CreatePlaylistMutationDocument = /* GraphQL */ `
  mutation CreatePlaylist($name: String!) {
    CreatePlaylist(name: $name)
  }
`;

export async function CreatePlaylist(name?: string) {
  if (!name) {
    return;
  }
  await request(CreatePlaylistMutationDocument, { name });
}

const DeletePlaylistMutationDocument = /* GraphQL */ `
  mutation DeletePlaylist($name: String!) {
    DeletePlaylist(name: $name)
  }
`;

export async function DeletePlaylist(name?: string) {
  if (!name) {
    return;
  }
  await request(DeletePlaylistMutationDocument, { name });
}

const RenamePlaylistMutationDocument = /* GraphQL */ `
  mutation RenamePlaylist($name: String!, $newName: String!) {
    RenamePlaylist(name: $name, newName: $newName)
  }
`;

export async function RenamePlaylist(name?: string, newName?: string) {
  if (!name || !newName) {
    return;
  }
  await request(RenamePlaylistMutationDocument, { name, newName });
}

const AddSongToPlaylistMutationDocument = /* GraphQL */ `
  mutation AddSongToPlaylist($name: String!, $songPath: String!) {
    AddSongToPlaylist(name: $name, songPath: $songPath)
  }
`;

export async function AddSongToPlaylist(name?: string, songPath?: string) {
  if (!name || !songPath) {
    return;
  }
  await request(AddSongToPlaylistMutationDocument, { name, songPath });
}

const DeleteSongFromPlaylistMutationDocument = /* GraphQL */ `
  mutation DeleteSongFromPlaylist($name: String!, $songPath: String!) {
    DeleteSongFromPlaylist(name: $name, songPath: $songPath)
  }
`;

export async function DeleteSongFromPlaylist(name?: string, songPath?: string) {
  if (!name || !songPath) {
    return;
  }
  await request(DeleteSongFromPlaylistMutationDocument, { name, songPath });
}
